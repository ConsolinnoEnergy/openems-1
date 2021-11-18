package io.openems.edge.heater.gasboiler.viessmann;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
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
import io.openems.edge.heater.gasboiler.viessmann.api.GasBoilerViessmann;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * This module reads the most important variables available via Modbus from a Viessmann gas boiler and maps them to OpenEMS
 * channels. The module is written to be used with the Heater interface methods (EnableSignal) and ExceptionalState.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like temperature specified,
 * the heater will turn on with default settings. The default settings are configurable in the config.
 * The heater can be controlled with setHeatingPowerPercentSetpoint() (set power in %) or setTemperatureSetpoint().
 * However, currently the code does not yet support setTemperatureSetpoint().
 * setHeatingPowerSetpoint() (set power in kW) and related methods are currently not supported by this heater.
 * If the heater is activated by ExceptionalState, it will go to the setHeatingPowerPercentSetpoint() value specified
 * by the ExceptionalStateValue.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.GasBoiler.Viessmann",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = { //
        EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
        EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
        })
public class GasBoilerViessmannImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, GasBoilerViessmann {

    private final Logger log = LoggerFactory.getLogger(GasBoilerViessmannImpl.class);

    @Reference
    ComponentManager cpm;

    private boolean printInfoToLog;
    private boolean readOnly = false;
    private double heatingPowerPercentSetting;
    static final int hvac_off = 6;

    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "GASBOILER_VIESSMANN_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "GASBOILER_VIESSMANN_EXCEPTIONAL_STATE_IDENTIFIER";

    private final String[] errorList = ErrorList.STANDARD_ERRORS.getErrorList();

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    protected ConfigurationAdmin cm;

