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

import java.io.*;
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
 * The module is written to be used with the Heater interface methods. However, this chp does not have variable power
 * control. So the methods setHeatingPowerSetpoint(), setElectricPowerSetpoint() and setTemperatureSetpoint() are not
 * available. This also means power control with ExceptionalState value is not possible.
 * Some sort of power control is possible if it is a bigger chp containing several units. It is possible to set
 * the number of modules that should turn on. This is not coded in yet.
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
	private final int MAX_INTERVAL = 540;		// unit is seconds.
	private LocalDateTime timestamp;
	private boolean debug;
	private boolean basicInfo;

	private boolean componentEnabled;
	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private boolean readyForCommands;
	private boolean chpEngineRunning;

	private boolean turnOnChp;
	private boolean useEnableSignal;
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

		this.componentEnabled = config.enabled();

		// Limit interval to 9 minutes max. Because on/off command needs to be sent to Dachs at least once every 10 minutes.
		this.interval = Math.min(config.interval(), this.MAX_INTERVAL);

		this.timestamp = LocalDateTime.now().minusSeconds(this.interval);		// Subtract interval, so polling starts immediately.
		this.urlBuilderIP = config.address();
		String gltpass = config.username() + ":" + config.password();
		this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(gltpass.getBytes()));
		this.getSerialAndPartsNumber();
		this.debug = config.debug();
		this.basicInfo = config.basicInfo();

		this.readOnly = config.readOnly();
		this.startupStateChecked = false;
		if (this.readOnly == false) {
			this.turnOnChp = config.turnOnChp();
			TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
			this.useEnableSignal = config.useEnableSignalChannel();
			if (this.useEnableSignal) {
				timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.enableSignalTimerId(), config.waitTimeEnableSignal());
				this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
			}
			this.useExceptionalState = config.useExceptionalState();
			if (this.useExceptionalState) {
				timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.exceptionalStateTimerId(), config.waitTimeExceptionalState());
				this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
			}
		}
		if (this.componentEnabled == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}
	}

	@Deactivate
	protected void deactivate() { super.deactivate(); }

	@Override
	public void run() throws OpenemsError.OpenemsNamedException {

		if (this.componentEnabled) {
			// How often the Dachs is polled is determined by "interval"
			if (ChronoUnit.SECONDS.between(this.timestamp, LocalDateTime.now()) >= this.interval) {
				this.updateChannels();
				this.timestamp = LocalDateTime.now();

				// Output to log depending on config settings.
				this.printDataToLog();

				if (this.readOnly == false && this.readyForCommands) {

					if (this.useEnableSignal) {

						// This is supposed to be the on-off switch.
						// Use the "ENABLE_SIGNAL" channel like a write channel is used with Modbus. getEnableSignal() is the readout,
						// setEnableSignal(value) is the write command. The two values are separate, but should be at the same value
						// (with maybe a minor delay) when everything works as intended.
						// There are some things to watch out for:
						// - This is not a hard command, especially the "off" command. The Dachs has a list of reasons to be running
						// 	 (see Dachs-Lauf-Anforderungen), the "external requirement" (this on/off switch) being one of many. If
						// 	 any one of those reasons is true, it is running. Only if all of them are false, it will shut down.
						// 	 Bottom line, only if nothing else tells the Dachs to run will "ENABLE_SIGNAL = false" do anything.
						//   And "ENABLE_SIGNAL = true" might be ignored because of a limitation.
						// - Timing: need to send "on" command at least every 10 minutes for the Dachs to keep running.
						//   "interval" is capped at 9 minutes, so this should be taken care of.
						// - Also: You cannot switch a CHP on/off as you want. There is a limit on how often you can start. Number of
						//   starts should be minimized. Currently the code does not enforce any restrictions in this regard!
						this.turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

						// If the component has just been started, it will most likely take a few cycles before a controller
						// sends an EnableSignal (assuming the CHP should be running). Since no EnableSignal means ’turn off the
						// CHP’, the component will always turn off the CHP during the first few cycles. If the CHP is already
						// on, this would turn the CHP off and on again, which is bad for the lifetime. A scenario where this
						// would happen is if the component or OpenEMS is restarted while the CHP is running.
						// To avoid that, check the CHP status at the startup of the component. If it is on, the component sends
						// the EnableSignal to itself once to keep the CHP on until the timer runs out. This gives any
						// controllers enough time to send the EnableSignal themselves.
						if (this.startupStateChecked == false) {
							this.startupStateChecked = true;
							this.turnOnChp = this.chpEngineRunning;
							if (this.turnOnChp) {
								try {
									this.getEnableSignalChannel().setNextWriteValue(true);
								} catch (OpenemsError.OpenemsNamedException e) {
									this.log.warn("Couldn't write in Channel " + e.getMessage());
								}
							}
						}

					}

					// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
					if (this.useExceptionalState) {
						boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
						if (exceptionalStateActive) {
							int exceptionalStateValue = this.getExceptionalStateValue();
							if (exceptionalStateValue <= 0) {
								this.turnOnChp = false;
							} else {
								this.turnOnChp = true;
							}
						}
					}

					if (this.turnOnChp) {
						this.activateDachs();
					} else {
						this.deactivateDachs();
					}
				}
			}
		}
	}

	// This method communicates with the chp and processes the response. 
	protected void updateChannels() {
		// "getKeyDachs(...)" is the method to request data from the chp. The answer is saved to "serverMessage".
		String serverMessage = getKeyDachs(		//For a description of these arguments, look in DachsGltInterfaceChannel
                        "k=Wartung_Cache.fStehtAn&" +
						"k=Hka_Bd.ulAnzahlStarts&" +
                        "k=Hka_Mw1.usDrehzahl&" +
						"k=Hka_Bd.ulBetriebssekunden&" +
						"k=Hka_Bd.ulArbeitThermKon&" +
                        "k=Hka_Bd.ulArbeitThermHka&" +
						"k=Hka_Bd.ulArbeitElektr&" +
						"k=Hka_Bd.Anforderung.UStromF_Anf.bFlagSF&" +
						"k=Hka_Bd.UHka_Anf.Anforderung.fStrom&" +
						"k=Hka_Bd.UStromF_Frei.bFreigabe&" +
						"k=Hka_Bd.Anforderung.ModulAnzahl&" +
						"k=Hka_Bd.UHka_Anf.usAnforderung&" +
						"k=Hka_Bd.UHka_Frei.usFreigabe&" +
                        "k=Hka_Mw1.Temp.sbRuecklauf&" +
                        "k=Hka_Mw1.Temp.sbVorlauf&" +
						"k=Hka_Mw1.sWirkleistung&" +
						"k=Hka_Bd.bWarnung&" +
                        "k=Hka_Bd.bStoerung");

		// The return message now needs to be processed. The message is queried for a marker, then the value after the
		// marker is read. Then the value is parsed according to the description in the manual. The result is then
		// written into the corresponding channel.

		// Test if a marker can be found in the message to see if the server message is ok. If this marker can not be
		// found, the server message is most likely garbage.
		if (serverMessage.contains("Hka_Bd.bStoerung=")) {
			this.readyForCommands = true;
			
			// Parse error message.
			String errorMessage = "";
			String stoerung = this.readEntryAfterString(serverMessage, "Hka_Bd.bStoerung=");		// already checked that this entry exists in serverMessage.
			if (stoerung.length() == 0) {
				// stoerung should contain "0" for no error or the number of the error code(s). If stoerung contains 
				// nothing, something went wrong.
				errorMessage = "Failed to transmit error code, ";
			} else {
				if (stoerung.equals("0") == false) {
					errorMessage = "Code " + stoerung + ": ";
					if (stoerung.contains("101")) {
						errorMessage = errorMessage + "Abgasfühler HKA-Austritt - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("102")) {
						errorMessage = errorMessage + "Kühlwasserfühler Motor - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("103")) {
						errorMessage = errorMessage + "Kühlwasserfühler Generator - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("104")) {
						errorMessage = errorMessage + "Abgasfühler Motor-Austritt - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("105")) {
						errorMessage = errorMessage + "Vorlauftemperatur - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("106")) {
						errorMessage = errorMessage + "Rücklauftemperatur - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("107")) {
						errorMessage = errorMessage + "Fühler 1 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("108")) {
						errorMessage = errorMessage + "Fühler 2 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("109")) {
						errorMessage = errorMessage + "Außenfühler - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("110")) {
						errorMessage = errorMessage + "Kapselfühler - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("111")) {
						errorMessage = errorMessage + "Fühler Regler intern - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("120")) {
						errorMessage = errorMessage + "Abgastemperatur Motor-Austritt - zu hoch;G:>620°C,HR:>520°C, ";
					}
					if (stoerung.contains("121")) {
						errorMessage = errorMessage + "Kapseltemperatur - zu hoch; > 120°C, ";
					}
					if (stoerung.contains("122")) {
						errorMessage = errorMessage + "Kühlwassertemperatur Motor (Austritt) - zu hoch; > 95°C, ";
					}
					if (stoerung.contains("123")) {
						errorMessage = errorMessage + "Abgastemperatur HKA-Austritt - zu hoch; > 210°C, ";
					}
					if (stoerung.contains("124")) {
						errorMessage = errorMessage + "Kühlwassertemperatur Generator (Eintritt) - zu hoch; > 77°C, ";
					}
					if (stoerung.contains("129")) {
						errorMessage = errorMessage + "Rückleistung - Brennstoffversorgung oder Zündung fehlerhaft, ";
					}
					if (stoerung.contains("130")) {
						errorMessage = errorMessage + "Drehzahl nach Anlasser AUS - Drehzahl trotz ausgeschaltetem Anlasser bei Fehlstart, ";
					}
					if (stoerung.contains("131")) {
						errorMessage = errorMessage + "HKA-Anlauf < 100 U/min - 1 sek nach Anlasser ein: n < 100 U/min, ";
					}
					if (stoerung.contains("133")) {
						errorMessage = errorMessage + "HKA-Lauf < 2300 U/min - n<2300 U/min für 30 sek nach Erreichen 800 U/min, ";
					}
					if (stoerung.contains("139")) {
						errorMessage = errorMessage + "Generatorzuschaltung - keine Zuschaltung bei Start Drehzahl > 2600 U/Min, ";
					}
					if (stoerung.contains("140")) {
						errorMessage = errorMessage + "Generatorabschaltung - Drehzahl nicht im Drehzahlfenster länger als 1 Sek, ";
					}
					if (stoerung.contains("151")) {
						errorMessage = errorMessage + "Startfreigabe - Startfreigabe von Überwachung fehlt, ";
					}
					if (stoerung.contains("152")) {
						errorMessage = errorMessage + "NO UC_Daten b. Ini - interner Fehler, ";
					}
					if (stoerung.contains("154")) {
						errorMessage = errorMessage + "NO KraftstoffInfo - Kraftstofftyp nicht erkannt, ";
					}
					if (stoerung.contains("155")) {
						errorMessage = errorMessage + "Dif. Kraftstofftyp - unterschiedliche Kraftstofftypen erkannt, ";
					}
					if (stoerung.contains("159")) {
						errorMessage = errorMessage + "Spannung b. Start - Spannungsfehler vor Start, ";
					}
					if (stoerung.contains("160")) {
						errorMessage = errorMessage + "Spannung - Spannungsfehler nach Generatorzuschaltung, ";
					}
					if (stoerung.contains("162")) {
						errorMessage = errorMessage + "Leistung zu hoch - Leistung um mehr als 500 Watt zu hoch, ";
					}
					if (stoerung.contains("163")) {
						errorMessage = errorMessage + "Leistung zu klein - Leistung um mehr als 500 Watt zuniedrig, ";
					}
					if (stoerung.contains("164")) {
						errorMessage = errorMessage + "Leistung im Stand - Mehr als +- 200 Watt bei stehenderAnlage, ";
					}
					if (stoerung.contains("167")) {
						errorMessage = errorMessage + "Frequenz bei Start - Frequenzfehler vor Start, ";
					}
					if (stoerung.contains("168")) {
						errorMessage = errorMessage + "Frequenz - Frequenzfehler nachGeneratorzuschaltung, ";
					}
					if (stoerung.contains("171")) {
						errorMessage = errorMessage + "Öldruckschalter - Öldruckschalter im Stillstand länger als 2.6s geschlossen, ";
					}
					if (stoerung.contains("172")) {
						errorMessage = errorMessage + "Ölstand prüfen! - Öldruckschalter während des Laufes länger als 12s offen, ";
					}
					if (stoerung.contains("173")) {
						errorMessage = errorMessage + "MV Gas 1 / Hubmagnet - undicht, Abschaltung dauert länger als 5 s, ";
					}
					if (stoerung.contains("174")) {
						errorMessage = errorMessage + "MV Gas 2 - undicht, Abschaltung dauert länger als 5 s, ";
					}
					if (stoerung.contains("177")) {
						errorMessage = errorMessage + "Wartung notwendig - 1*täglich entstörbar; +300h=>nicht entstörbar (Wartungsbestätigung erf.), ";
					}
					if (stoerung.contains("179")) {
						errorMessage = errorMessage + "4 Starts < 2300 U/min - 4 erfolglose Startversuche Drehzahl < 2300 U/min nach 1 Minute, ";
					}
					if (stoerung.contains("180")) {
						errorMessage = errorMessage + "Unterbrechung RF-Abbrand > 4 - nur bei Öl: 5 Abschaltungen bei Russfilterregeneration, ";
					}
					if (stoerung.contains("184")) {
						errorMessage = errorMessage + "Drehfeld falsch - Drehfeld prüfen, ";
					}
					if (stoerung.contains("185")) {
						errorMessage = errorMessage + "Flüssigkeitsschalter - nur bei Öl: Schalter geöffnet (erkennt Flüssigkeit), ";
					}
					if (stoerung.contains("187")) {
						errorMessage = errorMessage + "Überdrehzahl - Drehzahl>3000 U/min, ";
					}
					if (stoerung.contains("188")) {
						errorMessage = errorMessage + "4 Starts 400 - 800 U/min - 4 erfolglose Startversuche 400 U/min < Drehzahl < 800 U/min, ";
					}
					if (stoerung.contains("189")) {
						errorMessage = errorMessage + "4 Starts < 400 U/min - 4 erfolglose Startversuche Drehzahl < 400 U/min, ";
					}
					if (stoerung.contains("190")) {
						errorMessage = errorMessage + "Drehzahl > 15 U/min vor Start - Drehzahl vor Start > 15 U/m / Öldruck vor Start, ";
					}
					if (stoerung.contains("191")) {
						errorMessage = errorMessage + "Drehzahl > 3500 U/min - Überdrehzahl, ";
					}
					if (stoerung.contains("192")) {
						errorMessage = errorMessage + "UC verriegelt - Dachs von Überwachungssoftware verriegelt, ";
					}
					if (stoerung.contains("200")) {
						errorMessage = errorMessage + "Fehler Stromnetz - keine genaue Spezifikation möglich, ";
					}
					if (stoerung.contains("201")) {
						errorMessage = errorMessage + "Fehler MSR2 intern - keine genaue Spezifikation möglich, ";
					}
					if (stoerung.contains("202")) {
						errorMessage = errorMessage + "Synchronisierung - Überwachungscontroller asynchron, Dachs am "
								+ "Motorschutzschalter aus- und einschalten, ";
					}
					if (stoerung.contains("203")) {
						errorMessage = errorMessage + "Eeprom defekt - interner Fehler, ";
					}
					if (stoerung.contains("204")) {
						errorMessage = errorMessage + "Ergebnis ungleich - interner Fehler, ";
					}
					if (stoerung.contains("205")) {
						errorMessage = errorMessage + "Dif auf Messkanal - interner Fehler, ";
					}
					if (stoerung.contains("206")) {
						errorMessage = errorMessage + "Multiplexer - interner Fehler, ";
					}
					if (stoerung.contains("207")) {
						errorMessage = errorMessage + "Hauptrelais - interner Fehler, ";
					}
					if (stoerung.contains("208")) {
						errorMessage = errorMessage + "AD-Wandler - interner Fehler, ";
					}
					if (stoerung.contains("209")) {
						errorMessage = errorMessage + "Versorgung MCs - interner Fehler, ";
					}
					if (stoerung.contains("210")) {
						errorMessage = errorMessage + "Prog.-laufzeit - 24h Abschaltung durch Überwachung, ";
					}
					if (stoerung.contains("212")) {
						errorMessage = errorMessage + "Identifizierung - gegenseitige Identifizierung der Controller fehlerhaft, ";
					}
					if (stoerung.contains("213")) {
						errorMessage = errorMessage + "Prog.-durchlauf - interner Fehler, ";
					}
					if (stoerung.contains("214")) {
						errorMessage = errorMessage + "Busfehler intern - Störung auf dem internen CAN-Bus, ";
					}
					if (stoerung.contains("215")) {
						errorMessage = errorMessage + "Leitungsbruch Gen - Leitungsunterbrechung zwischen Generatorschütz und Generator, ";
					}
					if (stoerung.contains("216")) {
						errorMessage = errorMessage + "Spannung > 280V - mindestens eine Spannung > 280 V (>40ms), ";
					}
					if (stoerung.contains("217")) {
						errorMessage = errorMessage + "Impedanz- es wurde ein Impedanzsprung > ENS-Grenzwert gemessen, ";
					}
					if (stoerung.contains("218")) {
						errorMessage = errorMessage + "U-Si am X22 fehlt - an X22/15 liegt keine Spannung an, ";
					}
					if (stoerung.contains("219")) {
						errorMessage = errorMessage + "U-SiKette fehlt - an X5/2 liegt keine Spannung an, ";
					}
					if (stoerung.contains("220")) {
						errorMessage = errorMessage + "Gasdruck fehlt - an X22/13 liegt keine Spannung an, ";
					}
					if (stoerung.contains("221")) {
						errorMessage = errorMessage + "Rückmeldungen - interner Fehler, ";
					}
					if (stoerung.contains("222")) {
						errorMessage = errorMessage + "Rückm Generator - Signal an X21/7, ";
					}
					if (stoerung.contains("223")) {
						errorMessage = errorMessage + "Rückm Sanftanlauf - Signal an X21/5, ";
					}
					if (stoerung.contains("224")) {
						errorMessage = errorMessage + "Rückm Magnetv. - Sicherung F21 prüfen, ";
					}
					if (stoerung.contains("225")) {
						errorMessage = errorMessage + "Rückm Anlasser - Signal an X21/8, ";
					}
					if (stoerung.contains("226")) {
						errorMessage = errorMessage + "Rückm Hubmagnet - Sicherung F18 prüfen, ";
					}
					if (stoerung.contains("250")) {
						errorMessage = errorMessage + "Vorlauffühler Heizkreis 1 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("251")) {
						errorMessage = errorMessage + "Vorlauffühler Heizkreis 2 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("252")) {
						errorMessage = errorMessage + "Warmwasserfühler - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("253")) {
						errorMessage = errorMessage + "Fühler 3 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("254")) {
						errorMessage = errorMessage + "Fühler 4 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("255")) {
						errorMessage = errorMessage + "Raumtemp. Fühler 1 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("256")) {
						errorMessage = errorMessage + "Raumtemp. Fühler 2 - Unterbrechung/Kurzschluss, ";
					}
					if (stoerung.contains("270")) {
						errorMessage = errorMessage + "Leitregler mehrfach - nur bei MM und LR: Leitregler mehrfach eingestellt, ";
					}
					if (stoerung.contains("271")) {
						errorMessage = errorMessage + "Modul-Nr. mehrfach - nur bei MM und LR: Regler-Adresse mehrfach eingestellt, ";
					}
					if (stoerung.contains("350")) {
						errorMessage = errorMessage + "EEP_DatenRP not OK - interner Fehler, ";
					}
					if (stoerung.contains("354")) {
						errorMessage = errorMessage + "User Stack > Soll - interner Fehler, ";
					}
					if (stoerung.contains("355")) {
						errorMessage = errorMessage + "Int. Stack > Soll - interner Fehler, ";
					}
					// In case the code is not one in the list.
					if (errorMessage.charAt(errorMessage.length() - 2) == ':') {
						errorMessage = errorMessage.substring(0, errorMessage.length() - 2) + " (unknown error code), ";
					}
				}
			}


			String warningMessage = "";
			if (serverMessage.contains("Hka_Bd.bWarnung=")) {
				String warningCode = this.readEntryAfterString(serverMessage, "Hka_Bd.bWarnung=");
				if (warningCode.length() == 0) {
					// warningCode should contain "0" for no warning. If it is empty, something went wrong.
					warningMessage = "Failed to transmit warning code, ";
				} else {
					if (warningCode.equals("0") == false) {
						warningMessage = "Warning code: " + warningCode + ", ";

						// Would put more code here to parse warning code, but the warning codes are not yet in the manual.

					}
				}
			} else {
				warningMessage = "Failed to transmit warning code, ";
			}
			

			if (serverMessage.contains("Hka_Mw1.sWirkleistung=")) {
				String wirkleistung = "";	// To make sure there is no null exception when parsing.
				wirkleistung = wirkleistung + this.readEntryAfterString(serverMessage, "Hka_Mw1.sWirkleistung=");
				try {
					this._setEffectiveElectricPower(Double.parseDouble(wirkleistung.trim()));
				} catch (NumberFormatException e) {		// This catches wirkleistung possibly being empty.
					errorMessage = errorMessage + "Can't parse effective electrical power (Wirkleistung): " + e.getMessage() + ", ";
					this._setEffectiveElectricPower(NO_DATA);
				}
			} else {
				errorMessage = errorMessage + "Failed to transmit effective electrical power (Wirkleistung), ";
				this._setEffectiveElectricPower(NO_DATA);
			}


			if (serverMessage.contains("Hka_Mw1.Temp.sbVorlauf=")) {
				String flowTemp = "";   // To make sure there is no null exception when parsing.
				flowTemp = flowTemp + this.readEntryAfterString(serverMessage, "Hka_Mw1.Temp.sbVorlauf=");
				try {
					this._setFlowTemperature(Integer.parseInt(flowTemp.trim())*10);	// Convert to dezidegree.
				} catch (NumberFormatException e) {		// This catches flowTemp possibly being empty.
					errorMessage = errorMessage + "Can't parse foreward temperature (Vorlauf): " + e.getMessage() + ", ";
					this._setFlowTemperature(NO_DATA);
				}
			} else {
				errorMessage = errorMessage + "Failed to transmit foreward temperature (Vorlauf), ";
				this._setFlowTemperature(NO_DATA);
			}


			if (serverMessage.contains("Hka_Mw1.Temp.sbRuecklauf=")) {
				String returnTemp = "";		// To make sure there is no null exception when parsing.
				returnTemp = returnTemp + this.readEntryAfterString(serverMessage, "Hka_Mw1.Temp.sbRuecklauf=");
				try {
					this._setReturnTemperature(Integer.parseInt(returnTemp.trim()) * 10);	// Convert to dezidegree.
				} catch (NumberFormatException e) {		// This catches returnTemp possibly being empty.
					errorMessage = errorMessage + "Can't parse return temperature (Ruecklauf): " + e.getMessage() + ", ";
					this._setReturnTemperature(NO_DATA);
				}
			} else {
				errorMessage = errorMessage + "Failed to transmit return temperature (Ruecklauf), ";
				this._setReturnTemperature(NO_DATA);
			}
			

			boolean runClearance = false;
			boolean stateUndefined = false;
			if (serverMessage.contains("Hka_Bd.UHka_Frei.usFreigabe=")) {
				String freigabe = "";
				freigabe = freigabe + this.readEntryAfterString(serverMessage, "Hka_Bd.UHka_Frei.usFreigabe=");
				if (freigabe.equals("65535")) {	// This is the int equivalent of hex FFFF. Manual discusses freigabe code in hex.
					runClearance = true;
					this._setNotReadyMessage("Code FFFF: Dachs is ready to run.");
				} else {
					try {
						int tempInt = Integer.parseInt(freigabe.trim());
						String inHex = Integer.toHexString(tempInt).toUpperCase();
						// The code is a 4 digit hex. Digit 1, 3 and 4 can be from 0 to F, while digit 2 is C to F. The 0 to F
						// digits code for four options. Their 0 or 1 states are concatenated to form a 4 digit binary, that
						// is then represented by a single digit hex. Digit 2 of the hex code represents just two options,
						// that's why it is C to F.
						// The manual discusses the code in hex representation, while the server transmits the hex code as a
						// base 10 integer. When transforming back to a four digit hex, account for the fact that a 0 in the
						// first hex digit would have been lost in translation.
						// Make sure inHex has 4 digits just in case, to prevent string out of bounds error.
						for (int i = inHex.length(); i < 4; i++) {
							inHex = "0" + inHex;
						}
						String returnMessage = "Code " + inHex + ": Clearance not given by";
						String firstDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(0)),16));
						//Make sure it has 4 digits
						for (int i = firstDigitInBinary.length(); i < 4; i++) {
							firstDigitInBinary = "0" + firstDigitInBinary;
						}
						if (firstDigitInBinary.charAt(0) == '0') {
							returnMessage = returnMessage + " Inbetriebname OK,";
						}
						if (firstDigitInBinary.charAt(1) == '0') {
							returnMessage = returnMessage + " Taste OnOff,";
						}
						if (firstDigitInBinary.charAt(2) == '0') {
							returnMessage = returnMessage + " Interne Freigabe HKA,";
						}
						if (firstDigitInBinary.charAt(3) == '0') {
							returnMessage = returnMessage + " Eingang Modulfreigabe,";
						}
						if (inHex.charAt(1) == 'C') {
							returnMessage = returnMessage + " Startverzögerung, Netz OK,";
						}
						if (inHex.charAt(1) == 'D') {
							returnMessage = returnMessage + " Startverzögerung,";
						}
						if (inHex.charAt(1) == 'E') {
							returnMessage = returnMessage + " Netz OK,";
						}
						String thirdDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(2)),16));
						for (int i = thirdDigitInBinary.length(); i < 4; i++) {
							thirdDigitInBinary = "0" + thirdDigitInBinary;
						}
						if (thirdDigitInBinary.charAt(0) == '0') {
							returnMessage = returnMessage + " Rueckmeldung SiKette,";
						}
						if (thirdDigitInBinary.charAt(1) == '0') {
							returnMessage = returnMessage + " Max Ruecklauftemperatur,";
						}
						if (thirdDigitInBinary.charAt(2) == '0') {
							returnMessage = returnMessage + " Temperatur,";
						}
						if (thirdDigitInBinary.charAt(3) == '0') {
							returnMessage = returnMessage + " Stoerung,";
						}
						String fourthDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(3)),16));
						for (int i = fourthDigitInBinary.length(); i < 4; i++) {
							fourthDigitInBinary = "0" + fourthDigitInBinary;
						}
						if (fourthDigitInBinary.charAt(0) == '0') {
							returnMessage = returnMessage + " Lauf24h,";
						}
						if (fourthDigitInBinary.charAt(1) == '0') {
							returnMessage = returnMessage + " Stillstandszeit,";
						}
						if (fourthDigitInBinary.charAt(2) == '0') {
							returnMessage = returnMessage + " Abschaltzeit,";
						}
						if (fourthDigitInBinary.charAt(3) == '0') {
							returnMessage = returnMessage + " Anforderung HKA.";
						}
						if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
							returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
						}
						this._setNotReadyMessage(returnMessage);
					} catch (NumberFormatException e) {
						errorMessage = errorMessage + "Can't parse Chp ready indicator (Freigabe): " + e.getMessage() + ", ";
						this.logError(this.log, "Error, can't parse NotReadyCode: " + e.getMessage());
						this._setNotReadyMessage("Code " + freigabe + ": Error deciphering code.");
					}
				}
			} else {
				errorMessage = errorMessage + "Failed to transmit Chp ready indicator (Freigabe), ";
				this._setNotReadyMessage("Failed to transmit Chp ready indicator (Freigabe).");
				stateUndefined = true;
			}


			boolean stateStartingUp = false;
			if (serverMessage.contains("Hka_Bd.UHka_Anf.usAnforderung=")) {
				String laufAnforderung = "";
				laufAnforderung = laufAnforderung + this.readEntryAfterString(serverMessage, "Hka_Bd.UHka_Anf.usAnforderung=");
				if (laufAnforderung.equals("0")) {
					this._setRunRequestMessage("Code 0: Nothing is requesting the Dachs to run right now.");
				} else {
					stateStartingUp = true;
					try {
						int tempInt = Integer.parseInt(laufAnforderung.trim());
						String returnMessage = "Code " + Integer.toHexString(tempInt).toUpperCase() + ": Running requested by";
						// This code represents 8 options. Their 0 or 1 states are concatenated to a 8 digit binary that the
						// server transmits as a base 10 integer. The Manual discusses the code as a hex number.
						String inBinary = Integer.toBinaryString(tempInt);
						// Make sure the string has 8 digits to avoid string out of bounds error.
						for (int i = inBinary.length(); i < 8; i++) {
							inBinary = "0" + inBinary;
						}
						if (inBinary.charAt(0) == '1') {
							returnMessage = returnMessage + " Mehrmodul,";
						}
						if (inBinary.charAt(1) == '1') {
							returnMessage = returnMessage + " Strom,";
						}
						if (inBinary.charAt(2) == '1') {
							returnMessage = returnMessage + " Manuell,";
						}
						if (inBinary.charAt(3) == '1') {
							returnMessage = returnMessage + " Extern,";
						}
						if (inBinary.charAt(4) == '1') {
							returnMessage = returnMessage + " Hoher Sollwert,";
						}
						if (inBinary.charAt(5) == '1') {
							returnMessage = returnMessage + " BW Bereitung,";
						}
						if (inBinary.charAt(6) == '1') {
							returnMessage = returnMessage + " Waerme,";
						}
						if (inBinary.charAt(7) == '1') {
							returnMessage = returnMessage + " Mindestlaufzeit.";
						}
						if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
							returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
						}
						this._setRunRequestMessage(returnMessage);
					} catch (NumberFormatException e) {
						// This is not really needed for chp operation, so it is a warning and not an error.
						warningMessage = warningMessage + "Can't parse run request code (Lauf Anforderung): " + e.getMessage() + ", ";
						this._setRunRequestMessage("Code " + laufAnforderung + ": Error deciphering code.");
					}
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit run request code (Lauf Anforderung), ";
				this._setRunRequestMessage("Failed to transmit run request code (Lauf Anforderung).");
			}


			if (serverMessage.contains("Hka_Bd.Anforderung.ModulAnzahl=")) {
				String modulzahl = "";
				modulzahl = modulzahl + this.readEntryAfterString(serverMessage, "Hka_Bd.Anforderung.ModulAnzahl=");
				try {
					this._setNumberOfModules(Integer.parseInt(modulzahl.trim()));
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse requested modules (Anforderung Modul Anzahl): " + e.getMessage() + ", ";
					this._setNumberOfModules(-1);	// -1 to indicate an error.
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit requested modules (Anforderung Modul Anzahl), ";
				this._setNumberOfModules(-1);
			}


			if (serverMessage.contains("Hka_Bd.UStromF_Frei.bFreigabe=")) {
				String freigabeStromfuehrung = "";
				freigabeStromfuehrung = freigabeStromfuehrung + this.readEntryAfterString(serverMessage, "Hka_Bd.UStromF_Frei.bFreigabe=");
				if (freigabeStromfuehrung.equals("255")) {
					this._setElectricModeClearanceMessage("Code FF: Dachs is in electric power guided mode.");
				} else {
					try {
						int tempInt = Integer.parseInt(freigabeStromfuehrung.trim());
						String inHex = Integer.toHexString(tempInt).toUpperCase();
						String returnMessage = "Code " + inHex + ": No electric power guided mode because the following is missing -";
						// This code represents 4 options. Their 0 or 1 states are concatenated to a 4 digit binary that is
						// represented by a single digit hex. To this hex, F0 is added to form a two digit hex where only the
						// second digit has meaning. The server then transmits this number as a base 10 integer. The Manual
						// discusses the code as a hex number.
						String secondDigitInBinary = "1111";	// Fallbackvalue in case inHex.length() != 2
						if (inHex.length() == 2) {
							secondDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(1)),16));
						} else {
							returnMessage = "Code " + inHex + ": Error deciphering code.";
						}
						for (int i = secondDigitInBinary.length(); i < 4; i++) {
							secondDigitInBinary = "0" + secondDigitInBinary;
						}
						if (secondDigitInBinary.charAt(0) == '0') {
							returnMessage = returnMessage + " SoWi,";
						}
						if (secondDigitInBinary.charAt(1) == '0') {
							returnMessage = returnMessage + " HtNt,";
						}
						if (secondDigitInBinary.charAt(2) == '0') {
							returnMessage = returnMessage + " MaxStrom,";
						}
						if (secondDigitInBinary.charAt(3) == '0') {
							returnMessage = returnMessage + " Anforderung Strom.";
						}
						if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
							returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
						}
						this._setElectricModeClearanceMessage(returnMessage);
					} catch (NumberFormatException e) {
						warningMessage = "Can't parse electricity guided mode clearance code (Freigabe Stromfuehrung): " + e.getMessage() + ", ";
						this._setElectricModeClearanceMessage("Code " + freigabeStromfuehrung + ": Error deciphering code.");
					}
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit electricity guided mode clearance code (Freigabe Stromfuehrung), ";
				this._setElectricModeClearanceMessage("Failed to transmit electricity guided mode clearance code (Freigabe Stromfuehrung).");
			}


			if (serverMessage.contains("Hka_Bd.UHka_Anf.Anforderung.fStrom=")) {
				String anforderungStrom = this.readEntryAfterString(serverMessage, "Hka_Bd.UHka_Anf.Anforderung.fStrom=");
				if (anforderungStrom.equals("true")) {
					this._setElectricModeRunFlag(true);
				} else {
					this._setElectricModeRunFlag(false);
				}
				if (anforderungStrom.length() == 0) {
					warningMessage = warningMessage + "Failed to transmit electricity guided mode run flag (Anforderung Strom), ";
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit electricity guided mode run flag (Anforderung Strom), ";
				this._setElectricModeRunFlag(false);
			}


			if (serverMessage.contains("Hka_Bd.Anforderung.UStromF_Anf.bFlagSF=")) {
				String stromAnforderungSettings = "";
				stromAnforderungSettings = stromAnforderungSettings + this.readEntryAfterString(serverMessage, "Hka_Bd.Anforderung.UStromF_Anf.bFlagSF=");
				if (stromAnforderungSettings.equals("0")) {
					this._setElectricModeSettingsMessage("Code 0: No component is requesting electric power guided mode.");
				} else {
					try {
						int tempInt = Integer.parseInt(stromAnforderungSettings.trim());
						String returnMessage = "Code " + Integer.toHexString(tempInt).toUpperCase() + ": Electric power guided mode requested by";
						// This code represents 5 options. Their 0 or 1 states are concatenated to a 5 digit binary that the
						// server transmits as a base 10 integer. The Manual discusses the code as a hex number.
						String inBinary = Integer.toBinaryString(tempInt);
						// Make sure the string has 5 digits to avoid string out of bounds error.
						for (int i = inBinary.length(); i < 5; i++) {
							inBinary = "0" + inBinary;
						}
						if (inBinary.charAt(0) == '1') {
							returnMessage = returnMessage + " Energie Zaehler 2,";
						}
						if (inBinary.charAt(1) == '1') {
							returnMessage = returnMessage + " Energie Zaehler 1,";
						}
						if (inBinary.charAt(2) == '1') {
							returnMessage = returnMessage + " DigExtern,";
						}
						if (inBinary.charAt(3) == '1') {
							returnMessage = returnMessage + " Uhr intern,";
						}
						if (inBinary.charAt(4) == '1') {
							returnMessage = returnMessage + " Can extern.";
						}
						if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
							returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
						}
						this._setElectricModeSettingsMessage(returnMessage);
					} catch (NumberFormatException e) {
						warningMessage = warningMessage + "Can't parse electricity guided mode requests (Anforderungen Stromfuehrung): " + e.getMessage() + ", ";
						this._setElectricModeSettingsMessage("Code " + stromAnforderungSettings + ": Error deciphering code.");
					}
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit electricity guided mode requests (Anforderungen Stromfuehrung), ";
				this._setElectricModeSettingsMessage("Failed to transmit electricity guided mode requests (Anforderungen Stromfuehrung).");
			}


			if (serverMessage.contains("Hka_Bd.ulArbeitElektr=")) {
				String arbeitElectr = "";
				arbeitElectr = arbeitElectr + this.readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitElektr=");
				try {
					this._setElectricalWork(Double.parseDouble(arbeitElectr));
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse generated electrical work (Erzeugte elektrische Arbeit): " + e.getMessage() + ", ";
					this._setElectricalWork(-1.0);	// -1 to indicate an error.
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit generated electrical work (Erzeugte elektrische Arbeit), ";
				this._setElectricalWork(-1.0);
			}


			if (serverMessage.contains("Hka_Bd.ulArbeitThermHka=")) {
				String arbeitTherm = "";
				arbeitTherm = arbeitTherm + this.readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitThermHka=");
				try {
					this._setThermalWork(Double.parseDouble(arbeitTherm));
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse generated thermal work (Erzeugte thermische Arbeit): " + e.getMessage() + ", ";
					this._setThermalWork(-1.0);	// -1 to indicate an error.
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit generated thermal work (Erzeugte thermische Arbeit), ";
				this._setThermalWork(-1.0);
			}


			if (serverMessage.contains("Hka_Bd.ulArbeitThermKon=")) {
				String arbeitThermKon = "";
				arbeitThermKon = arbeitThermKon + this.readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitThermKon=");
				try {
					this._setThermalWorkCond(Double.parseDouble(arbeitThermKon));
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse generated thermal work condenser (Erzeugte thermische Arbeit Kondenser): " + e.getMessage() + ", ";
					this._setThermalWorkCond(-1.0);	// -1 to indicate an error.
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit generated thermal work condenser (Erzeugte thermische Arbeit Kondenser), ";
				this._setThermalWorkCond(-1.0);
			}


			if (serverMessage.contains("Hka_Bd.ulBetriebssekunden=")) {
				String runtimeSinceRestart = "";
				runtimeSinceRestart = runtimeSinceRestart + this.readEntryAfterString(serverMessage, "Hka_Bd.ulBetriebssekunden=");
				try {
					this._setRuntimeSinceRestart(Double.parseDouble(runtimeSinceRestart));
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse runtime since restart (Betriebsstunden): " + e.getMessage() + ", ";
					this._setRuntimeSinceRestart(-1.0);	// -1 to indicate an error.
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit runtime since restart (Betriebsstunden), ";
				this._setRuntimeSinceRestart(-1.0);
			}


			int rpmReadout = 0;
			if (serverMessage.contains("Hka_Mw1.usDrehzahl=")) {
				String drehzahl = "";
				drehzahl = drehzahl + this.readEntryAfterString(serverMessage, "Hka_Mw1.usDrehzahl=");
	            try {
					rpmReadout = Integer.parseInt(drehzahl.trim());
	                this._setRpm(rpmReadout);
	            } catch (NumberFormatException e) {
	            	errorMessage = errorMessage + "Can't parse engine rpm (Motordrehzahl): " + e.getMessage() + ", ";
					this._setRpm(-1);	// -1 to indicate an error.
	            }
			} else {
				errorMessage = errorMessage + "Failed to transmit engine rpm (Motordrehzahl), ";
				this._setRpm(-1);
			}


			if (serverMessage.contains("Hka_Bd.ulAnzahlStarts=")) {
				String engineStarts = "";
				engineStarts = engineStarts + this.readEntryAfterString(serverMessage, "Hka_Bd.ulAnzahlStarts=");
				try {
					this._setEngineStarts(Integer.parseInt(engineStarts.trim()));
				} catch (NumberFormatException e) {
					warningMessage = warningMessage + "Can't parse engine starts (Anzahl Starts): " + e.getMessage() + ", ";
					this._setEngineStarts(-1);	// -1 to indicate an error.
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit engine starts (Anzahl Starts), ";
				this._setEngineStarts(-1);
			}


			String wartungFlag = this.readEntryAfterString(serverMessage, "Wartung_Cache.fStehtAn=");
			if (serverMessage.contains("Wartung_Cache.fStehtAn=") && wartungFlag.length() > 0) {
				if (wartungFlag.equals("true")) {
					this._setMaintenanceFlag(true);
					warningMessage = warningMessage + "Maintenance needed (Wartung steht an), ";
				} else {
					this._setMaintenanceFlag(false);
				}
			} else {
				warningMessage = warningMessage + "Failed to transmit maintenance flag (Wartung steht an), ";
				this._setMaintenanceFlag(false);
			}
			
			
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

			if (stateUndefined) {
				this.chpEngineRunning = false;
				this._setHeaterState(HeaterState.UNDEFINED.getValue());
			} else if (rpmReadout > NORMAL_OPERATION_RPM_THRESHOLD) {
				this.chpEngineRunning = true;
				this._setHeaterState(HeaterState.HEATING.getValue());
			} else if (stateStartingUp) {
				this.chpEngineRunning = true;
				this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
			} else if (runClearance) {
				this.chpEngineRunning = false;
				this._setHeaterState(HeaterState.STANDBY.getValue());
			} else {
				this.chpEngineRunning = false;
				this._setHeaterState(HeaterState.BLOCKED.getValue());
			}

		} else {
			this.readyForCommands = false;
			this._setErrorMessage("Couldn't read data from GLT interface.");
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
	}


	// Separate method for these as they don't change and only need to be requested once.
    protected void getSerialAndPartsNumber() {
        String temp = this.getKeyDachs("k=Hka_Bd_Stat.uchSeriennummer&k=Hka_Bd_Stat.uchTeilenummer");
        if (temp.contains("Hka_Bd_Stat.uchSeriennummer=")) {
			this._setSerialNumber(this.readEntryAfterString(temp, "Hka_Bd_Stat.uchSeriennummer="));
			this._setPartsNumber(this.readEntryAfterString(temp, "Hka_Bd_Stat.uchTeilenummer="));
        } else {
        	// Writing to log here is ok as this executes only once.
            this.logError(this.log, "Error: Couldn't read data from GLT interface.");
        }
    }

    
    // Extract a value from the server return message. "message" is the return message from the server. "marker" is the 
    // value after which you want to read. Reads until the end of the line.
    protected String readEntryAfterString(String message, String marker) {
        return message.substring(message.indexOf(marker) + marker.length(), message.indexOf("/n", message.indexOf(marker)));
	}
    
    
    protected void printDataToLog() {
    	if (this.basicInfo) {
    		this.logInfo(this.log, "---- CHP Senertec Dachs ----");
    		this.logInfo(this.log, "Engine rpm: " + getRpm());
    		this.logInfo(this.log, "Flow temp: " + getFlowTemperature());
    		this.logInfo(this.log, "Return temp: " + getReturnTemperature());
    		this.logInfo(this.log, "Effective electric power: " + getEffectiveElectricPower());
    		this.logInfo(this.log, "Heater state: " + getHeaterState());
    		this.logInfo(this.log, "Error message: " + getErrorMessage());
    		this.logInfo(this.log, "Warning message: " + getWarningMessage());
    	}
    	if (this.debug) {
    		this.logInfo(this.log, "Serial number: " + getSerialNumber());
    		this.logInfo(this.log, "Parts number: " + getPartsNumber());
    		this.logInfo(this.log, "Engine starts: " + getEngineStarts());
    		this.logInfo(this.log, "Runtime: " + getRuntimeSinceRestart());
    		this.logInfo(this.log, "Run request message: " + getRunRequestMessage());
    		this.logInfo(this.log, "Not ready message: " + getNotReadyMessage());
    		this.logInfo(this.log, "Number of modules requested: " + getNumberOfModules());
    		this.logInfo(this.log, "Electricity guided operation clearance: " + getElectricModeClearanceMessage());
    		this.logInfo(this.log, "Electricity guided operation settings: " + getElectricModeSettingsMessage());
    		this.logInfo(this.log, "Electricity guided operation run flag: " + getElectricModeRunFlag());
    		this.logInfo(this.log, "Electical work done: " + getElectricalWork());
    		this.logInfo(this.log, "Thermal work done: " + getThermalWork());
    		this.logInfo(this.log, "Thermal work condenser done: " + getThermalWorkCond());
    	}
    	
    }

	/**
	 * Send read request to server.
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

	protected void activateDachs() {
		String returnMessage = this.setKeysDachs("Stromf_Ew.Anforderung_GLT.bAktiv=1");
		if (this.debug) {
			this.logInfo(this.log, "Sending \"run request\" signal to Dachs Chp. Return message: " + returnMessage);
		}
	}

	protected void deactivateDachs() {
		String returnMessage = this.setKeysDachs("Stromf_Ew.Anforderung_GLT.bAktiv=0");
		if (this.debug) {
			this.logInfo(this.log, "Sending \"no need to run\" signal to Dachs Chp. Return message: " + returnMessage);
		}
	}

}
