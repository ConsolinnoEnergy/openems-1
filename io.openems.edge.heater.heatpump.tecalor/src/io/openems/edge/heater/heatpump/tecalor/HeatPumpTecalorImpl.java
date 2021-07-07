package io.openems.edge.heater.heatpump.tecalor;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.api.SmartGridState;
import io.openems.edge.heater.heatpump.tecalor.api.HeatpumpTecalorChannel;
import io.openems.edge.heater.api.HeatpumpSmartGrid;

import java.util.Arrays;
import java.util.Optional;

import io.openems.edge.heater.heatpump.tecalor.api.OperatingMode;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module reads the most important variables available via Modbus from a Tecalor heat pump and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpAlphaInnotecChannel, HeatpumpSmartGrid and Heater.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatPumpTecalor",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class HeatPumpTecalorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeatpumpTecalorChannel {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(HeatPumpTecalorImpl.class);
	private boolean debug;
	private boolean componentEnabled;
	private boolean readOnly;
	private boolean sgReadyActive;

	private boolean turnOnHeatpump;
	private String defaultModeOfOperation;
	private boolean useEnableSignal;
	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "HEAT_PUMP_TECALOR_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "HEAT_PUMP_TECALOR_EXCEPTIONAL_STATE_IDENTIFIER";

	// This is essential for Modbus to work, but the compiler does not warn you when it is missing!
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpTecalorImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpTecalorChannel.ChannelId.values(),
				HeatpumpSmartGrid.ChannelId.values(),
				Heater.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.debug = config.debug();
		this.componentEnabled = config.enabled();
		this.readOnly = config.readOnly();
		this.sgReadyActive = config.sgReady();
		TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
		this.useEnableSignal = config.useEnableSignalChannel();
		this.defaultModeOfOperation = config.defaultModeOfOperation();
		if (this.useEnableSignal) {
			String timerIdEnableSignal;
			if (config.enableSignalTimerIsCyclesNotSeconds()) {
				timerIdEnableSignal = "TimerByCycles";
			} else {
				timerIdEnableSignal = "TimerByTime";
			}
			timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerIdEnableSignal, config.waitTimeEnableSignal());
			this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
		}
		this.useExceptionalState = config.useExceptionalState();
		if (this.useExceptionalState) {
			String timerIdEnableSignal;
			if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
				timerIdEnableSignal = "TimerByCycles";
			} else {
				timerIdEnableSignal = "TimerByTime";
			}
			timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerIdEnableSignal, config.waitTimeExceptionalState());
			this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
		}

	}


	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		ModbusProtocol protocol = new ModbusProtocol(this,
				new FC4ReadInputRegistersTask(506, Priority.LOW,
						/* Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
						   to negative. That means if the value you read is positive and <32767, there is no difference
						   between signed and unsigned.
						   The pump sends 0x8000H (= signed -32768) when a value is not available. The
						   ElementToChannelConverter function is used to replace that value with "null", as this is
						   better for the visualization. */
						m(HeatpumpTecalorChannel.ChannelId.IR507_AUSSENTEMP, new SignedWordElement(506),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR508_ISTTEMPHK1, new SignedWordElement(507),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR509_SOLLTEMPHK1, new SignedWordElement(508),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR510_SOLLTEMPHK1, new SignedWordElement(509),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR511_ISTTEMPHK2, new SignedWordElement(510),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR512_SOLLTEMPHK2, new SignedWordElement(511),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR513_VORLAUFISTTEMPWP, new SignedWordElement(512),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR514_VORLAUFISTTEMPNHZ, new SignedWordElement(513),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(514),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(515),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR517_FESTWERTSOLLTEMP, new SignedWordElement(516),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR518_PUFFERISTTEMP, new SignedWordElement(517),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR519_PUFFERSOLLTEMP, new SignedWordElement(518),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR520_HEIZUNGSDRUCK, new SignedWordElement(519),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR521_VOLUMENSTROM, new SignedWordElement(520),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR522_WWISTTEMP, new SignedWordElement(521),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR523_WWSOLLTEMP, new SignedWordElement(522),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR524_GEBLAESEISTTEMP, new SignedWordElement(523),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR525_GEBLAESESOLLTEMP, new SignedWordElement(524),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR526_FLAECHEISTTEMP, new SignedWordElement(525),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalorChannel.ChannelId.IR527_FLAECHESOLLTEMP, new SignedWordElement(526),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H)
				),
				new FC4ReadInputRegistersTask(2500, Priority.HIGH,
						m(HeatpumpTecalorChannel.ChannelId.IR2501_STATUSBITS, new UnsignedWordElement(2500),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR2502_EVUFREIGABE, new UnsignedWordElement(2501),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(2502),
						m(HeatpumpTecalorChannel.ChannelId.IR2504_ERRORSTATUS, new UnsignedWordElement(2503),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR2505_BUSSTATUS, new UnsignedWordElement(2504),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR2506_DEFROST, new UnsignedWordElement(2505),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR2507_ERROR_CODE, new UnsignedWordElement(2506),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(3500, Priority.LOW,
						m(HeatpumpTecalorChannel.ChannelId.IR3501_HEATPRODUCED_VDHEIZENTAG, new UnsignedWordElement(3500),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3502_HEATPRODUCED_VDHEIZENSUMKWH, new UnsignedWordElement(3501),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3503_HEATPRODUCED_VDHEIZENSUMMWH, new UnsignedWordElement(3502),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3504_HEATPRODUCED_VDWWTAG, new UnsignedWordElement(3503),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3505_HEATPRODUCED_VDWWSUMKWH, new UnsignedWordElement(3504),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3506_HEATPRODUCED_VDWWSUMMWH, new UnsignedWordElement(3505),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3507_HEATPRODUCED_NZHHEIZENSUMKWH, new UnsignedWordElement(3506),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3508_HEATPRODUCED_NZHHEIZENSUMMWH, new UnsignedWordElement(3507),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3509_HEATPRODUCED_NZHWWSUMKWH, new UnsignedWordElement(3508),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3510_HEATPRODUCED_NZHWWSUMMWH, new UnsignedWordElement(3509),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3511_CONSUMEDPOWER_VDHEIZENTAG, new UnsignedWordElement(3510),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3512_CONSUMEDPOWER_VDHEIZENSUMKWH, new UnsignedWordElement(3511),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3513_CONSUMEDPOWER_VDHEIZENSUMMWH, new UnsignedWordElement(3512),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3514_CONSUMEDPOWER_VDWWTAG, new UnsignedWordElement(3513),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3515_CONSUMEDPOWER_VDWWSUMKWH, new UnsignedWordElement(3514),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR3516_CONSUMEDPOWER_VDWWSUMMWH, new UnsignedWordElement(3515),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(5000, Priority.HIGH,
						m(HeatpumpTecalorChannel.ChannelId.IR5001_SGREADY_OPERATINGMODE, new UnsignedWordElement(5000),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.IR5002_REGLERKENNUNG, new UnsignedWordElement(5001),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(1500, Priority.HIGH,
						m(HeatpumpTecalorChannel.ChannelId.HR1501_BERTIEBSART, new UnsignedWordElement(1500),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1502_KOMFORTTEMPHK1, new SignedWordElement(1501),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1503_ECOTEMPHK1, new SignedWordElement(1502),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1504_SLOPEHK1, new SignedWordElement(1503),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1505_KOMFORTTEMPHK2, new SignedWordElement(1504),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1506_ECOTEMPHK2, new SignedWordElement(1505),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1507_SLOPEHK2, new SignedWordElement(1506),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1508_FESTWERTBETRIEB, new SignedWordElement(1507),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1509_BIVALENZTEMPERATURHZG, new SignedWordElement(1508),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1510_KOMFORTTEMPWW, new SignedWordElement(1509),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1511_ECOTEMPWW, new SignedWordElement(1510),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1512_WARMWASSERSTUFEN, new SignedWordElement(1511),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1513_BIVALENZTEMPERATURWW, new SignedWordElement(1512),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG, new SignedWordElement(1513),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG, new SignedWordElement(1514),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG, new SignedWordElement(1515),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG, new SignedWordElement(1516),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG, new SignedWordElement(1517),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG, new SignedWordElement(1518),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(4000, Priority.HIGH,
						m(HeatpumpTecalorChannel.ChannelId.HR4001_SGREADY_ONOFF, new UnsignedWordElement(4000),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR4002_SGREADY_INPUT1, new UnsignedWordElement(4001),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalorChannel.ChannelId.HR4003_SGREADY_INPUT2, new UnsignedWordElement(4002),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					/* Modbus write tasks take the "setNextWriteValue" value of a channel and send them to the device.
				       Modbus read tasks put values in the "setNextValue" field, which get automatically transferred to the
				       "value" field of the channel. By default, the "setNextWriteValue" field is NOT copied to the
				       "setNextValue" and "value" field. In essence, this makes "setNextWriteValue" and "setNextValue"/"value"
				       two separate channels.
				       That means: Modbus read tasks will not overwrite any "setNextWriteValue" values. You do not have to
				       watch the order in which you call read and write tasks.
				       Also: if you do not add a Modbus read task for a write channel, any "setNextWriteValue" values will
				       not be transferred to the "value" field of the channel, unless you add code that does that. */
					new FC16WriteRegistersTask(1500,
							m(HeatpumpTecalorChannel.ChannelId.HR1501_BERTIEBSART, new UnsignedWordElement(1500),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1502_KOMFORTTEMPHK1, new SignedWordElement(1501),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1503_ECOTEMPHK1, new SignedWordElement(1502),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1504_SLOPEHK1, new SignedWordElement(1503),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1505_KOMFORTTEMPHK2, new SignedWordElement(1504),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1506_ECOTEMPHK2, new SignedWordElement(1505),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1507_SLOPEHK2, new SignedWordElement(1506),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1508_FESTWERTBETRIEB, new SignedWordElement(1507),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1509_BIVALENZTEMPERATURHZG, new SignedWordElement(1508),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1510_KOMFORTTEMPWW, new SignedWordElement(1509),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1511_ECOTEMPWW, new SignedWordElement(1510),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1512_WARMWASSERSTUFEN, new UnsignedWordElement(1511),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1513_BIVALENZTEMPERATURWW, new SignedWordElement(1512),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG, new SignedWordElement(1513),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG, new SignedWordElement(1514),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG, new SignedWordElement(1515),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG, new SignedWordElement(1516),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG, new SignedWordElement(1517),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG, new SignedWordElement(1518),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(4000,
							m(HeatpumpTecalorChannel.ChannelId.HR4001_SGREADY_ONOFF, new UnsignedWordElement(4000),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR4002_SGREADY_INPUT1, new UnsignedWordElement(4001),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalorChannel.ChannelId.HR4003_SGREADY_INPUT2, new UnsignedWordElement(4002),
									ElementToChannelConverter.DIRECT_1_TO_1)
					)
			);
		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (this.componentEnabled && EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE.equals(event.getTopic())) {
			//channeltest();	// Just for testing
			this.channelmapping();
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
	protected void channelmapping() {

		// Map error to Heater interface ErrorMessage.
		boolean isError = false;
		if (this.getErrorStatus().isDefined()) {
			isError = this.getErrorStatus().get();
		}
		if (this.getErrorCode().isDefined()) {
			int errorCode = this.getErrorCode().get();
			if (errorCode > 0) {
				_setErrorMessage("Error Code: " + errorCode);
			}
		} else {
			if (isError) {
				_setErrorMessage("An unknown error occurred.");
			} else {
				_setErrorMessage("No error");
			}
		}

		// Map status to Heater interface HeaterState
		int statusBits = 0;
		boolean signalReceived = false;
		boolean isRunning = false;
		if (this.getStatusBits().isDefined()) {
			signalReceived = true;
			statusBits = this.getStatusBits().get();
			boolean aufheizprogramm = (statusBits & 0b0100) == 0b0100;
			boolean nhzAktiv = (statusBits & 0b01000) == 0b01000;
			boolean heizbetrieb = (statusBits & 0b010000) == 0b010000;
			boolean warmwasserbetrieb = (statusBits & 0b0100000) == 0b0100000;
			boolean verdichterAktiv = (statusBits & 0b01000000) == 0b01000000;
			boolean kuehlbetrieb = (statusBits & 0b0100000000) == 0b0100000000;
			isRunning = aufheizprogramm || nhzAktiv || heizbetrieb || warmwasserbetrieb || verdichterAktiv || kuehlbetrieb;
		}
		boolean notBlocked = true;
		if (this.getElSupBlockRelease().isDefined()) {
			notBlocked = this.getElSupBlockRelease().get();
		}
		if (isRunning) {
			_setHeaterState(HeaterState.HEATING.getValue());
		} else if (notBlocked && signalReceived) {
			_setHeaterState(HeaterState.STANDBY.getValue());
		} else if (notBlocked == false || isError) {
		_setHeaterState(HeaterState.BLOCKED.getValue());
		} else {
			// You land here when no channel has data (’signalReceived == false’, ’notBlocked == true’, ’isError == false’).
			_setHeaterState(HeaterState.OFF.getValue());
		}

		// Map HeatPumpSmartGrid "SmartGridState" read values.
		if (this.getSgReadyOperatingMode().isDefined()) {
			int generalizedSgState = this.getSgReadyOperatingMode().get();
			_setSmartGridState(generalizedSgState);
		} else {
			_setSmartGridState(SmartGridState.UNDEFINED.getValue());
		}

		if (this.readOnly == false) {

			if (this.sgReadyActive) {

				// Map SG generalized "SmartGridState" write values.
				Optional<Integer> sgState = this.getSmartGridStateChannel().getNextWriteValueAndReset();
				if (sgState.isPresent()) {
					int generalizedSgStateWrite = sgState.get();
					boolean sgInput1;
					boolean sgInput2;
					switch (generalizedSgStateWrite) {
						case 0:
							// Off
							sgInput1 = false;
							sgInput2 = true;
							break;
						case 2:
							// Force on, increased temperature levels.
							sgInput1 = true;
							sgInput2 = false;
							break;
						case 3:
							// Force on, max temperature levels.
							sgInput1 = true;
							sgInput2 = true;
							break;
						case 1:
						default:
							// Standard
							sgInput1 = false;
							sgInput2 = false;
							break;
					}
					try {
						this.setSgReadyOnOff(true);
						this.getSgReadyInput1Channel().setNextWriteValue(sgInput1);
						this.getSgReadyInput2Channel().setNextWriteValue(sgInput2);
					} catch (OpenemsError.OpenemsNamedException e) {
						e.printStackTrace();
					}
				}
			}

			if (this.useEnableSignal || this.useExceptionalState) {

				// Handle EnableSignal.
				if (this.useEnableSignal) {
					this.turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);
				}

				// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
				int exceptionalStateValue = 0;
				boolean exceptionalStateActive = false;
				if (this.useExceptionalState) {
					exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
					if (exceptionalStateActive) {
						exceptionalStateValue = this.getExceptionalStateValue();
						if (exceptionalStateValue <= 0) {
							// Turn off heat pump when ExceptionalStateValue = 0.
							this.turnOnHeatpump = false;
						} else {
							// When ExceptionalStateValue is > 0, turn heat pump on.
							this.turnOnHeatpump = true;
							/*
							if (exceptionalStateValue > 100) {
								exceptionalStateValue = 100;
							}
							*/
						}
					}
				}

				if (this.turnOnHeatpump) {
					OperatingMode setModeTo = OperatingMode.STANDBY;
					switch (this.defaultModeOfOperation) {
						case "Programmbetrieb":
							setModeTo = OperatingMode.PROGRAM_MODE;
							break;
						case "Komfortbetrieb":
							setModeTo = OperatingMode.COMFORT_MODE;
							break;
						case "ECO-Betrieb":
							setModeTo = OperatingMode.ECO_MODE;
							break;
						case "Warmwasserbetrieb":
							setModeTo = OperatingMode.DOMESTIC_HOT_WATER;
							break;

						// "Bereitschaftsbetrieb" is OperatingMode.STANDBY, ’setModeTo’ is initialized as that.
					}
					try {
						this.setOperatingMode(setModeTo.getValue());
					} catch (OpenemsError.OpenemsNamedException e) {
						e.printStackTrace();
					}

					/* if (this.useExceptionalState) {
						// Currently not used. If exceptionalStateValue should do more than just on/off, code for that goes here.
					} */
				} else {
					try {
						this.setOperatingMode(OperatingMode.ANTIFREEZE.getValue());
					} catch (OpenemsError.OpenemsNamedException e) {
						e.printStackTrace();
					}
				}
			}

		}

		// Map "getCircuit1SetpointTemp" according to WPM version.
		if (getControllerModel().isDefined()) {
			int reglerId = getControllerModel().get();
			if (reglerId == 391) {
				// WPM version is 3i
				if (this.channel(HeatpumpTecalorChannel.ChannelId.IR509_SOLLTEMPHK1).value().isDefined()) {
					int setpointHk1 = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR509_SOLLTEMPHK1).value().get();
					this.getCircuit1SetpointTempChannel().setNextValue(setpointHk1);
				}
			} else {
				// WPM version is not 3i
				if (this.channel(HeatpumpTecalorChannel.ChannelId.IR510_SOLLTEMPHK1).value().isDefined()) {
					int setpointHk1 = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR510_SOLLTEMPHK1).value().get();
					this.getCircuit1SetpointTempChannel().setNextValue(setpointHk1);
				}
			}
		}

		// Map energy channels that are transmitted as two modbus values.
		boolean channelsHaveValues1 = this.channel(HeatpumpTecalorChannel.ChannelId.IR3502_HEATPRODUCED_VDHEIZENSUMKWH).value().isDefined()
				&& this.channel(HeatpumpTecalorChannel.ChannelId.IR3503_HEATPRODUCED_VDHEIZENSUMMWH).value().isDefined();
		if (channelsHaveValues1) {
			int sum = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3502_HEATPRODUCED_VDHEIZENSUMKWH).value().get()
					+ ((Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3503_HEATPRODUCED_VDHEIZENSUMMWH).value().get() * 1000);
			this.getProducedHeatCircuitTotalChannel().setNextValue(sum);
		}
		boolean channelsHaveValues2 = this.channel(HeatpumpTecalorChannel.ChannelId.IR3505_HEATPRODUCED_VDWWSUMKWH).value().isDefined()
				&& this.channel(HeatpumpTecalorChannel.ChannelId.IR3506_HEATPRODUCED_VDWWSUMMWH).value().isDefined();
		if (channelsHaveValues2) {
			int sum = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3505_HEATPRODUCED_VDWWSUMKWH).value().get()
					+ ((Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3506_HEATPRODUCED_VDWWSUMMWH).value().get() * 1000);
			this.getProducedHeatWaterTotalChannel().setNextValue(sum);
		}
		boolean channelsHaveValues3 = this.channel(HeatpumpTecalorChannel.ChannelId.IR3507_HEATPRODUCED_NZHHEIZENSUMKWH).value().isDefined()
				&& this.channel(HeatpumpTecalorChannel.ChannelId.IR3508_HEATPRODUCED_NZHHEIZENSUMMWH).value().isDefined();
		if (channelsHaveValues3) {
			int sum = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3507_HEATPRODUCED_NZHHEIZENSUMKWH).value().get()
					+ ((Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3508_HEATPRODUCED_NZHHEIZENSUMMWH).value().get() * 1000);
			this.getProducedHeatCircuitTotalAuxChannel().setNextValue(sum);
		}
		boolean channelsHaveValues4 = this.channel(HeatpumpTecalorChannel.ChannelId.IR3509_HEATPRODUCED_NZHWWSUMKWH).value().isDefined()
				&& this.channel(HeatpumpTecalorChannel.ChannelId.IR3510_HEATPRODUCED_NZHWWSUMMWH).value().isDefined();
		if (channelsHaveValues4) {
			int sum = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3509_HEATPRODUCED_NZHWWSUMKWH).value().get()
					+ ((Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3510_HEATPRODUCED_NZHWWSUMMWH).value().get() * 1000);
			this.getProducedHeatWaterTotalAuxChannel().setNextValue(sum);
		}
		boolean channelsHaveValues5 = this.channel(HeatpumpTecalorChannel.ChannelId.IR3512_CONSUMEDPOWER_VDHEIZENSUMKWH).value().isDefined()
				&& this.channel(HeatpumpTecalorChannel.ChannelId.IR3513_CONSUMEDPOWER_VDHEIZENSUMMWH).value().isDefined();
		if (channelsHaveValues5) {
			int sum = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3512_CONSUMEDPOWER_VDHEIZENSUMKWH).value().get()
					+ ((Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3513_CONSUMEDPOWER_VDHEIZENSUMMWH).value().get() * 1000);
			this.getConsumedPowerCircuitTotalChannel().setNextValue(sum);
		}
		boolean channelsHaveValues6 = this.channel(HeatpumpTecalorChannel.ChannelId.IR3515_CONSUMEDPOWER_VDWWSUMKWH).value().isDefined()
				&& this.channel(HeatpumpTecalorChannel.ChannelId.IR3516_CONSUMEDPOWER_VDWWSUMMWH).value().isDefined();
		if (channelsHaveValues6) {
			int sum = (Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3515_CONSUMEDPOWER_VDWWSUMKWH).value().get()
					+ ((Integer) this.channel(HeatpumpTecalorChannel.ChannelId.IR3516_CONSUMEDPOWER_VDWWSUMMWH).value().get() * 1000);
			this.getConsumedPowerWaterTotalChannel().setNextValue(sum);
		}

		if (this.debug) {
			this.logInfo(this.log, "--Heat pump Tecalor--");
			this.logInfo(this.log, "Status Bits 2501:");
			this.logInfo(this.log, "0 - HK1 Pumpe = " + (((statusBits & 0b01) == 0b01) ? 1 : 0));
			this.logInfo(this.log, "1 - HK2 Pumpe = " + (((statusBits & 0b010) == 0b010) ? 1 : 0));
			this.logInfo(this.log, "2 - Aufheizprogramm = " + (((statusBits & 0b0100) == 0b0100) ? 1 : 0));
			this.logInfo(this.log, "3 - NHZ Stufen in Betrieb = " + (((statusBits & 0b01000) == 0b01000) ? 1 : 0));
			this.logInfo(this.log, "4 - WP im Heizbetrieb = " + (((statusBits & 0b010000) == 0b010000) ? 1 : 0));
			this.logInfo(this.log, "5 - WP im Warmwasserbetrieb = " + (((statusBits & 0b0100000) == 0b0100000) ? 1 : 0));
			this.logInfo(this.log, "6 - Verdichter in Betrieb = " + (((statusBits & 0b01000000) == 0b01000000) ? 1 : 0));
			this.logInfo(this.log, "7 - Sommerbetrieb aktiv = " + (((statusBits & 0b010000000) == 0b010000000) ? 1 : 0));
			this.logInfo(this.log, "8 - Kühlbetrieb aktiv = " + (((statusBits & 0b0100000000) == 0b0100000000) ? 1 : 0));
			this.logInfo(this.log, "9 - Min. eine IWS im Abtaubetrieb = " + (((statusBits & 0b01000000000) == 0b01000000000) ? 1 : 0));
			this.logInfo(this.log, "10 - Silentmode1 aktiv = " + (((statusBits & 0b010000000000) == 0b010000000000) ? 1 : 0));
			this.logInfo(this.log, "11 - Silentmode2 aktiv = " + (((statusBits & 0b0100000000000) == 0b0100000000000) ? 1 : 0));
			this.logInfo(this.log, "");
			this.logInfo(this.log, "SmartGrid-Modus (Tecalor, 1-4): " + getSgReadyOperatingMode());
			this.logInfo(this.log, "Reglerkennung: " + getControllerModel());
			this.logInfo(this.log, "EVU-Freigabe: " + getElSupBlockRelease());
			this.logInfo(this.log, "Fehlerstatus: " + getErrorStatus());
			this.logInfo(this.log, "Fehlernummer: " + getErrorCode());
			this.logInfo(this.log, "");
			this.logInfo(this.log, "Aussentemp: " + getOutsideTemp());
			this.logInfo(this.log, "Pufferspeicher Temp: " + getStorageTankTemp());
			this.logInfo(this.log, "Heizkreis1 Temp: " + getCircuit1Temp());
			this.logInfo(this.log, "Heizkreis2 Temp: " + getCircuit2Temp());
			this.logInfo(this.log, "Vorlauf Temp: " + getFlowTemperature());
			this.logInfo(this.log, "Rücklauf Temp: " + getReturnTemperature());
			this.logInfo(this.log, "Warmwasser Temp: " + getDomesticHotWaterTemp());
			this.logInfo(this.log, "");
			this.logInfo(this.log, "--Schreibbare Parameter--");
			this.logInfo(this.log, "Betriebsart: " + getOperatingModeChannel().value().asEnum().getName());
			this.logInfo(this.log, "SmartGrid-Ready aktiviert: " + getSgReadyOnOff());
			this.logInfo(this.log, "SmartGrid-Modus (OpenEMS, 0-3): " + getSmartGridStateChannel().value().asEnum().getName());
			this.logInfo(this.log, "");
		}

	}

}