    public GasBoilerViessmannImpl() {
        super(OpenemsComponent.ChannelId.values(),
                GasBoilerViessmann.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException,
            ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbusBridgeId());

        this.printInfoToLog = config.printInfoToLog();
        this.readOnly = config.readOnly();
        if (this.isEnabled() == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }

        // Settings needed when not in ’read only’ mode.
        if (this.readOnly == false) {
            this.heatingPowerPercentSetting = config.defaultSetPointPowerPercent();
            this.initializeTimers(config);

            // Deactivating controllers for heating circuits because we will not need them
            this.setHc1OperatingMode(hvac_off);
            this.setHc2OperatingMode(hvac_off);
            this.setHc3OperatingMode(hvac_off);
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
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this,
                new FC2ReadInputsTask(200, Priority.HIGH,
                        m(GasBoilerViessmann.ChannelId.DISTURBANCE, new CoilElement(200)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_1, new CoilElement(201)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_2, new CoilElement(202)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_3, new CoilElement(203)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_4, new CoilElement(204)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_5, new CoilElement(205)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_6, new CoilElement(206)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_7, new CoilElement(207)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_8, new CoilElement(208)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_9, new CoilElement(209)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_10, new CoilElement(210)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_11, new CoilElement(211)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_12, new CoilElement(212)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_13, new CoilElement(213)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_14, new CoilElement(214)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_15, new CoilElement(215)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_16, new CoilElement(216)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_17, new CoilElement(217)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_18, new CoilElement(218)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_19, new CoilElement(219)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_20, new CoilElement(220)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_21, new CoilElement(221)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_22, new CoilElement(222)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_23, new CoilElement(223)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_24, new CoilElement(224)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_25, new CoilElement(225)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_26, new CoilElement(226)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_27, new CoilElement(227)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_28, new CoilElement(228)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_29, new CoilElement(229)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_30, new CoilElement(230)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_31, new CoilElement(231)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_32, new CoilElement(232)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_33, new CoilElement(233)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_34, new CoilElement(234)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_35, new CoilElement(235)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_36, new CoilElement(236)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_37, new CoilElement(237)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_38, new CoilElement(238)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_39, new CoilElement(239)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_40, new CoilElement(240)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_41, new CoilElement(241)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_42, new CoilElement(242)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_43, new CoilElement(243)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_44, new CoilElement(244)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_45, new CoilElement(245)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_46, new CoilElement(246)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_47, new CoilElement(247)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_48, new CoilElement(248)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_49, new CoilElement(249)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_50, new CoilElement(250)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_51, new CoilElement(251)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_52, new CoilElement(252)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_53, new CoilElement(253)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_54, new CoilElement(254)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_55, new CoilElement(255)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_56, new CoilElement(256)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_57, new CoilElement(257)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_58, new CoilElement(258)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_59, new CoilElement(259)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_60, new CoilElement(260)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_61, new CoilElement(261)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_62, new CoilElement(262)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_63, new CoilElement(263)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_64, new CoilElement(264)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_65, new CoilElement(265)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_66, new CoilElement(266)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_67, new CoilElement(267)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_68, new CoilElement(268)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_69, new CoilElement(269)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_70, new CoilElement(270)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_71, new CoilElement(271)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_72, new CoilElement(272)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_73, new CoilElement(273)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_74, new CoilElement(274)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_75, new CoilElement(275)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_76, new CoilElement(276)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_77, new CoilElement(277)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_78, new CoilElement(278)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_79, new CoilElement(279)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_80, new CoilElement(280)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_81, new CoilElement(281)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_82, new CoilElement(282)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_83, new CoilElement(283)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_84, new CoilElement(284)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_85, new CoilElement(285)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_86, new CoilElement(286)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_87, new CoilElement(287)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_88, new CoilElement(288)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_89, new CoilElement(289)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_90, new CoilElement(290)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_91, new CoilElement(291)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_92, new CoilElement(292)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_93, new CoilElement(293)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_94, new CoilElement(294)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_95, new CoilElement(295)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_96, new CoilElement(296)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_97, new CoilElement(297)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_98, new CoilElement(298)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_99, new CoilElement(299)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_100, new CoilElement(300)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_101, new CoilElement(301)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_102, new CoilElement(302)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_103, new CoilElement(303)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_104, new CoilElement(304)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_105, new CoilElement(305)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_106, new CoilElement(306)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_107, new CoilElement(307)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_108, new CoilElement(308)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_109, new CoilElement(309)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_110, new CoilElement(310)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_111, new CoilElement(311)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_112, new CoilElement(312)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_113, new CoilElement(313)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_114, new CoilElement(314)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_115, new CoilElement(315)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_116, new CoilElement(316)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_117, new CoilElement(317)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_118, new CoilElement(318)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_119, new CoilElement(319)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_120, new CoilElement(320)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_121, new CoilElement(321)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_122, new CoilElement(322)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_123, new CoilElement(323)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_124, new CoilElement(324)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_125, new CoilElement(325)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_126, new CoilElement(326)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_127, new CoilElement(327)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_128, new CoilElement(328)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_129, new CoilElement(329)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_130, new CoilElement(330)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_131, new CoilElement(331)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_132, new CoilElement(332)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_133, new CoilElement(333)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_134, new CoilElement(334)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_135, new CoilElement(335)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_136, new CoilElement(336)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_137, new CoilElement(337)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_138, new CoilElement(338)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_139, new CoilElement(339)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_140, new CoilElement(340)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_141, new CoilElement(341)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_142, new CoilElement(342)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_143, new CoilElement(343)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_144, new CoilElement(344)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_145, new CoilElement(345)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_146, new CoilElement(346)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_147, new CoilElement(347)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_148, new CoilElement(348)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_149, new CoilElement(349)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_150, new CoilElement(350)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_151, new CoilElement(351)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_152, new CoilElement(352)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_153, new CoilElement(353)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_154, new CoilElement(354)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_155, new CoilElement(355)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_156, new CoilElement(356)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_157, new CoilElement(357)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_158, new CoilElement(358)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_159, new CoilElement(359)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_160, new CoilElement(360)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_161, new CoilElement(361)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_162, new CoilElement(362)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_163, new CoilElement(363)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_164, new CoilElement(364)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_165, new CoilElement(365)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_166, new CoilElement(366)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_167, new CoilElement(367)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_168, new CoilElement(368)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_169, new CoilElement(369)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_170, new CoilElement(370)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_171, new CoilElement(371)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_172, new CoilElement(372)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_173, new CoilElement(373)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_174, new CoilElement(374)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_175, new CoilElement(375)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_176, new CoilElement(376)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_177, new CoilElement(377)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_178, new CoilElement(378)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_179, new CoilElement(379)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_180, new CoilElement(380)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_181, new CoilElement(381)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_182, new CoilElement(382)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_183, new CoilElement(383)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_184, new CoilElement(384)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_185, new CoilElement(385)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_186, new CoilElement(386)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_187, new CoilElement(387)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_188, new CoilElement(388)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_189, new CoilElement(389)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_190, new CoilElement(390)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_191, new CoilElement(391)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_192, new CoilElement(392)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_193, new CoilElement(393)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_194, new CoilElement(394)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_195, new CoilElement(395)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_196, new CoilElement(396)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_197, new CoilElement(397)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_198, new CoilElement(398)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_199, new CoilElement(399)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_200, new CoilElement(400)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_201, new CoilElement(401)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_202, new CoilElement(402)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_203, new CoilElement(403)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_204, new CoilElement(404)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_205, new CoilElement(405)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_206, new CoilElement(406)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_207, new CoilElement(407)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_208, new CoilElement(408)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_209, new CoilElement(409)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_210, new CoilElement(410)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_211, new CoilElement(411)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_212, new CoilElement(412)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_213, new CoilElement(413)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_214, new CoilElement(414)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_215, new CoilElement(415)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_216, new CoilElement(416)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_217, new CoilElement(417)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_218, new CoilElement(418)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_219, new CoilElement(419)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_220, new CoilElement(420)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_221, new CoilElement(421)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_222, new CoilElement(422)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_223, new CoilElement(423)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_224, new CoilElement(424)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_225, new CoilElement(425)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_226, new CoilElement(426)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_227, new CoilElement(427)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_228, new CoilElement(428)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_229, new CoilElement(429)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_230, new CoilElement(430)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_231, new CoilElement(431)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_232, new CoilElement(432)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_233, new CoilElement(433)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_234, new CoilElement(434)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_235, new CoilElement(435)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_236, new CoilElement(436)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_237, new CoilElement(437)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_238, new CoilElement(438)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_239, new CoilElement(439)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_240, new CoilElement(440)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_241, new CoilElement(441)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_242, new CoilElement(442)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_243, new CoilElement(443)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_244, new CoilElement(444)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_245, new CoilElement(445)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_246, new CoilElement(446)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_247, new CoilElement(447)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_248, new CoilElement(448)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_249, new CoilElement(449)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_250, new CoilElement(450)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_251, new CoilElement(451)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_252, new CoilElement(452)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_253, new CoilElement(453)),
                        m(GasBoilerViessmann.ChannelId.ERROR_BIT_255, new CoilElement(455))
                ),

                // read holding registers
                new FC3ReadRegistersTask(5, Priority.HIGH,
                        m(GasBoilerViessmann.ChannelId.DEVICE_OPERATING_MODE, new UnsignedWordElement(5)),
                        new DummyRegisterElement(6, 9),
                        m(GasBoilerViessmann.ChannelId.DEVICE_POWER_MODE, new UnsignedWordElement(10)),
                        m(Heater.ChannelId.SET_POINT_HEATING_POWER_PERCENT, new UnsignedWordElement(11), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(12, 12),
                        m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(14, 16),
                        m(GasBoilerViessmann.ChannelId.HC1_OPERATING_MODE, new UnsignedWordElement(17)),
                        new DummyRegisterElement(18, 19),
                        m(GasBoilerViessmann.ChannelId.HC2_OPERATING_MODE, new UnsignedWordElement(20)),
                        new DummyRegisterElement(21, 22),
                        m(GasBoilerViessmann.ChannelId.HC3_OPERATING_MODE, new UnsignedWordElement(23))
                ),

                // read input registers
                new FC4ReadInputRegistersTask(6, Priority.HIGH,
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new UnsignedWordElement(6), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(7, 7),
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(8), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(9, 30),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(31)),
                        new DummyRegisterElement(32, 35),
                        m(GasBoilerViessmann.ChannelId.OPERATING_HOURS_TIER1, new UnsignedWordElement(36)),
                        m(GasBoilerViessmann.ChannelId.OPERATING_HOURS_TIER2, new UnsignedWordElement(37)),
                        m(GasBoilerViessmann.ChannelId.BOILER_STARTS, new UnsignedWordElement(38)),
                        m(GasBoilerViessmann.ChannelId.BOILER_STATE, new UnsignedWordElement(39))
                )
        );
        if (this.readOnly == false) {
            protocol.addTasks(
                    // Write to holding registers
                    new FC6WriteRegisterTask(5,
                            m(GasBoilerViessmann.ChannelId.DEVICE_OPERATING_MODE, new UnsignedWordElement(5))),
                    new FC6WriteRegisterTask(10,
                            m(GasBoilerViessmann.ChannelId.DEVICE_POWER_MODE, new UnsignedWordElement(10))),
                    new FC6WriteRegisterTask(11,
                            m(GasBoilerViessmann.ChannelId.HR11_MODBUS, new UnsignedWordElement(11), ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
                    new FC6WriteRegisterTask(13,
                            m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
                    new FC6WriteRegisterTask(17,
                            m(GasBoilerViessmann.ChannelId.HC1_OPERATING_MODE, new UnsignedWordElement(17))),
                    new FC6WriteRegisterTask(20,
                            m(GasBoilerViessmann.ChannelId.HC2_OPERATING_MODE, new UnsignedWordElement(20))),
                    new FC6WriteRegisterTask(23,
                            m(GasBoilerViessmann.ChannelId.HC3_OPERATING_MODE, new UnsignedWordElement(23)))
            );
        }
        return protocol;
    }

    private List<String> generateErrorList() {
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < 255; i++) {
            if (this.getError(i + 1).isDefined()) {
                if (this.getError(i + 1).get()) {
                    errorList.add(this.errorList[i]);
                }
            }
        }
        return errorList;
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() == false) {
            return;
        }
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            this.channelmapping();
            if (this.printInfoToLog) {
                this.printInfo();
            }
        } else if (this.readOnly == false && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            this.writeCommands();
        }
    }

