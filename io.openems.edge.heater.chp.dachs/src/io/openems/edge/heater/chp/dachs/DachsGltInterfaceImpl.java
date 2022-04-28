package io.openems.edge.heater.chp.dachs;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.Chp;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.api.StartupCheckHandler;
import io.openems.edge.heater.chp.dachs.api.DachsGltInterface;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Chp Dachs GLT interface.
 * This controller communicates with a Senertec Dachs Chp via the GLT web interface and maps the return message to 
 * OpenEMS channels. Read and write is supported.
 * Not all GLT commands have been coded in yet, only those for basic CHP operation.
 *
 * <p>The module is written to be used with the Heater interface methods. However, this chp does not have variable power
 * control. So the methods setHeatingPowerSetpoint(), setElectricPowerSetpoint() and setTemperatureSetpoint() are not
 * available. This also means power control with ExceptionalState value is not possible.
 * Some sort of power control is possible if it is a bigger chp containing several units. It is possible to set
 * the number of modules that should turn on. This is not coded in yet.</p>
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.SenertecDachs",
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		immediate = true)
// This module uses Controller instead of EventHandler because "HttpURLConnection" does not work in "handleEvent()".
public class DachsGltInterfaceImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller,
		ExceptionalState, DachsGltInterface {

	@Reference
	protected ComponentManager cpm;

	private static final int NO_DATA = -1;	// Value to put in a channel if no data is received.
	private static final int NORMAL_OPERATION_RPM_THRESHOLD = 2000;	// If engine RPM is above this threshold, the chp is considered to be running normally. Typical RPM is ~2400.

	private final Logger log = LoggerFactory.getLogger(DachsGltInterfaceImpl.class);
	private InputStream is = null;
	private String urlBuilderIP;
	private String basicAuth;
	private int interval;
	private static final int MAX_INTERVAL = 540;		// unit is seconds.
	private LocalDateTime timestamp;
	private boolean debug;
	private boolean basicInfo;
	
	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private boolean readyForCommands;

	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "DACHS_CHP_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "DACHS_CHP_EXCEPTIONAL_STATE_IDENTIFIER";


	public DachsGltInterfaceImpl() {
		super(OpenemsComponent.ChannelId.values(),
				DachsGltInterface.ChannelId.values(),
				Chp.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values(),
				Controller.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// Limit interval to 9 minutes max. Because on/off command needs to be sent to Dachs at least once every 10 minutes.
		this.interval = Math.min(config.interval(), MAX_INTERVAL);

		this.timestamp = LocalDateTime.now().minusSeconds(this.interval);		// Subtract interval, so polling starts immediately.
		this.urlBuilderIP = config.address();
		String gltpass = config.username() + ":" + config.password();
		this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(gltpass.getBytes()));
		this.getSerialAndPartsNumber();
		this.debug = config.debug();
		this.basicInfo = config.basicInfo();
		this.readOnly = config.readOnly();
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}
		
		if (this.readOnly == false) {
			this.startupStateChecked = false;
			this.initializeTimers(config);
		}
	}

	private void initializeTimers(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
		timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.enableSignalTimerId(), config.waitTimeEnableSignal());
		this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
		this.useExceptionalState = config.useExceptionalState();
		if (this.useExceptionalState) {
			timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.exceptionalStateTimerId(), config.waitTimeExceptionalState());
			this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
		}
	}
	
	@Deactivate
	protected void deactivate() {
		super.deactivate(); 
	}

	@Override
	public void run() {
		
		// How often the Dachs is polled is determined by ’interval’.
		if (this.isEnabled() && ChronoUnit.SECONDS.between(this.timestamp, LocalDateTime.now()) >= this.interval) {
			this.updateChannels();
			this.timestamp = LocalDateTime.now();

			// Output to log depending on config settings.
			this.printDataToLog();

			if (this.readOnly == false && this.readyForCommands) {

				// Handle EnableSignal.
				boolean turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

				// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
				if (this.useExceptionalState) {
					boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
					if (exceptionalStateActive) {
						int exceptionalStateValue = this.getExceptionalStateValue();
						if (exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
							turnOnChp = false;
						} else {
							turnOnChp = true;
						}
					}
				}

				// Check heater state at startup. Avoid turning off heater just because EnableSignal initial value is ’false’.
				if (this.startupStateChecked == false) {
					this.startupStateChecked = true;
					turnOnChp = StartupCheckHandler.deviceAlreadyHeating(this, this.log);
				}

				/* This is the on-off switch. There are some things to watch out for:
				 - This is not a hard command, especially the ’off’ command. The Dachs has a list of reasons to be 
				   running (see ’Dachs-Lauf-Anforderungen’), the ’external requirement’ (this on/off switch) being 
				   one of many. If any one of those reasons is true, it is running. Only if all of them are false,
				   it will shut down.
				   Bottom line, deactivateDachs() only does something if nothing else tells the Dachs to run.
				   And activateDachs() might be ignored because of a limitation.
				 - Timing: need to send ’on’ command at least every 10 minutes for the Dachs to keep running.
				   ’interval’ is capped at 9 minutes, so this should be taken care of.
				 - Also: You cannot switch a CHP on/off as you want. Number of starts should be minimized, and because
				   of that there is a limit/hour on how often you can start. If the limit is reached, the chp won't start.
				   Currently the code does not enforce any restrictions to not hit that limit! */
				if (turnOnChp) {
					this.activateDachs();
				} else {
					this.deactivateDachs();
				}
			}
		}
	}
	
	/**
	 * This method communicates with the chp and processes the response.
	 * ’getKeyDachs(...)’ is the method to request data from the chp. The answer is saved to ’serverMessage’.
	 * The ’serverMessage’ is a string structured as ’key=value’, with a ’/n’ separating each key-value pair.
	 */
	protected void updateChannels() {
		String serverMessage = this.getKeyDachs("k=Wartung_Cache.fStehtAn&" 
				+ "k=Hka_Bd.ulAnzahlStarts&" 
				+ "k=Hka_Mw1.usDrehzahl&" 
				+ "k=Hka_Bd.ulBetriebssekunden&" 
				+ "k=Hka_Bd.ulArbeitThermKon&" 
				+ "k=Hka_Bd.ulArbeitThermHka&" 
				+ "k=Hka_Bd.ulArbeitElektr&" 
				+ "k=Hka_Bd.Anforderung.UStromF_Anf.bFlagSF&" 
				+ "k=Hka_Bd.UHka_Anf.Anforderung.fStrom&" 
				+ "k=Hka_Bd.UStromF_Frei.bFreigabe&" 
				+ "k=Hka_Bd.Anforderung.ModulAnzahl&" 
				+ "k=Hka_Bd.UHka_Anf.usAnforderung&" 
				+ "k=Hka_Bd.UHka_Frei.usFreigabe&" 
				+ "k=Hka_Mw1.Temp.sbRuecklauf&" 
				+ "k=Hka_Mw1.Temp.sbVorlauf&" 
				+ "k=Hka_Mw1.sWirkleistung&" 
				+ "k=Hka_Bd.bWarnung&" 
				+ "k=Hka_Bd.bStoerung"); // A description of these keys is found in DachsGltInterfaceChannel

		/* Test if a specific string can be found in the message to see if the server message is ok. If this string can 
		   not be found, the server message is most likely garbage. -> Abort */
		if (serverMessage.contains("Hka_Bd.bStoerung=")) {
			this.readyForCommands = true;

			String errorMessage = this.parseErrorCode(serverMessage);

			String warningMessage = this.parseWarningCode(serverMessage);

			errorMessage = errorMessage + this.parseAndSetEffectiveElectricPower(serverMessage);

			errorMessage = errorMessage + this.parseAndSetFlowTemperature(serverMessage);

			errorMessage = errorMessage + this.parseAndSetReturnTemperature(serverMessage);

			boolean runClearance = false;
			boolean stateUndefined = false;
			String runClearanceBitsAsIntString = this.readEntryAfterString(serverMessage, "Hka_Bd.UHka_Frei.usFreigabe=");
			if (runClearanceBitsAsIntString.length() > 0) {
				if (runClearanceBitsAsIntString.equals("65535")) {	// This is the int equivalent of hex FFFF. Manual discusses ’Freigabe’ code in hex.
					runClearance = true;
					this._setRunEnableBits("Code FFFF: Dachs is ready to run.");
				} else {
					errorMessage = errorMessage + this.parseAndSetDachsRunEnable(runClearanceBitsAsIntString);
				}
			} else {
				errorMessage = errorMessage + "Failed to transmit Chp ready indicator (Freigabe), ";
				this._setRunEnableBits("Failed to transmit Chp ready indicator (Freigabe).");
				stateUndefined = true;
			}

			boolean stateStartingUp = false;
			String runRequestBitsAsIntString = this.readEntryAfterString(serverMessage, "Hka_Bd.UHka_Anf.usAnforderung=");
			if (runRequestBitsAsIntString.length() > 0) {
				if (runRequestBitsAsIntString.equals("0")) {
					this._setRequestOfRunBits("Code 0: Nothing is requesting the Dachs to run right now.");
				} else {
					try {
						int runRequestBits = Integer.parseInt(runRequestBitsAsIntString.trim());
						String returnMessage = this.parseRunRequestBits(runRequestBits);
						this._setRequestOfRunBits(returnMessage);
						if (runRequestBits > 0) {
							// This is probably not necessary, but just in case.
							stateStartingUp = true;
						}
					} catch (NumberFormatException e) {
						// This is not really needed for chp operation, so it is a warning and not an error.
						warningMessage = warningMessage + "Can't parse run request code (Lauf Anforderung): " + e.getMessage() + ", ";
						this._setRequestOfRunBits("Code " + runRequestBitsAsIntString + ": Error parsing code.");
					}
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit run request code (Lauf Anforderung), ";
				this._setRequestOfRunBits("Failed to transmit run request code (Lauf Anforderung).");
			}

			warningMessage = warningMessage + this.parseAndSetNumberOfModules(serverMessage);

			warningMessage = warningMessage + this.parseAndSetEnableElectricity(serverMessage);

			warningMessage = warningMessage + this.parseAndSetElectricityDemandFlag(serverMessage);

			warningMessage = warningMessage + this.parseAndSetElectricityRequestBits(serverMessage);

			warningMessage = warningMessage + this.parseAndSetGeneratedElectricalWork(serverMessage);

			warningMessage = warningMessage + this.parseAndSetGeneratedThermalWork(serverMessage);

			warningMessage = warningMessage + this.parseAndSetGeneratedThermalWorkCond(serverMessage);

			warningMessage = warningMessage + this.parseAndSetRuntimeSinceRestart(serverMessage);

			int rpmReadout = 0;
			String engineSpeedString = this.readEntryAfterString(serverMessage, "Hka_Mw1.usDrehzahl=");
			if (engineSpeedString.length() > 0) {
	            try {
					rpmReadout = Integer.parseInt(engineSpeedString.trim());
	                this._setRpm(rpmReadout);
	            } catch (NumberFormatException e) {
	            	errorMessage = errorMessage + "Can't parse engine rpm (Motordrehzahl): " + e.getMessage() + ", ";
					this._setRpm(NO_DATA);
	            }
			} else {
				errorMessage = errorMessage + "Failed to transmit engine rpm (Motordrehzahl), ";
				this._setRpm(NO_DATA);
			}

			warningMessage = warningMessage + this.parseAndSetEngineStarts(serverMessage);

			warningMessage = warningMessage + this.parseAndSetMaintenanceFlag(serverMessage);
			
			if (errorMessage.length() > 0) {
				errorMessage = errorMessage.substring(0, errorMessage.length() - 2) + ".";
			} else {
				errorMessage = "No error";
			}
			this._setErrorMessage(errorMessage);
			
			if (warningMessage.length() > 0) {
				warningMessage = warningMessage.substring(0, warningMessage.length() - 2) + ".";
			} else {
				warningMessage = "No warning";
			}
			this._setWarningMessage(warningMessage);

			boolean chpEngineRunning;
			if (stateUndefined) {
				chpEngineRunning = false;
				this._setHeaterState(HeaterState.UNDEFINED.getValue());
			} else if (rpmReadout > NORMAL_OPERATION_RPM_THRESHOLD) {
				chpEngineRunning = true;
				this._setHeaterState(HeaterState.RUNNING.getValue());
			} else if (stateStartingUp) {
				chpEngineRunning = true;
				this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
			} else if (runClearance) {
				chpEngineRunning = false;
				this._setHeaterState(HeaterState.STANDBY.getValue());
			} else {
				chpEngineRunning = false;
				this._setHeaterState(HeaterState.BLOCKED_OR_ERROR.getValue());
			}
			// Switch process image because result is needed immediately for startupCheck.
			this.getHeaterStateChannel().nextProcessImage();

		} else {
			this.readyForCommands = false;
			this._setErrorMessage("Couldn't read data from GLT interface.");
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
	}

	/**
	 * Extract the error code from the server message and return the description of the error code(s) as a string.
	 *
	 * @param serverMessage the server message
	 * @return the error message
	 */
	protected String parseErrorCode(String serverMessage) {
		String returnMessage = "";
		String value = this.readEntryAfterString(serverMessage, "Hka_Bd.bStoerung=");
		if (value.length() == 0) {
			// ’value’ should contain "0" for no error, or the number of the error code(s). If ’value’ contains 
			// nothing, something went wrong.
			returnMessage = "Failed to transmit error code, ";
		} else {
			if (value.equals("0") == false) {
				returnMessage = "Code " + value + ": ";
				if (value.contains("101")) {
					returnMessage = returnMessage + "Dachs outlet exhaust sensor - interruption/short-circuit (Abgasfuehler "
							+ "HKA-Austritt - Unterbrechung/Kurzschluss), "; // HKA = Dachs
				}
				if (value.contains("102")) {
					returnMessage = returnMessage + "Engine water temperature sensor - interruption/short-circuit "
							+ "(Kuehlwasserfuehler Motor - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("103")) {
					returnMessage = returnMessage + "Generator water temperature sensor - interruption/short-circuit "
							+ "(Kuehlwasserfuehler Generator - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("104")) {
					returnMessage = returnMessage + "Engine outlet exhaust sensor - interruption/short-circuit (Abgasfuehler "
							+ "Motor-Austritt - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("105")) {
					returnMessage = returnMessage + "Flow temperature sensor - interruption/short-circuit (Vorlauftemperatur "
							+ "- Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("106")) {
					returnMessage = returnMessage + "Return temperature sensor - interruption/short-circuit (Ruecklauftemperatur "
							+ "- Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("107")) {
					returnMessage = returnMessage + "Sensor 1 - interruption/short-circuit (Fuehler 1 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("108")) {
					returnMessage = returnMessage + "Sensor 2 - interruption/short-circuit (Fuehler 2 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("109")) {
					returnMessage = returnMessage + "Outside sensor - interruption/short-circuit (Außenfuehler - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("110")) {
					returnMessage = returnMessage + "Enclosure sensor - interruption/short-circuit (Kapselfuehler - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("111")) {
					returnMessage = returnMessage + "Controller internal sensor - interruption/short-circuit (Fuehler Regler "
							+ "intern - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("120")) {
					returnMessage = returnMessage + "Engine outlet exhaust temperature too high (Abgastemperatur Motor-Austritt "
							+ "- zu hoch;G:>620°C,HR:>520°C), ";
				}
				if (value.contains("121")) {
					returnMessage = returnMessage + "Enclosure temperature too high (Kapseltemperatur - zu hoch; > 120°C), ";
				}
				if (value.contains("122")) {
					returnMessage = returnMessage + "Engine water temperature too high (Kuehlwassertemperatur Motor (Austritt) - zu hoch; > 95°C), ";
				}
				if (value.contains("123")) {
					returnMessage = returnMessage + "Dachs outlet exhaust temperature too high (Abgastemperatur HKA-Austritt - zu hoch; > 210°C), ";
				}
				if (value.contains("124")) {
					returnMessage = returnMessage + "Generator water temperature (inlet) too high (Kuehlwassertemperatur "
							+ "Generator (Eintritt) - zu hoch; > 77°C), ";
				}
				if (value.contains("129")) {
					returnMessage = returnMessage + "Reverse power - fuel supply or ignition faulty (Rueckleistung - "
							+ "Brennstoffversorgung oder Zuendung fehlerhaft), ";
				}
				if (value.contains("130")) {
					returnMessage = returnMessage + "Rotation speed in spite of switched-off starter after false start "
							+ "(Drehzahl nach Anlasser AUS - Drehzahl trotz ausgeschaltetem Anlasser bei Fehlstart), ";
				}
				if (value.contains("131")) {
					returnMessage = returnMessage + "Engine RPM too low (HKA-Anlauf < 100 U/min - 1 sek nach Anlasser ein: n < 100 U/min), ";
				}
				if (value.contains("133")) {
					returnMessage = returnMessage + "Engine RPM too low (HKA-Lauf < 2300 U/min - n<2300 U/min fuer 30 sek "
							+ "nach Erreichen 800 U/min), ";
				}
				if (value.contains("139")) {
					returnMessage = returnMessage + "Generator not connected because engine RPM too high (Generatorzuschaltung - "
							+ "keine Zuschaltung bei Start Drehzahl > 2600 U/Min), ";
				}
				if (value.contains("140")) {
					returnMessage = returnMessage + "Generator shutdown because engine RPM not steady (Generatorabschaltung - "
							+ "Drehzahl nicht im Drehzahlfenster laenger als 1 Sek), ";
				}
				if (value.contains("151")) {
					returnMessage = returnMessage + "Start release be monitoring system missing (Startfreigabe von Ueberwachung fehlt), ";
				}
				if (value.contains("152")) {
					returnMessage = returnMessage + "No UC Data at initialise - internal fault (NO UC_Daten b. Ini - interner Fehler), ";
				}
				if (value.contains("154")) {
					returnMessage = returnMessage + "No fuel info. - fuel type not identified (Kraftstofftyp nicht erkannt), ";
				}
				if (value.contains("155")) {
					returnMessage = returnMessage + "Different fuel types identified (Unterschiedliche Kraftstofftypen erkannt), ";
				}
				if (value.contains("159")) {
					returnMessage = returnMessage + "Voltage at start - voltage fault before start (Spannung b. Start - "
							+ "Spannungsfehler vor Start), ";
				}
				if (value.contains("160")) {
					returnMessage = returnMessage + "Voltage fault after generator connection (Spannung - Spannungsfehler "
							+ "nach Generatorzuschaltung), ";
				}
				if (value.contains("162")) {
					returnMessage = returnMessage + "Output too high by more than 500 W (Leistung um mehr als 500 Watt zu hoch), ";
				}
				if (value.contains("163")) {
					returnMessage = returnMessage + "Output too low by more than 500 W (Leistung um mehr als 500 Watt zuniedrig), ";
				}
				if (value.contains("164")) {
					returnMessage = returnMessage + "More than +-200 Watt with the system at a standstill (Leistung im "
							+ "Stand - Mehr als +- 200 Watt bei stehender Anlage), ";
				}
				if (value.contains("167")) {
					returnMessage = returnMessage + "Frequency at start - frequency fault before start (Frequenz bei Start "
							+ "- Frequenzfehler vor Start), ";
				}
				if (value.contains("168")) {
					returnMessage = returnMessage + "Frequency fault after generator connection (Frequenzfehler nach "
							+ "Generatorzuschaltung), ";
				}
				if (value.contains("171")) {
					returnMessage = returnMessage + "Oil pressure switch closed for longer than 2.6 s at standstill "
							+ "(Oeldruckschalter im Stillstand laenger als 2.6s geschlossen), ";
				}
				if (value.contains("172")) {
					returnMessage = returnMessage + "Check oil level - oil pressure switch open for longer than 12 s during "
							+ "operation (Oelstand pruefen! - Oeldruckschalter waehrend des Laufes laenger als 12s offen), ";
				}
				if (value.contains("173")) {
					returnMessage = returnMessage + "Gas 1 solenoid valve - leaking, shut-down takes longer than 5 s "
							+ "(MV Gas 1 / Hubmagnet - undicht, Abschaltung dauert laenger als 5 s), ";
				}
				if (value.contains("174")) {
					returnMessage = returnMessage + "Gas 2 solenoid valve - leaking, shut-down takes longer than 5 s "
							+ "(MV Gas 2 - undicht, Abschaltung dauert laenger als 5 s), ";
				}
				if (value.contains("177")) {
					returnMessage = returnMessage + "Maintenance needed - fault can be cleared, not anymore after +300h "
							+ "(Wartung notwendig - 1*taeglich entstoerbar; +300h=>nicht entstoerbar (Wartungsbestaetigung erf.)), ";
				}
				if (value.contains("179")) {
					returnMessage = returnMessage + "4 unsuccessful start attempts - speed < 2300 rpm after 1 min (4 erfolglose "
							+ "Startversuche Drehzahl < 2300 U/min nach 1 Minute), ";
				}
				if (value.contains("180")) {
					returnMessage = returnMessage + "Interruptions during soot filter regeneration > 4 (Unterbrechung "
							+ "RF-Abbrand > 4 - nur bei Oel: 5 Abschaltungen bei Russfilterregeneration), ";
				}
				if (value.contains("184")) {
					returnMessage = returnMessage + "Rotating magnetic field error (Drehfeld falsch - Drehfeld pruefen), ";
				}
				if (value.contains("185")) {
					returnMessage = returnMessage + "Only with oil: Switch open (detecs fluid) (Fluessigkeitsschalter - "
							+ "nur bei Oel: Schalter geoeffnet (erkennt Fluessigkeit)), ";
				}
				if (value.contains("187")) {
					returnMessage = returnMessage + "Overspeed - speed > 3000 rpm (Ueberdrehzahl - Drehzahl > 3000 U/min), ";
				}
				if (value.contains("188")) {
					returnMessage = returnMessage + "Startup unsuccessful, RPM not reached (4 erfolglose Startversuche "
							+ "400 U/min < Drehzahl < 800 U/min), ";
				}
				if (value.contains("189")) {
					returnMessage = returnMessage + "Startup unsuccessful, RPM not reached (4 erfolglose Startversuche "
							+ "Drehzahl < 400 U/min), ";
				}
				if (value.contains("190")) {
					returnMessage = returnMessage + "Speed > 15 rpm before start / oil pressure before start (Drehzahl vor "
							+ "Start > 15 U/min / Oeldruck vor Start), ";
				}
				if (value.contains("191")) {
					returnMessage = returnMessage + "Engine RPM too high (Drehzahl > 3500 U/min - Ueberdrehzahl), ";
				}
				if (value.contains("192")) {
					returnMessage = returnMessage + "Dachs locked by monitoring software (UC verriegelt - Dachs von "
							+ "Ueberwachungssoftware verriegelt), ";
				}
				if (value.contains("200")) {
					returnMessage = returnMessage + "Power grid fault (Fehler Stromnetz - keine genaue Spezifikation moeglich), ";
				}
				if (value.contains("201")) {
					returnMessage = returnMessage + "Internal fault MSR2 controller (Fehler MSR2 intern - keine genaue Spezifikation moeglich), ";
				}
				if (value.contains("202")) {
					returnMessage = returnMessage + "Monitoring controller synchronisation fault - switch Dachs ON and OFF "
							+ "at the engine protection switch (Synchronisierung - Ueberwachungscontroller asynchron, Dachs am "
							+ "Motorschutzschalter aus- und einschalten), ";
				}
				if (value.contains("203")) {
					returnMessage = returnMessage + "Eeprom error (Eeprom defekt - interner Fehler), ";
				}
				if (value.contains("204")) {
					returnMessage = returnMessage + "Different result - internal error (Ergebnis ungleich - interner Fehler), ";
				}
				if (value.contains("205")) {
					returnMessage = returnMessage + "Difference on measuring channel (Dif auf Messkanal - interner Fehler), ";
				}
				if (value.contains("206")) {
					returnMessage = returnMessage + "Multiplexer error (Multiplexer - interner Fehler), ";
				}
				if (value.contains("207")) {
					returnMessage = returnMessage + "Main relay error (Hauptrelais - interner Fehler), ";
				}
				if (value.contains("208")) {
					returnMessage = returnMessage + "A/D converter error (AD-Wandler - interner Fehler), ";
				}
				if (value.contains("209")) {
					returnMessage = returnMessage + "MC supply (Versorgung MCs - interner Fehler), ";
				}
				if (value.contains("210")) {
					returnMessage = returnMessage + "Prog. operation times - 24h shut-down via monitoring (Prog.-laufzeit - "
							+ "24h Abschaltung durch Ueberwachung), ";
				}
				if (value.contains("212")) {
					returnMessage = returnMessage + "Reciprocal identification of the controller faulty (Gegenseitige "
							+ "Identifizierung der Controller fehlerhaft), ";
				}
				if (value.contains("213")) {
					returnMessage = returnMessage + "Prog. throughput internal fault (Prog.-durchlauf - interner Fehler), ";
				}
				if (value.contains("214")) {
					returnMessage = returnMessage + "Internal CAN bus fault (Busfehler intern - Stoerung auf dem internen CAN-Bus), ";
				}
				if (value.contains("215")) {
					returnMessage = returnMessage + "Line break between the generator contactor and generator (Leitungsunterbrechung "
							+ "zwischen Generatorschuetz und Generator), ";
				}
				if (value.contains("216")) {
					returnMessage = returnMessage + "At least one voltage > 280 V (>40ms) (Mindestens eine Spannung > 280 V (>40ms)), ";
				}
				if (value.contains("217")) {
					returnMessage = returnMessage + "An impedance gap > ENS threshold was measured (Impedanz- es wurde ein "
							+ "Impedanzsprung > ENS-Grenzwert gemessen), ";
				}
				if (value.contains("218")) {
					returnMessage = returnMessage + "No voltage present at X22/15 (U-Si am X22 fehlt - an X22/15 liegt keine Spannung an), ";
				}
				if (value.contains("219")) {
					returnMessage = returnMessage + "No voltage present at X5/2 (U-Si Kette fehlt - an X5/2 liegt keine Spannung an), ";
				}
				if (value.contains("220")) {
					returnMessage = returnMessage + "No voltage present at X22/13 (Gasdruck fehlt - an X22/13 liegt keine Spannung an), ";
				}
				if (value.contains("221")) {
					returnMessage = returnMessage + "Acknowledgements - internal fault (Rueckmeldungen - interner Fehler), ";
				}
				if (value.contains("222")) {
					returnMessage = returnMessage + "Generator ack. - signal at X21/7 (Rueckm Generator - Signal an X21/7), ";
				}
				if (value.contains("223")) {
					returnMessage = returnMessage + "Soft start ack. - signal at X21/5 (Rueckm Sanftanlauf - Signal an X21/5), ";
				}
				if (value.contains("224")) {
					returnMessage = returnMessage + "Solenoid valve ack. - check fuse F21 (Rueckm Magnetv. - Sicherung F21 pruefen), ";
				}
				if (value.contains("225")) {
					returnMessage = returnMessage + "Starter ack. - signal at X21/8 (Rueckm Anlasser - Signal an X21/8), ";
				}
				if (value.contains("226")) {
					returnMessage = returnMessage + "Solenoid ack. - check fuse F18 (Rueckm Hubmagnet - Sicherung F18 pruefen), ";
				}
				if (value.contains("250")) {
					returnMessage = returnMessage + "Flow temperature sensor error heating circuit 1 (Vorlauffuehler Heizkreis 1 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("251")) {
					returnMessage = returnMessage + "Flow temperature sensor error heating circuit 1 (Vorlauffuehler Heizkreis 2 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("252")) {
					returnMessage = returnMessage + "Temperature sensor error domestic hot water (Warmwasserfuehler - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("253")) {
					returnMessage = returnMessage + "Temperature sensor 3 error (Fuehler 3 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("254")) {
					returnMessage = returnMessage + "Temperature sensor 4 error (Fuehler 4 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("255")) {
					returnMessage = returnMessage + "Room temperature sensor 1 error (Raumtemp. Fuehler 1 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("256")) {
					returnMessage = returnMessage + "Room temperature sensor 2 error (Raumtemp. Fuehler 2 - Unterbrechung/Kurzschluss), ";
				}
				if (value.contains("270")) {
					returnMessage = returnMessage + "Only with multi module - more than one master controller set (Nur bei "
							+ "MehrModul und LR: mehr als ein Leitregler eingestellt), ";
				}
				if (value.contains("271")) {
					returnMessage = returnMessage + "Only with multi module - duplicate controller address (Nur bei MehrModul "
							+ "und LR: Regler-Adresse mehrfach belegt), ";
				}
				if (value.contains("350")) {
					returnMessage = returnMessage + "EEP data RP not OK, ";
				}
				if (value.contains("354")) {
					returnMessage = returnMessage + "User stack > nominal (User Stack > Soll - interner Fehler), ";
				}
				if (value.contains("355")) {
					returnMessage = returnMessage + "Internal stack > nominal (Int. Stack > Soll - interner Fehler), ";
				}
				// In case the code is not one in the list.
				if (returnMessage.charAt(returnMessage.length() - 2) == ':') {
					returnMessage = returnMessage.substring(0, returnMessage.length() - 2) + " (unknown error code), ";
				}
			}
		}
		return returnMessage;
	}

	/**
	 * Extract the warning code from the server message and return the description of the warning code(s) as a string.
	 *
	 * @param serverMessage the server message
	 * @return the warning message
	 */
	protected String parseWarningCode(String serverMessage) {
		String returnMessage = "";
		String value = this.readEntryAfterString(serverMessage, "Hka_Bd.bWarnung=");
		if (value.length() == 0) {
			// value should contain "0" for no warning. If it is empty, something went wrong.
			returnMessage = "Failed to transmit warning code, ";
		} else {
			if (value.equals("0") == false) {
				returnMessage = "Code " + value + ": ";
				if (value.contains("610")) {
					returnMessage = returnMessage + "Burner does not start - SEplus switched off? (Brenner startet nicht - SEplus ausgeschaltet?), ";
				}
				if (value.contains("620")) {
					returnMessage = returnMessage + "Burner locks - fault clearance necessary (Brenner verriegelt - Entstoerung notwendig), ";
				}
				if (value.contains("630")) {
					returnMessage = returnMessage + "Burner running without demand - internal fault SEplus or false enable "
							+ "wiring at MSR2 (Brennerlauf ohne Anforderung - Interner Fehler SEplus oder falsche Verdrahtung der Freigabe am MSR2), ";
				}
				if (value.contains("650")) {
					returnMessage = returnMessage + "Check hydraulic / Check MSR2-settings (Hydraulik ueberpruefen / "
							+ "MSR2-Einstellungen pruefen), ";
				}
				if (value.contains("652")) {
					returnMessage = returnMessage + "Exhaust heat exchanger/soot filter contaminated, check injection "
							+ "(Abgaswaermetausche/Russfilter verschutzt, Einspritzung ueberpruefen), ";
				}
				if (value.contains("653")) {
					returnMessage = returnMessage + "Exhaust heat exchanger contaminated (Abgaswaermetausche verschutzt), ";
				}
				if (value.contains("654")) {
					returnMessage = returnMessage + "Fault in cooling water flow / cooling water low / calcification "
							+ "(Durchflussstoerung Kuehlwasser / zu wenig Kuehlwasser / Kalkablagerungen), ";
				}
				if (value.contains("655")) {
					returnMessage = returnMessage + "Thermostat defect / ext. pump pushes cooling water into the system "
							+ "(Thermostat defekt / ext. Pumpe druekt Kuehlwasser in die Anlage), ";
				}
				if (value.contains("661")) {
					returnMessage = returnMessage + "Exhaust sensor error, only Dachs G/F (Abgasfuehler Motor-Austritt "
							+ "Unterbrechung/Kurzschluss, nur Dachs G/F), ";
				}
				if (value.contains("662")) {
					returnMessage = returnMessage + "Exhaust temperature too high, only Dachs G/F (Abgastemperatur "
							+ "Motor-Austritt zu hoch, nur Dachs G/F), ";
				}
				if (value.contains("698")) {
					returnMessage = returnMessage + "Fuel pressure absent, only Dachs RS (Kraftstoffdruck fehlt, nur bei Dachs RS), ";
				}
				if (value.contains("699")) {
					returnMessage = returnMessage + "EEPROM data faulty upon initialization (Bei Initialisierung EEPROM Daten fehlerhaft), ";
				}
				if (value.contains("700")) {
					returnMessage = returnMessage + "Dachs cannot start because of power grid errors (voltage, frequency), average U (Brenner verriegelt - Entstoerung notwendig), ";
				}
				if (value.contains("711")) {
					returnMessage = returnMessage + "Only with multi module: No enabling of Flags ’max return temp’ and "
							+ "respective ’temperature’ in spite of pump running (nur MehrModul: keine Freigabe des Flags "
							+ "’Max. Ruecklauftemp’ bzw. ’Temperatur’ trotz Pumpenlauf), ";
				}
				if (value.contains("726")) {
					returnMessage = returnMessage + "Flow temperature not reached after 3h, check sensor placement "
							+ "(Vorlauftemp nach 3h nicht erreicht, Platzierung Vorlauffuehler pruefen), ";
				}
				// In case the code is not one in the list.
				if (returnMessage.charAt(returnMessage.length() - 2) == ':') {
					returnMessage = returnMessage.substring(0, returnMessage.length() - 2) + " (unknown warning code), ";
				}
			}
		}
		return returnMessage;
	}

	/**
	 * Parse the effective electric power from the server message and put the result in the channel EFFECTIVE_ELECTRIC_POWER.
	 * Returns an error message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return an error message on error or an empty string
	 */
	protected String parseAndSetEffectiveElectricPower(String serverMessage) {
		String errorMessage = "";
		String effectiveElectricPowerString = this.readEntryAfterString(serverMessage, "Hka_Mw1.sWirkleistung=");
		if (effectiveElectricPowerString.length() > 0) {
			try {
				this._setEffectiveElectricPower(Double.parseDouble(effectiveElectricPowerString.trim()));
			} catch (NumberFormatException e) {
				errorMessage = errorMessage + "Can't parse effective electrical power (Wirkleistung): " + e.getMessage() + ", ";
				this._setEffectiveElectricPower(NO_DATA);
			}
		} else {
			errorMessage = errorMessage + "Failed to transmit effective electrical power (Wirkleistung), ";
			this._setEffectiveElectricPower(NO_DATA);
		}
		return errorMessage;
	}

	/**
	 * Parse the flow temperature from the server message and put the result in the channel FLOW_TEMPERATURE.
	 * Returns an error message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return an error message on error or an empty string
	 */
	protected String parseAndSetFlowTemperature(String serverMessage) {
		String errorMessage = "";
		String flowTempString = this.readEntryAfterString(serverMessage, "Hka_Mw1.Temp.sbVorlauf=");
		if (flowTempString.length() > 0) {
			try {
				this._setFlowTemperature(Integer.parseInt(flowTempString.trim()) * 10);	// Convert to dezidegree.
			} catch (NumberFormatException e) {
				errorMessage = errorMessage + "Can't parse flow temperature (Vorlauf): " + e.getMessage() + ", ";
				this._setFlowTemperature(NO_DATA);
			}
		} else {
			errorMessage = errorMessage + "Failed to transmit flow temperature (Vorlauf), ";
			this._setFlowTemperature(NO_DATA);
		}
		return errorMessage;
	}

	/**
	 * Parse the return temperature from the server message and put the result in the channel RETURN_TEMPERATURE.
	 * Returns an error message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return an error message on error or an empty string
	 */
	protected String parseAndSetReturnTemperature(String serverMessage) {
		String errorMessage = "";
		String returnTempString = this.readEntryAfterString(serverMessage, "Hka_Mw1.Temp.sbRuecklauf=");
		if (returnTempString.length() > 0) {
			try {
				this._setReturnTemperature(Integer.parseInt(returnTempString.trim()) * 10);	// Convert to dezidegree.
			} catch (NumberFormatException e) {
				errorMessage = errorMessage + "Can't parse return temperature (Ruecklauf): " + e.getMessage() + ", ";
				this._setReturnTemperature(NO_DATA);
			}
		} else {
			errorMessage = errorMessage + "Failed to transmit return temperature (Ruecklauf), ";
			this._setReturnTemperature(NO_DATA);
		}
		return errorMessage;
	}

	/**
	 * Parse the number of requested modules from the server message and put the result in the channel NUMBER_OF_MODULES.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetNumberOfModules(String serverMessage) {
		String warningMessage = "";
		String numberOfModulesString = this.readEntryAfterString(serverMessage, "Hka_Bd.Anforderung.ModulAnzahl=");
		if (numberOfModulesString.length() > 0) {
			try {
				this._setNumberOfModules(Integer.parseInt(numberOfModulesString.trim()));
			} catch (NumberFormatException e) {
				warningMessage = warningMessage + "Can't parse number of requested modules (Anforderung Modul Anzahl): " + e.getMessage() + ", ";
				this._setNumberOfModules(NO_DATA);
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit requested modules (Anforderung Modul Anzahl), ";
			this._setNumberOfModules(NO_DATA);
		}
		return warningMessage;
	}

	/**
	 * Parses the run enable bits from a string and puts the result in the channel RUN_ENABLE_BITS.
	 * Returns an error message if an error occurred, otherwise the return string is empty.
	 *
	 * @param runEnableBitsAsIntString the run clearance bits as an int string
	 * @return an error message on error or an empty string
	 */
	protected String parseAndSetDachsRunEnable(String runEnableBitsAsIntString) {
		String errorMessage = "";
		try {
			int runEnableBits = Integer.parseInt(runEnableBitsAsIntString.trim());
			StringBuilder inHex = new StringBuilder(Integer.toHexString(runEnableBits).toUpperCase());
			for (int i = inHex.length(); i < 4; i++) {
				inHex.insert(0, "0");
			}
			String returnMessage = "Code " + inHex + ": Run enable not given by";
			if ((runEnableBits & 0b01) == 0) {
				returnMessage = returnMessage + " request of CHP (Anforderung Dachs),";
			}
			if ((runEnableBits & 0b010) == 0) {
				returnMessage = returnMessage + " shutoff time (Abschaltzeit),";
			}
			if ((runEnableBits & 0b0100) == 0) {
				returnMessage = returnMessage + " standstill time (Stillstandzeit),";
			}
			if ((runEnableBits & 0b01000) == 0) {
				returnMessage = returnMessage + " run24h (Lauf24h),";
			}
			if ((runEnableBits & 0b010000) == 0) {
				returnMessage = returnMessage + " error (Stoerung),";
			}
			if ((runEnableBits & 0b0100000) == 0) {
				returnMessage = returnMessage + " temperature,";
			}
			if ((runEnableBits & 0b01000000) == 0) {
				returnMessage = returnMessage + " max return temperature (Max Ruecklauftemp),";
			}
			if ((runEnableBits & 0b010000000) == 0) {
				returnMessage = returnMessage + " feedback safety chain (Rueckmeldung SiKette),";
			}
			if ((runEnableBits & 0b0100000000) == 0) {
				returnMessage = returnMessage + " power grid OK (Netz OK),";
			}
			if ((runEnableBits & 0b01000000000) == 0) {
				returnMessage = returnMessage + " startup delay (Startverzoegerung),";
			}
			if ((runEnableBits & 0b010000000000) == 0) {
				returnMessage = returnMessage + " power grid connection OK (Zuschaltung Netz OK),";
			}
			// Bit 0b0100000000000 not used.
			if ((runEnableBits & 0b01000000000000) == 0) {
				returnMessage = returnMessage + " input enable module (Eingang Modulfreigabe),";
			}
			if ((runEnableBits & 0b010000000000000) == 0) {
				returnMessage = returnMessage + " internal enable of CHP (Interne Freigabe Dachs),";
			}
			if ((runEnableBits & 0b0100000000000000) == 0) {
				returnMessage = returnMessage + " key OnOff (Taste OnOff),";
			}
			if ((runEnableBits & 0b01000000000000000) == 0) {
				returnMessage = returnMessage + " commissioning OK (Inbetriebnahme OK).";
			}
			if ((runEnableBits & 0b01111011111111111) == 0b01111011111111111) {
				returnMessage = returnMessage + " unknown.";
			}
			if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
				returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
			}
			this._setRunEnableBits(returnMessage);
		} catch (NumberFormatException e) {
			errorMessage = errorMessage + "Can't parse Dachs run enable bits (Freigabe): " + e.getMessage() + ", ";
			this.logError(this.log, "Error, can't parse Dachs run enable bits: " + e.getMessage());
			this._setRunEnableBits("Code " + runEnableBitsAsIntString + ": Error parsing code.");
		}
		return errorMessage;
	}

	/**
	 * Parses the run request bits to a string.
	 *
	 * @param runRequestBits the run request bits
	 * @return the parsed string
	 */
	protected String parseRunRequestBits(int runRequestBits) {
		String returnMessage = "Code " + Integer.toHexString(runRequestBits).toUpperCase() + ": Running requested by";
		if ((runRequestBits & 0b01) == 0b01) {
			returnMessage = returnMessage + " min. operation time (Mindestlaufzeit),";
		}
		if ((runRequestBits & 0b010) == 0b010) {
			returnMessage = returnMessage + " heat (Waerme),";
		}
		if ((runRequestBits & 0b0100) == 0b0100) {
			returnMessage = returnMessage + " domestic hot water generation (BW Bereitung),";
		}
		if ((runRequestBits & 0b01000) == 0b01000) {
			returnMessage = returnMessage + " high setpoint (Hoher Sollwert),";
		}
		if ((runRequestBits & 0b010000) == 0b010000) {
			returnMessage = returnMessage + " external (Extern),";
		}
		if ((runRequestBits & 0b0100000) == 0b0100000) {
			returnMessage = returnMessage + " manual (Manuell),";
		}
		if ((runRequestBits & 0b01000000) == 0b01000000) {
			returnMessage = returnMessage + " electricity (Strom),";
		}
		if ((runRequestBits & 0b010000000) == 0b010000000) {
			returnMessage = returnMessage + " multi module (Mehrmodul),";
		}
		if ((runRequestBits & 0b0100000000) == 0b0100000000) {
			returnMessage = returnMessage + " cyclical operation (Zyklischer Dachslauf).";
		}
		if ((runRequestBits & 0b0111111111) == 0) {
			returnMessage = " unknown.";
		}
		if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
			returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
		}
		return returnMessage;
	}

	/**
	 * Parses the enable electricity bits from the server message and puts the result in the channel
	 * ENABLE_ELECTRICITY_BITS.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetEnableElectricity(String serverMessage) {
		String warningMessage = "";
		String enableElectricityBitsString = this.readEntryAfterString(serverMessage, "Hka_Bd.UStromF_Frei.bFreigabe=");
		if (enableElectricityBitsString.length() > 0) {
			if (enableElectricityBitsString.equals("255")) {
				this._setEnableElectricityBits("Code FF: Enable electricity is true.");
			} else {
				try {
					int enableElectricityBits = Integer.parseInt(enableElectricityBitsString.trim());
					String inHex = Integer.toHexString(enableElectricityBits).toUpperCase();
					String returnMessage = "Code " + inHex + ": Enable electricity is false because the following is missing -";
					if ((enableElectricityBits & 0b01) == 0) {
						returnMessage = returnMessage + " Electricity demand (Anforderung Strom),";
					}
					if ((enableElectricityBits & 0b010) == 0) {
						returnMessage = returnMessage + " MaxElectricity (MaxStrom),";
					}
					if ((enableElectricityBits & 0b0100) == 0) {
						returnMessage = returnMessage + " HtLt (HtNt),";
					}
					if ((enableElectricityBits & 0b01000) == 0) {
						returnMessage = returnMessage + " SuWi (SoWi).";
					}
					if ((enableElectricityBits & 0b01111) == 0b01111) {
						returnMessage = returnMessage + " unknown.";
					}
					if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
						returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
					}
					this._setEnableElectricityBits(returnMessage);
				} catch (NumberFormatException e) {
					warningMessage = "Can't parse enable electricity code (Freigabe Stromfuehrung): " + e.getMessage() + ", ";
					this._setEnableElectricityBits("Code " + enableElectricityBitsString + ": Error parsing code.");
				}
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit electricity guided mode clearance code (Freigabe Stromfuehrung), ";
			this._setEnableElectricityBits("Failed to transmit electricity guided mode clearance code (Freigabe Stromfuehrung).");
		}
		return warningMessage;
	}

	/**
	 * Parses the electricity request bits from the server message and puts the result in the channel
	 * ELECTRICITY_REQUEST_BITS.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetElectricityRequestBits(String serverMessage) {
		String warningMessage = "";
		String electricityRequestBitsString = this.readEntryAfterString(serverMessage, "Hka_Bd.Anforderung.UStromF_Anf.bFlagSF=");
		if (electricityRequestBitsString.length() > 0) {
			if (electricityRequestBitsString.equals("0")) {
				this._setElectricityRequestBits("Code 0: No component is requesting electricity.");
			} else {
				try {
					int tempInt = Integer.parseInt(electricityRequestBitsString.trim());
					String returnMessage = "Code " + Integer.toHexString(tempInt).toUpperCase() + ": Electricity requested by";
					if ((tempInt & 0b01) == 0b01) {
						returnMessage = returnMessage + " Can external,";
					}
					if ((tempInt & 0b010) == 0b010) {
						returnMessage = returnMessage + " Internal timer (Uhr intern),";
					}
					if ((tempInt & 0b0100) == 0b0100) {
						returnMessage = returnMessage + " DigExternal,";
					}
					if ((tempInt & 0b01000) == 0b01000) {
						returnMessage = returnMessage + " Energy meter 1 (Energie Zaehler 1),";
					}
					if ((tempInt & 0b010000) == 0b010000) {
						returnMessage = returnMessage + " Energy meter 2 (Energie Zaehler 2),";
					}
					if ((tempInt & 0b0100000) == 0b0100000) {
						returnMessage = returnMessage + " Siemens remote control (Siemens Fernbedienung).";
					}
					if ((tempInt & 0b0111111) == 0) {
						returnMessage = " unknown.";
					}
					if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
						returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
					}
					this._setElectricityRequestBits(returnMessage);
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse electricity request bits (Anforderungen Stromfuehrung): " + e.getMessage() + ", ";
					this._setElectricityRequestBits("Code " + electricityRequestBitsString + ": Error parsing code.");
				}
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit electricity request bits (Anforderungen Stromfuehrung), ";
			this._setElectricityRequestBits("Failed to transmit electricity request bits (Anforderungen Stromfuehrung).");
		}
		return warningMessage;
	}

	/**
	 * Parses the electricity demand flag from the server message and puts the result in the channel
	 * ELECTRICITY_REQUEST_FLAG.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetElectricityDemandFlag(String serverMessage) {
		String warningMessage = "";
		String electricModeRunFlagString = this.readEntryAfterString(serverMessage, "Hka_Bd.UHka_Anf.Anforderung.fStrom=");
		if (electricModeRunFlagString.length() > 0) {
			if (electricModeRunFlagString.equals("true")) {
				this._setElectricityRequestFlag(true);
			} else {
				this._setElectricityRequestFlag(false);
			}
		} else {
			warningMessage = "Failed to transmit electricity guided mode run flag (Anforderung Strom), ";
			this._setElectricityRequestFlag(false);
		}
		return warningMessage;
	}

	/**
	 * Parses the generated electrical work from the server message and puts the result in the channel ELECTRICAL_WORK.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetGeneratedElectricalWork(String serverMessage) {
		String warningMessage = "";
		String electricalWorkString = this.readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitElektr=");
		if (electricalWorkString.length() > 0) {
			try {
				this._setElectricalWork(Double.parseDouble(electricalWorkString));
			} catch (NumberFormatException e) {
				warningMessage = warningMessage + "Can't parse generated electrical work (Erzeugte elektrische Arbeit): " + e.getMessage() + ", ";
				this._setElectricalWork(NO_DATA);
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit generated electrical work (Erzeugte elektrische Arbeit), ";
			this._setElectricalWork(NO_DATA);
		}
		return warningMessage;
	}

	/**
	 * Parses the generated thermal work from the server message and puts the result in the channel THERMAL_WORK.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetGeneratedThermalWork(String serverMessage) {
		String warningMessage = "";
		String thermalWorkString = this.readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitThermHka=");
		if (thermalWorkString.length() > 0) {
			try {
				this._setThermalWork(Double.parseDouble(thermalWorkString));
			} catch (NumberFormatException e) {
				warningMessage = warningMessage + "Can't parse generated thermal work (Erzeugte thermische Arbeit): " + e.getMessage() + ", ";
				this._setThermalWork(NO_DATA);
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit generated thermal work (Erzeugte thermische Arbeit), ";
			this._setThermalWork(NO_DATA);
		}
		return warningMessage;
	}

	/**
	 * Parses the generated thermal work condenser from the server message and puts the result in the channel THERMAL_WORK.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetGeneratedThermalWorkCond(String serverMessage) {
		String warningMessage = "";
		String thermalWorkCondString = this.readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitThermKon=");
		if (thermalWorkCondString.length() > 0) {
			try {
				this._setThermalWorkCond(Double.parseDouble(thermalWorkCondString));
			} catch (NumberFormatException e) {
				warningMessage = warningMessage + "Can't parse generated thermal work condenser (Erzeugte thermische Arbeit Kondenser): " + e.getMessage() + ", ";
				this._setThermalWorkCond(NO_DATA);
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit generated thermal work condenser (Erzeugte thermische Arbeit Kondenser), ";
			this._setThermalWorkCond(NO_DATA);
		}
		return warningMessage;
	}

	/**
	 * Parses the runtime since restart from the server message and puts the result in the channel RUNTIME.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetRuntimeSinceRestart(String serverMessage) {
		String warningMessage = "";
		String runtimeSinceRestartString = this.readEntryAfterString(serverMessage, "Hka_Bd.ulBetriebssekunden=");
		if (runtimeSinceRestartString.length() > 0) {
			try {
				this._setRuntimeSinceRestart(Double.parseDouble(runtimeSinceRestartString));
			} catch (NumberFormatException e) {
				warningMessage = warningMessage + "Can't parse runtime since restart (Betriebsstunden): " + e.getMessage() + ", ";
				this._setRuntimeSinceRestart(NO_DATA);
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit runtime since restart (Betriebsstunden), ";
			this._setRuntimeSinceRestart(NO_DATA);
		}
		return warningMessage;
	}

	/**
	 * Parses the engine starts from the server message and puts the result in the channel ENGINE_STARTS.
	 * Returns a warning message if an error occurred, otherwise the return string is empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message on error or an empty string
	 */
	protected String parseAndSetEngineStarts(String serverMessage) {
		String warningMessage = "";
		String engineStarts = this.readEntryAfterString(serverMessage, "Hka_Bd.ulAnzahlStarts=");
		if (engineStarts.length() > 0) {
			try {
				this._setEngineStarts(Integer.parseInt(engineStarts.trim()));
			} catch (NumberFormatException e) {
				warningMessage = warningMessage + "Can't parse engine starts (Anzahl Starts): " + e.getMessage() + ", ";
				this._setEngineStarts(NO_DATA);
			}
		} else {
			warningMessage = warningMessage + "Failed to transmit engine starts (Anzahl Starts), ";
			this._setEngineStarts(NO_DATA);
		}
		return warningMessage;
	}

	/**
	 * Parses the maintenance flag from the server message and puts the result in the channel MAINTENANCE.
	 * Returns a warning message if the maintenance flag is true or if an error occurred. Otherwise the return string is
	 * empty.
	 *
	 * @param serverMessage the server message
	 * @return a warning message if maintenance is needed or on error, otherwise an empty string
	 */
	protected String parseAndSetMaintenanceFlag(String serverMessage) {
		String warningMessage = "";
		String maintenanceFlagString = this.readEntryAfterString(serverMessage, "Wartung_Cache.fStehtAn=");
		if (maintenanceFlagString.length() > 0) {
			if (maintenanceFlagString.equals("true")) {
				this._setMaintenanceFlag(true);
				warningMessage = "Maintenance needed (Wartung steht an), ";
			} else {
				this._setMaintenanceFlag(false);
			}
		} else {
			warningMessage = "Failed to transmit maintenance flag (Wartung steht an), ";
			this._setMaintenanceFlag(false);
		}
		return warningMessage;
	}

    /**
	 * Read the serial number and parts number.
	 * Separate method for these as they don't change and only need to be requested once.
	 */
    protected void getSerialAndPartsNumber() {
        String serverMessage = this.getKeyDachs("k=Hka_Bd_Stat.uchSeriennummer&k=Hka_Bd_Stat.uchTeilenummer");
        if (serverMessage.contains("Hka_Bd_Stat.uchSeriennummer=")) {
			this._setSerialNumber(this.readEntryAfterString(serverMessage, "Hka_Bd_Stat.uchSeriennummer="));
			this._setPartsNumber(this.readEntryAfterString(serverMessage, "Hka_Bd_Stat.uchTeilenummer="));
        } else {
			// Writing to log here is ok as this executes only once.
			this.logError(this.log, "Couldn't read data from GLT interface.");
        }
    }

    /**
	 * Extract a value from the server return message. ’message’ is the return message from the server. ’key’ is the
	 * value after which you want to read. Reads until the end of the line.
	 *
	 * @param message the return message from the server
	 * @param key the value after which you want to read
	 * @return the string after ’key’, until the end of the line
	 */
    protected String readEntryAfterString(String message, String key) {
		if (message.contains(key)) {
			return message.substring(message.indexOf(key) + key.length(), message.indexOf("/n", message.indexOf(key)));
		} else {
			return "";
		}
	}

    /**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
    protected void printDataToLog() {
    	if (this.basicInfo) {
    		this.logInfo(this.log, "---- CHP Senertec Dachs ----");
    		this.logInfo(this.log, "Engine rpm: " + getRpm());
    		this.logInfo(this.log, "Flow temp: " + getFlowTemperature());
    		this.logInfo(this.log, "Return temp: " + getReturnTemperature());
    		this.logInfo(this.log, "Effective electric power: " + getEffectiveElectricPower());
    		this.logInfo(this.log, "Heater state: " + getHeaterState());
			this.logInfo(this.log, "Warning message: " + getWarningMessage());
    		this.logInfo(this.log, "Error message: " + getErrorMessage());
    	}
    	if (this.debug) {
    		this.logInfo(this.log, "Serial number: " + getSerialNumber());
    		this.logInfo(this.log, "Parts number: " + getPartsNumber());
    		this.logInfo(this.log, "Engine starts: " + getEngineStarts());
    		this.logInfo(this.log, "Runtime: " + getRuntimeSinceRestart());
    		this.logInfo(this.log, "Run request message: " + getRequestOfRunBits());
    		this.logInfo(this.log, "Not ready message: " + getRunEnableBits());
    		this.logInfo(this.log, "Number of modules requested: " + getNumberOfModules());
    		this.logInfo(this.log, "Electricity guided operation clearance: " + getEnableElectricityBits());
    		this.logInfo(this.log, "Electricity guided operation settings: " + getElectricityRequestBits());
    		this.logInfo(this.log, "Electricity guided operation run flag: " + getElectricityRequestFlag());
    		this.logInfo(this.log, "Electrical work done: " + getElectricalWork());
    		this.logInfo(this.log, "Thermal work done: " + getThermalWork());
    		this.logInfo(this.log, "Thermal work condenser done: " + getThermalWorkCond());
    	}
    }

	/**
	 * Returns the debug message.
	 *
	 * @return the debug message.
	 */
	public String debugLog() {
		String debugMessage = this.getHeaterState().asEnum().asCamelCase() //
				+ "|F:" + this.getFlowTemperature().asString() //
				+ "|R:" + this.getReturnTemperature().asString(); //
		if (this.getWarningMessage().get().equals("No warning") == false) {
			debugMessage = debugMessage + "|Warning";
		}
		if (this.getErrorMessage().get().equals("No error") == false) {
			debugMessage = debugMessage + "|Error";
		}
		return debugMessage;
	}

	/**
	 * Send read request to server.
	 *
	 * @param key the request string.
	 * @return the answer string.
	 */
	private String getKeyDachs(String key) {
		String message = "";
		try {
            URL url = new URL("http://" + this.urlBuilderIP + ":8081/getKey?" + key);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", this.basicAuth);
			this.is = connection.getInputStream();

            // Read text returned by server
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.is));
            String line;
            while ((line = reader.readLine()) != null) {
            	if (this.debug) {
            		this.logInfo(this.log, line);
            	}
                message = message + line + "/n";
            }
            reader.close();

		} catch (MalformedURLException e) {
			this.logError(this.log, "Malformed URL: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log, "I/O Error: " + e.getMessage());
            if (e.getMessage().contains("code: 401")) {
                this.logError(this.log, "Wrong user/password. Access refused.");
            } else if (e.getMessage().contains("code: 404") || e.getMessage().contains("Connection refused")) {
                this.logError(this.log, "No GLT interface at specified address.");
            }
		} finally {
			if (this.is != null) {
                try {
					this.is.close();
                } catch (IOException e) {
                    this.logError(this.log, "I/O Error: " + e.getMessage());
                }
            }
		}

		return message;
	}

	/**
	 * Send write request to server.
	 *
	 * @param key the request string.
	 * @return the answer string.
	 */
	private String setKeysDachs(String key) {
		String message = "";
		try {
			URL url = new URL("http://" + this.urlBuilderIP + ":8081/setKeys");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", this.basicAuth);
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", String.valueOf(key.length()));

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(key);
			writer.flush();
			writer.close();

			this.is = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(this.is));
			String line;
			while ((line = reader.readLine()) != null) {
				message = message + line + "/n";
			}
			reader.close();

		} catch (MalformedURLException e) {
			this.logError(this.log, "Malformed URL: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log, "I/O Error: " + e.getMessage());
		} finally {
			if (this.is != null) {
                try {
					this.is.close();
                } catch (IOException e) {
                    this.logError(this.log, "I/O Error: " + e.getMessage());
                }
            }
		}

		return message;
	}

	/**
	 * Activate the Dachs.
	 */
	protected void activateDachs() {
		String returnMessage = this.setKeysDachs("Stromf_Ew.Anforderung_GLT.bAktiv=1");
		if (this.debug) {
			this.logInfo(this.log, "Sending \"run request\" signal to Dachs Chp. Return message: " + returnMessage);
		}
	}

	/**
	 * Deactivate the Dachs.
	 */
	protected void deactivateDachs() {
		String returnMessage = this.setKeysDachs("Stromf_Ew.Anforderung_GLT.bAktiv=0");
		if (this.debug) {
			this.logInfo(this.log, "Sending \"no need to run\" signal to Dachs Chp. Return message: " + returnMessage);
		}
	}

}