    /**
     * Put values in channels that are not directly Modbus read values but derivatives.
     */
    protected void channelmapping() {

        // Parse state.
        if (getBoilerState().isDefined()) {

            // Parse errors.
            List<String> errorList = this.generateErrorList();
            if ((errorList.size() > 0)) {
                this._setErrorMessage(errorList.toString());
            } else {
                this._setErrorMessage("No error");
            }

            int boilerState = getBoilerState().get();
            if (boilerState > 0) {
                this._setHeaterState(HeaterState.RUNNING.getValue());
            } else {
                this._setHeaterState(HeaterState.STANDBY.getValue());
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
            this._setErrorMessage("No Modbus connection");
        }
        this.getErrorMessageChannel().nextProcessImage();
        this.getHeaterStateChannel().nextProcessImage();
    }

    /**
     * Determine commands and send them to the heater.
     * The channel SET_POINT_HEATING_POWER_PERCENT gets special treatment, because that is changed by ExceptionalState.
     * The write of that channel is not mapped to Modbus. This is done by a duplicate ’private’ channel. The write to
     * the ’public’ channel SET_POINT_HEATING_POWER_PERCENT is stored in a local variable and sent to Modbus using the
     * ’private’ channel.
     * The benefit of this design is that when ExceptionalState is active and applies it's own heatingPowerPercentSetpoint,
     * the previous set point is saved. Also, it is still possible to write to the channel during ExceptionalState.
     * A write to SET_POINT_HEATING_POWER_PERCENT is still registered and the value saved, but not executed. The changed
     * set point is then applied once ExceptionalState ends. This way you don't have to pay attention to the state of
     * the heater when writing in the SET_POINT_HEATING_POWER_PERCENT channel.
     */
    protected void writeCommands() {
        // Collect heatingPowerPercentSetpoint channel ’nextWrite’.
        Optional<Double> heatingPowerPercentOptional = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
        if (heatingPowerPercentOptional.isPresent()) {
            double setpoint = heatingPowerPercentOptional.get();
            // Restrict to valid write values
            setpoint = Math.min(setpoint, 100);
            setpoint = Math.max(0, setpoint);
            this.heatingPowerPercentSetting = setpoint;
        }

        // Handle EnableSignal.
        boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

        // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
        double heatingPowerPercentSetpointToModbus = this.heatingPowerPercentSetting;
        boolean exceptionalStateActive = false;
        if (this.useExceptionalState) {
            exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
            if (exceptionalStateActive) {
                int exceptionalStateValue = this.getExceptionalStateValue();
                if (exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
                    turnOnHeater = false;
                } else {
                    // When ExceptionalStateValue is between 0 and 100, set heater to this PowerPercentage.
                    turnOnHeater = true;
                    exceptionalStateValue = Math.min(exceptionalStateValue, this.DEFAULT_MAX_EXCEPTIONAL_VALUE);
                    heatingPowerPercentSetpointToModbus = exceptionalStateValue;
                }
            }
        }

        // Switch heater on or off.
        if (turnOnHeater) {
            try {
                this.setDevicePowerMode(1);
                this.getHr11ModbusChannel().setNextWriteValue(heatingPowerPercentSetpointToModbus);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't write in Channel " + e.getMessage());
            }
        } else {
            try {
                this.setDevicePowerMode(0);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't write in Channel " + e.getMessage());
            }
        }
    }

    /**
     * Information that is printed to the log if ’print info to log’ option is enabled.
     */
    protected void printInfo() {
        this.logInfo(this.log, "--Gasboiler Viessmann Vitotronic 100--");
        this.logInfo(this.log, "Power percent set point (write mode only): " + this.getHeatingPowerPercentSetpoint());
        this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
        this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
        //this.logInfo(this.log, "Operating mode: " + this.getDeviceOperatingMode());
        this.logInfo(this.log, "Operating hours tier1: " + this.getOperatingHoursTier1());
        this.logInfo(this.log, "Operating hours tier2: " + this.getOperatingHoursTier2());
        this.logInfo(this.log, "Boiler start counter: " + this.getBoilerStarts());
        this.logInfo(this.log, "Heater state: " + this.getHeaterState());
        this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
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
        if (this.getErrorMessage().get().equals("No error") == false) {
            debugMessage = debugMessage + "|Error";
        }
        return debugMessage;
    }
}
