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
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
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
import io.openems.edge.heater.gasboiler.viessmann.api.GasBoiler;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Viessmann.GasBoiler",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class GasBoilerImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, GasBoiler {

    private final Logger log = LoggerFactory.getLogger(GasBoilerImpl.class);

    @Reference
    ComponentManager cpm;

    private boolean componentEnabled;
    GasBoilerType gasBoilerType;
    private boolean printInfoToLog;
    private boolean readOnly = false;

    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "GASBOILER_VIESSMANN_RELAY_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "GASBOILER_VIESSMANN_RELAY_EXCEPTIONAL_STATE_IDENTIFIER";

    private final String[] errorList = ErrorList.STANDARD_ERRORS.getErrorList();

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    protected ConfigurationAdmin cm;

    public GasBoilerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                GasBoiler.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException,
            ConfigurationException {
        this.allocateGasBoilerType(config.gasBoilerType());
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbusBridgeId());
        this.componentEnabled = config.enabled();
        this.printInfoToLog = config.printInfoToLog();
        this.setHeatingPowerPercentSetpoint(config.defaultSetPointPowerPercent());
        this.readOnly = config.readOnly();

        if (this.readOnly == false) {
            TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
            String timerTypeEnableSignal;
            if (config.enableSignalTimerIsCyclesNotSeconds()) {
                timerTypeEnableSignal = "TimerByCycles";
            } else {
                timerTypeEnableSignal = "TimerByTime";
            }
            timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerTypeEnableSignal, config.waitTimeEnableSignal());
            this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
            this.useExceptionalState = config.useExceptionalState();
            if (this.useExceptionalState) {
                String timerTypeExceptionalState;
                if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
                    timerTypeExceptionalState = "TimerByCycles";
                } else {
                    timerTypeExceptionalState = "TimerByTime";
                }
                timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeExceptionalState, config.waitTimeExceptionalState());
                this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
            }

            // Deactivating controllers for heating circuits because we will not need them
            this.getHc1OperationMode().setNextWriteValue(6);
            this.getHc2OperationMode().setNextWriteValue(6);
            this.getHc3OperationMode().setNextWriteValue(6);
        }

        if (this.componentEnabled == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }
    }

    private void allocateGasBoilerType(String gasBoilerType) {
        switch (gasBoilerType) {
            case "Placeholder":
            case "VITOTRONIC_100":
            default:
                this.gasBoilerType = GasBoilerType.VITOTRONIC_100;
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this,
                new FC2ReadInputsTask(200, Priority.HIGH,
                        m(GasBoiler.ChannelId.DISTURBANCE, new CoilElement(200)),
                        m(GasBoiler.ChannelId.ERROR_BIT_1, new CoilElement(201)),
                        m(GasBoiler.ChannelId.ERROR_BIT_2, new CoilElement(202)),
                        m(GasBoiler.ChannelId.ERROR_BIT_3, new CoilElement(203)),
                        m(GasBoiler.ChannelId.ERROR_BIT_4, new CoilElement(204)),
                        m(GasBoiler.ChannelId.ERROR_BIT_5, new CoilElement(205)),
                        m(GasBoiler.ChannelId.ERROR_BIT_6, new CoilElement(206)),
                        m(GasBoiler.ChannelId.ERROR_BIT_7, new CoilElement(207)),
                        m(GasBoiler.ChannelId.ERROR_BIT_8, new CoilElement(208)),
                        m(GasBoiler.ChannelId.ERROR_BIT_9, new CoilElement(209)),
                        m(GasBoiler.ChannelId.ERROR_BIT_10, new CoilElement(210)),
                        m(GasBoiler.ChannelId.ERROR_BIT_11, new CoilElement(211)),
                        m(GasBoiler.ChannelId.ERROR_BIT_12, new CoilElement(212)),
                        m(GasBoiler.ChannelId.ERROR_BIT_13, new CoilElement(213)),
                        m(GasBoiler.ChannelId.ERROR_BIT_14, new CoilElement(214)),
                        m(GasBoiler.ChannelId.ERROR_BIT_15, new CoilElement(215)),
                        m(GasBoiler.ChannelId.ERROR_BIT_16, new CoilElement(216)),
                        m(GasBoiler.ChannelId.ERROR_BIT_17, new CoilElement(217)),
                        m(GasBoiler.ChannelId.ERROR_BIT_18, new CoilElement(218)),
                        m(GasBoiler.ChannelId.ERROR_BIT_19, new CoilElement(219)),
                        m(GasBoiler.ChannelId.ERROR_BIT_20, new CoilElement(220)),
                        m(GasBoiler.ChannelId.ERROR_BIT_21, new CoilElement(221)),
                        m(GasBoiler.ChannelId.ERROR_BIT_22, new CoilElement(222)),
                        m(GasBoiler.ChannelId.ERROR_BIT_23, new CoilElement(223)),
                        m(GasBoiler.ChannelId.ERROR_BIT_24, new CoilElement(224)),
                        m(GasBoiler.ChannelId.ERROR_BIT_25, new CoilElement(225)),
                        m(GasBoiler.ChannelId.ERROR_BIT_26, new CoilElement(226)),
                        m(GasBoiler.ChannelId.ERROR_BIT_27, new CoilElement(227)),
                        m(GasBoiler.ChannelId.ERROR_BIT_28, new CoilElement(228)),
                        m(GasBoiler.ChannelId.ERROR_BIT_29, new CoilElement(229)),
                        m(GasBoiler.ChannelId.ERROR_BIT_30, new CoilElement(230)),
                        m(GasBoiler.ChannelId.ERROR_BIT_31, new CoilElement(231)),
                        m(GasBoiler.ChannelId.ERROR_BIT_32, new CoilElement(232)),
                        m(GasBoiler.ChannelId.ERROR_BIT_33, new CoilElement(233)),
                        m(GasBoiler.ChannelId.ERROR_BIT_34, new CoilElement(234)),
                        m(GasBoiler.ChannelId.ERROR_BIT_35, new CoilElement(235)),
                        m(GasBoiler.ChannelId.ERROR_BIT_36, new CoilElement(236)),
                        m(GasBoiler.ChannelId.ERROR_BIT_37, new CoilElement(237)),
                        m(GasBoiler.ChannelId.ERROR_BIT_38, new CoilElement(238)),
                        m(GasBoiler.ChannelId.ERROR_BIT_39, new CoilElement(239)),
                        m(GasBoiler.ChannelId.ERROR_BIT_40, new CoilElement(240)),
                        m(GasBoiler.ChannelId.ERROR_BIT_41, new CoilElement(241)),
                        m(GasBoiler.ChannelId.ERROR_BIT_42, new CoilElement(242)),
                        m(GasBoiler.ChannelId.ERROR_BIT_43, new CoilElement(243)),
                        m(GasBoiler.ChannelId.ERROR_BIT_44, new CoilElement(244)),
                        m(GasBoiler.ChannelId.ERROR_BIT_45, new CoilElement(245)),
                        m(GasBoiler.ChannelId.ERROR_BIT_46, new CoilElement(246)),
                        m(GasBoiler.ChannelId.ERROR_BIT_47, new CoilElement(247)),
                        m(GasBoiler.ChannelId.ERROR_BIT_48, new CoilElement(248)),
                        m(GasBoiler.ChannelId.ERROR_BIT_49, new CoilElement(249)),
                        m(GasBoiler.ChannelId.ERROR_BIT_50, new CoilElement(250)),
                        m(GasBoiler.ChannelId.ERROR_BIT_51, new CoilElement(251)),
                        m(GasBoiler.ChannelId.ERROR_BIT_52, new CoilElement(252)),
                        m(GasBoiler.ChannelId.ERROR_BIT_53, new CoilElement(253)),
                        m(GasBoiler.ChannelId.ERROR_BIT_54, new CoilElement(254)),
                        m(GasBoiler.ChannelId.ERROR_BIT_55, new CoilElement(255)),
                        m(GasBoiler.ChannelId.ERROR_BIT_56, new CoilElement(256)),
                        m(GasBoiler.ChannelId.ERROR_BIT_57, new CoilElement(257)),
                        m(GasBoiler.ChannelId.ERROR_BIT_58, new CoilElement(258)),
                        m(GasBoiler.ChannelId.ERROR_BIT_59, new CoilElement(259)),
                        m(GasBoiler.ChannelId.ERROR_BIT_60, new CoilElement(260)),
                        m(GasBoiler.ChannelId.ERROR_BIT_61, new CoilElement(261)),
                        m(GasBoiler.ChannelId.ERROR_BIT_62, new CoilElement(262)),
                        m(GasBoiler.ChannelId.ERROR_BIT_63, new CoilElement(263)),
                        m(GasBoiler.ChannelId.ERROR_BIT_64, new CoilElement(264)),
                        m(GasBoiler.ChannelId.ERROR_BIT_65, new CoilElement(265)),
                        m(GasBoiler.ChannelId.ERROR_BIT_66, new CoilElement(266)),
                        m(GasBoiler.ChannelId.ERROR_BIT_67, new CoilElement(267)),
                        m(GasBoiler.ChannelId.ERROR_BIT_68, new CoilElement(268)),
                        m(GasBoiler.ChannelId.ERROR_BIT_69, new CoilElement(269)),
                        m(GasBoiler.ChannelId.ERROR_BIT_70, new CoilElement(270)),
                        m(GasBoiler.ChannelId.ERROR_BIT_71, new CoilElement(271)),
                        m(GasBoiler.ChannelId.ERROR_BIT_72, new CoilElement(272)),
                        m(GasBoiler.ChannelId.ERROR_BIT_73, new CoilElement(273)),
                        m(GasBoiler.ChannelId.ERROR_BIT_74, new CoilElement(274)),
                        m(GasBoiler.ChannelId.ERROR_BIT_75, new CoilElement(275)),
                        m(GasBoiler.ChannelId.ERROR_BIT_76, new CoilElement(276)),
                        m(GasBoiler.ChannelId.ERROR_BIT_77, new CoilElement(277)),
                        m(GasBoiler.ChannelId.ERROR_BIT_78, new CoilElement(278)),
                        m(GasBoiler.ChannelId.ERROR_BIT_79, new CoilElement(279)),
                        m(GasBoiler.ChannelId.ERROR_BIT_80, new CoilElement(280)),
                        m(GasBoiler.ChannelId.ERROR_BIT_81, new CoilElement(281)),
                        m(GasBoiler.ChannelId.ERROR_BIT_82, new CoilElement(282)),
                        m(GasBoiler.ChannelId.ERROR_BIT_83, new CoilElement(283)),
                        m(GasBoiler.ChannelId.ERROR_BIT_84, new CoilElement(284)),
                        m(GasBoiler.ChannelId.ERROR_BIT_85, new CoilElement(285)),
                        m(GasBoiler.ChannelId.ERROR_BIT_86, new CoilElement(286)),
                        m(GasBoiler.ChannelId.ERROR_BIT_87, new CoilElement(287)),
                        m(GasBoiler.ChannelId.ERROR_BIT_88, new CoilElement(288)),
                        m(GasBoiler.ChannelId.ERROR_BIT_89, new CoilElement(289)),
                        m(GasBoiler.ChannelId.ERROR_BIT_90, new CoilElement(290)),
                        m(GasBoiler.ChannelId.ERROR_BIT_91, new CoilElement(291)),
                        m(GasBoiler.ChannelId.ERROR_BIT_92, new CoilElement(292)),
                        m(GasBoiler.ChannelId.ERROR_BIT_93, new CoilElement(293)),
                        m(GasBoiler.ChannelId.ERROR_BIT_94, new CoilElement(294)),
                        m(GasBoiler.ChannelId.ERROR_BIT_95, new CoilElement(295)),
                        m(GasBoiler.ChannelId.ERROR_BIT_96, new CoilElement(296)),
                        m(GasBoiler.ChannelId.ERROR_BIT_97, new CoilElement(297)),
                        m(GasBoiler.ChannelId.ERROR_BIT_98, new CoilElement(298)),
                        m(GasBoiler.ChannelId.ERROR_BIT_99, new CoilElement(299)),
                        m(GasBoiler.ChannelId.ERROR_BIT_100, new CoilElement(300)),
                        m(GasBoiler.ChannelId.ERROR_BIT_101, new CoilElement(301)),
                        m(GasBoiler.ChannelId.ERROR_BIT_102, new CoilElement(302)),
                        m(GasBoiler.ChannelId.ERROR_BIT_103, new CoilElement(303)),
                        m(GasBoiler.ChannelId.ERROR_BIT_104, new CoilElement(304)),
                        m(GasBoiler.ChannelId.ERROR_BIT_105, new CoilElement(305)),
                        m(GasBoiler.ChannelId.ERROR_BIT_106, new CoilElement(306)),
                        m(GasBoiler.ChannelId.ERROR_BIT_107, new CoilElement(307)),
                        m(GasBoiler.ChannelId.ERROR_BIT_108, new CoilElement(308)),
                        m(GasBoiler.ChannelId.ERROR_BIT_109, new CoilElement(309)),
                        m(GasBoiler.ChannelId.ERROR_BIT_110, new CoilElement(310)),
                        m(GasBoiler.ChannelId.ERROR_BIT_111, new CoilElement(311)),
                        m(GasBoiler.ChannelId.ERROR_BIT_112, new CoilElement(312)),
                        m(GasBoiler.ChannelId.ERROR_BIT_113, new CoilElement(313)),
                        m(GasBoiler.ChannelId.ERROR_BIT_114, new CoilElement(314)),
                        m(GasBoiler.ChannelId.ERROR_BIT_115, new CoilElement(315)),
                        m(GasBoiler.ChannelId.ERROR_BIT_116, new CoilElement(316)),
                        m(GasBoiler.ChannelId.ERROR_BIT_117, new CoilElement(317)),
                        m(GasBoiler.ChannelId.ERROR_BIT_118, new CoilElement(318)),
                        m(GasBoiler.ChannelId.ERROR_BIT_119, new CoilElement(319)),
                        m(GasBoiler.ChannelId.ERROR_BIT_120, new CoilElement(320)),
                        m(GasBoiler.ChannelId.ERROR_BIT_121, new CoilElement(321)),
                        m(GasBoiler.ChannelId.ERROR_BIT_122, new CoilElement(322)),
                        m(GasBoiler.ChannelId.ERROR_BIT_123, new CoilElement(323)),
                        m(GasBoiler.ChannelId.ERROR_BIT_124, new CoilElement(324)),
                        m(GasBoiler.ChannelId.ERROR_BIT_125, new CoilElement(325)),
                        m(GasBoiler.ChannelId.ERROR_BIT_126, new CoilElement(326)),
                        m(GasBoiler.ChannelId.ERROR_BIT_127, new CoilElement(327)),
                        m(GasBoiler.ChannelId.ERROR_BIT_128, new CoilElement(328)),
                        m(GasBoiler.ChannelId.ERROR_BIT_129, new CoilElement(329)),
                        m(GasBoiler.ChannelId.ERROR_BIT_130, new CoilElement(330)),
                        m(GasBoiler.ChannelId.ERROR_BIT_131, new CoilElement(331)),
                        m(GasBoiler.ChannelId.ERROR_BIT_132, new CoilElement(332)),
                        m(GasBoiler.ChannelId.ERROR_BIT_133, new CoilElement(333)),
                        m(GasBoiler.ChannelId.ERROR_BIT_134, new CoilElement(334)),
                        m(GasBoiler.ChannelId.ERROR_BIT_135, new CoilElement(335)),
                        m(GasBoiler.ChannelId.ERROR_BIT_136, new CoilElement(336)),
                        m(GasBoiler.ChannelId.ERROR_BIT_137, new CoilElement(337)),
                        m(GasBoiler.ChannelId.ERROR_BIT_138, new CoilElement(338)),
                        m(GasBoiler.ChannelId.ERROR_BIT_139, new CoilElement(339)),
                        m(GasBoiler.ChannelId.ERROR_BIT_140, new CoilElement(340)),
                        m(GasBoiler.ChannelId.ERROR_BIT_141, new CoilElement(341)),
                        m(GasBoiler.ChannelId.ERROR_BIT_142, new CoilElement(342)),
                        m(GasBoiler.ChannelId.ERROR_BIT_143, new CoilElement(343)),
                        m(GasBoiler.ChannelId.ERROR_BIT_144, new CoilElement(344)),
                        m(GasBoiler.ChannelId.ERROR_BIT_145, new CoilElement(345)),
                        m(GasBoiler.ChannelId.ERROR_BIT_146, new CoilElement(346)),
                        m(GasBoiler.ChannelId.ERROR_BIT_147, new CoilElement(347)),
                        m(GasBoiler.ChannelId.ERROR_BIT_148, new CoilElement(348)),
                        m(GasBoiler.ChannelId.ERROR_BIT_149, new CoilElement(349)),
                        m(GasBoiler.ChannelId.ERROR_BIT_150, new CoilElement(350)),
                        m(GasBoiler.ChannelId.ERROR_BIT_151, new CoilElement(351)),
                        m(GasBoiler.ChannelId.ERROR_BIT_152, new CoilElement(352)),
                        m(GasBoiler.ChannelId.ERROR_BIT_153, new CoilElement(353)),
                        m(GasBoiler.ChannelId.ERROR_BIT_154, new CoilElement(354)),
                        m(GasBoiler.ChannelId.ERROR_BIT_155, new CoilElement(355)),
                        m(GasBoiler.ChannelId.ERROR_BIT_156, new CoilElement(356)),
                        m(GasBoiler.ChannelId.ERROR_BIT_157, new CoilElement(357)),
                        m(GasBoiler.ChannelId.ERROR_BIT_158, new CoilElement(358)),
                        m(GasBoiler.ChannelId.ERROR_BIT_159, new CoilElement(359)),
                        m(GasBoiler.ChannelId.ERROR_BIT_160, new CoilElement(360)),
                        m(GasBoiler.ChannelId.ERROR_BIT_161, new CoilElement(361)),
                        m(GasBoiler.ChannelId.ERROR_BIT_162, new CoilElement(362)),
                        m(GasBoiler.ChannelId.ERROR_BIT_163, new CoilElement(363)),
                        m(GasBoiler.ChannelId.ERROR_BIT_164, new CoilElement(364)),
                        m(GasBoiler.ChannelId.ERROR_BIT_165, new CoilElement(365)),
                        m(GasBoiler.ChannelId.ERROR_BIT_166, new CoilElement(366)),
                        m(GasBoiler.ChannelId.ERROR_BIT_167, new CoilElement(367)),
                        m(GasBoiler.ChannelId.ERROR_BIT_168, new CoilElement(368)),
                        m(GasBoiler.ChannelId.ERROR_BIT_169, new CoilElement(369)),
                        m(GasBoiler.ChannelId.ERROR_BIT_170, new CoilElement(370)),
                        m(GasBoiler.ChannelId.ERROR_BIT_171, new CoilElement(371)),
                        m(GasBoiler.ChannelId.ERROR_BIT_172, new CoilElement(372)),
                        m(GasBoiler.ChannelId.ERROR_BIT_173, new CoilElement(373)),
                        m(GasBoiler.ChannelId.ERROR_BIT_174, new CoilElement(374)),
                        m(GasBoiler.ChannelId.ERROR_BIT_175, new CoilElement(375)),
                        m(GasBoiler.ChannelId.ERROR_BIT_176, new CoilElement(376)),
                        m(GasBoiler.ChannelId.ERROR_BIT_177, new CoilElement(377)),
                        m(GasBoiler.ChannelId.ERROR_BIT_178, new CoilElement(378)),
                        m(GasBoiler.ChannelId.ERROR_BIT_179, new CoilElement(379)),
                        m(GasBoiler.ChannelId.ERROR_BIT_180, new CoilElement(380)),
                        m(GasBoiler.ChannelId.ERROR_BIT_181, new CoilElement(381)),
                        m(GasBoiler.ChannelId.ERROR_BIT_182, new CoilElement(382)),
                        m(GasBoiler.ChannelId.ERROR_BIT_183, new CoilElement(383)),
                        m(GasBoiler.ChannelId.ERROR_BIT_184, new CoilElement(384)),
                        m(GasBoiler.ChannelId.ERROR_BIT_185, new CoilElement(385)),
                        m(GasBoiler.ChannelId.ERROR_BIT_186, new CoilElement(386)),
                        m(GasBoiler.ChannelId.ERROR_BIT_187, new CoilElement(387)),
                        m(GasBoiler.ChannelId.ERROR_BIT_188, new CoilElement(388)),
                        m(GasBoiler.ChannelId.ERROR_BIT_189, new CoilElement(389)),
                        m(GasBoiler.ChannelId.ERROR_BIT_190, new CoilElement(390)),
                        m(GasBoiler.ChannelId.ERROR_BIT_191, new CoilElement(391)),
                        m(GasBoiler.ChannelId.ERROR_BIT_192, new CoilElement(392)),
                        m(GasBoiler.ChannelId.ERROR_BIT_193, new CoilElement(393)),
                        m(GasBoiler.ChannelId.ERROR_BIT_194, new CoilElement(394)),
                        m(GasBoiler.ChannelId.ERROR_BIT_195, new CoilElement(395)),
                        m(GasBoiler.ChannelId.ERROR_BIT_196, new CoilElement(396)),
                        m(GasBoiler.ChannelId.ERROR_BIT_197, new CoilElement(397)),
                        m(GasBoiler.ChannelId.ERROR_BIT_198, new CoilElement(398)),
                        m(GasBoiler.ChannelId.ERROR_BIT_199, new CoilElement(399)),
                        m(GasBoiler.ChannelId.ERROR_BIT_200, new CoilElement(400)),
                        m(GasBoiler.ChannelId.ERROR_BIT_201, new CoilElement(401)),
                        m(GasBoiler.ChannelId.ERROR_BIT_202, new CoilElement(402)),
                        m(GasBoiler.ChannelId.ERROR_BIT_203, new CoilElement(403)),
                        m(GasBoiler.ChannelId.ERROR_BIT_204, new CoilElement(404)),
                        m(GasBoiler.ChannelId.ERROR_BIT_205, new CoilElement(405)),
                        m(GasBoiler.ChannelId.ERROR_BIT_206, new CoilElement(406)),
                        m(GasBoiler.ChannelId.ERROR_BIT_207, new CoilElement(407)),
                        m(GasBoiler.ChannelId.ERROR_BIT_208, new CoilElement(408)),
                        m(GasBoiler.ChannelId.ERROR_BIT_209, new CoilElement(409)),
                        m(GasBoiler.ChannelId.ERROR_BIT_210, new CoilElement(410)),
                        m(GasBoiler.ChannelId.ERROR_BIT_211, new CoilElement(411)),
                        m(GasBoiler.ChannelId.ERROR_BIT_212, new CoilElement(412)),
                        m(GasBoiler.ChannelId.ERROR_BIT_213, new CoilElement(413)),
                        m(GasBoiler.ChannelId.ERROR_BIT_214, new CoilElement(414)),
                        m(GasBoiler.ChannelId.ERROR_BIT_215, new CoilElement(415)),
                        m(GasBoiler.ChannelId.ERROR_BIT_216, new CoilElement(416)),
                        m(GasBoiler.ChannelId.ERROR_BIT_217, new CoilElement(417)),
                        m(GasBoiler.ChannelId.ERROR_BIT_218, new CoilElement(418)),
                        m(GasBoiler.ChannelId.ERROR_BIT_219, new CoilElement(419)),
                        m(GasBoiler.ChannelId.ERROR_BIT_220, new CoilElement(420)),
                        m(GasBoiler.ChannelId.ERROR_BIT_221, new CoilElement(421)),
                        m(GasBoiler.ChannelId.ERROR_BIT_222, new CoilElement(422)),
                        m(GasBoiler.ChannelId.ERROR_BIT_223, new CoilElement(423)),
                        m(GasBoiler.ChannelId.ERROR_BIT_224, new CoilElement(424)),
                        m(GasBoiler.ChannelId.ERROR_BIT_225, new CoilElement(425)),
                        m(GasBoiler.ChannelId.ERROR_BIT_226, new CoilElement(426)),
                        m(GasBoiler.ChannelId.ERROR_BIT_227, new CoilElement(427)),
                        m(GasBoiler.ChannelId.ERROR_BIT_228, new CoilElement(428)),
                        m(GasBoiler.ChannelId.ERROR_BIT_229, new CoilElement(429)),
                        m(GasBoiler.ChannelId.ERROR_BIT_230, new CoilElement(430)),
                        m(GasBoiler.ChannelId.ERROR_BIT_231, new CoilElement(431)),
                        m(GasBoiler.ChannelId.ERROR_BIT_232, new CoilElement(432)),
                        m(GasBoiler.ChannelId.ERROR_BIT_233, new CoilElement(433)),
                        m(GasBoiler.ChannelId.ERROR_BIT_234, new CoilElement(434)),
                        m(GasBoiler.ChannelId.ERROR_BIT_235, new CoilElement(435)),
                        m(GasBoiler.ChannelId.ERROR_BIT_236, new CoilElement(436)),
                        m(GasBoiler.ChannelId.ERROR_BIT_237, new CoilElement(437)),
                        m(GasBoiler.ChannelId.ERROR_BIT_238, new CoilElement(438)),
                        m(GasBoiler.ChannelId.ERROR_BIT_239, new CoilElement(439)),
                        m(GasBoiler.ChannelId.ERROR_BIT_240, new CoilElement(440)),
                        m(GasBoiler.ChannelId.ERROR_BIT_241, new CoilElement(441)),
                        m(GasBoiler.ChannelId.ERROR_BIT_242, new CoilElement(442)),
                        m(GasBoiler.ChannelId.ERROR_BIT_243, new CoilElement(443)),
                        m(GasBoiler.ChannelId.ERROR_BIT_244, new CoilElement(444)),
                        m(GasBoiler.ChannelId.ERROR_BIT_245, new CoilElement(445)),
                        m(GasBoiler.ChannelId.ERROR_BIT_246, new CoilElement(446)),
                        m(GasBoiler.ChannelId.ERROR_BIT_247, new CoilElement(447)),
                        m(GasBoiler.ChannelId.ERROR_BIT_248, new CoilElement(448)),
                        m(GasBoiler.ChannelId.ERROR_BIT_249, new CoilElement(449)),
                        m(GasBoiler.ChannelId.ERROR_BIT_250, new CoilElement(450)),
                        m(GasBoiler.ChannelId.ERROR_BIT_251, new CoilElement(451)),
                        m(GasBoiler.ChannelId.ERROR_BIT_252, new CoilElement(452)),
                        m(GasBoiler.ChannelId.ERROR_BIT_253, new CoilElement(453)),
                        m(GasBoiler.ChannelId.ERROR_BIT_255, new CoilElement(455))
                ),

                // read holding registers
                new FC3ReadRegistersTask(5, Priority.LOW,
                        m(GasBoiler.ChannelId.DEVICE_OPERATION_MODE, new UnsignedWordElement(5)),
                        new DummyRegisterElement(6, 9),
                        m(GasBoiler.ChannelId.DEVICE_POWER_MODE, new UnsignedWordElement(10)),
                        m(Heater.ChannelId.SET_POINT_HEATING_POWER_PERCENT, new UnsignedWordElement(11), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(12, 12),
                        m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(14, 16),
                        m(GasBoiler.ChannelId.HC1_OPERATION_MODE, new UnsignedWordElement(17)),
                        new DummyRegisterElement(18, 19),
                        m(GasBoiler.ChannelId.HC2_OPERATION_MODE, new UnsignedWordElement(20)),
                        new DummyRegisterElement(21, 22),
                        m(GasBoiler.ChannelId.HC3_OPERATION_MODE, new UnsignedWordElement(23))
                ),

                // read input registers
                new FC4ReadInputRegistersTask(6, Priority.LOW,
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new UnsignedWordElement(6), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(7, 7),
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(8), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(9, 30),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(31)),
                        new DummyRegisterElement(32, 35),
                        m(GasBoiler.ChannelId.OPERATING_HOURS_TIER1, new UnsignedWordElement(36)),
                        m(GasBoiler.ChannelId.OPERATING_HOURS_TIER2, new UnsignedWordElement(37)),
                        m(GasBoiler.ChannelId.BOILER_STARTS, new UnsignedWordElement(38)),
                        m(GasBoiler.ChannelId.BOILER_STATE, new UnsignedWordElement(39))
                )
        );
        if (this.readOnly == false) {
            protocol.addTasks(
                    // Write to holding registers
                    new FC6WriteRegisterTask(5,
                            m(GasBoiler.ChannelId.DEVICE_OPERATION_MODE, new UnsignedWordElement(5))),
                    new FC6WriteRegisterTask(10,
                            m(GasBoiler.ChannelId.DEVICE_POWER_MODE, new UnsignedWordElement(10))),
                    new FC6WriteRegisterTask(11,
                            m(Heater.ChannelId.SET_POINT_HEATING_POWER_PERCENT, new UnsignedWordElement(11), ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
                    new FC6WriteRegisterTask(13,
                            m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
                    new FC6WriteRegisterTask(17,
                            m(GasBoiler.ChannelId.HC1_OPERATION_MODE, new UnsignedWordElement(17))),
                    new FC6WriteRegisterTask(20,
                            m(GasBoiler.ChannelId.HC2_OPERATION_MODE, new UnsignedWordElement(20))),
                    new FC6WriteRegisterTask(23,
                            m(GasBoiler.ChannelId.HC3_OPERATION_MODE, new UnsignedWordElement(23)))
            );
        }
        return protocol;
    }


/*    @Override
    public String debugLog() {
        String out = "";
        System.out.println("--------------" + super.id() + "--------------");
        List<Channel<?>> all = new ArrayList<>();
        Arrays.stream(GasBoilerData.ChannelId.values()).forEach(consumer -> {
            all.add(this.channel(consumer));
        });
        all.forEach(consumer -> System.out.println(consumer.channelId().id() + " value: " + (consumer.value().isDefined() ? consumer.value().get() : "UNDEFINED ") + (consumer.channelDoc().getUnit().getSymbol())));
        //TODO: Error/Warning status etc
        System.out.println("----------------------------------");
        return "ok";
    }
*/

    private List<String> generateErrorList() {
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < 255; i++) {
            if (this.getError(i + 1).getNextValue().isDefined()) {
                if (this.getError(i + 1).getNextValue().get()) {
                    errorList.add(this.errorList[i]);
                }
            }
        }
        return errorList;
    }

    @Override
    public void handleEvent(Event event) {
        if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            this.channelmapping();
        }
    }

    protected void channelmapping() {

        // Parse errors.
        List<String> errorList = this.generateErrorList();
        if ((errorList.size() > 0)) {
            this._setErrorMessage(errorList.toString());
        } else {
            this._setErrorMessage("No error");
        }

        // Parse state.
        if (getBoilerState().value().isDefined()) {
            int boilerState = getBoilerState().value().get();
            if (boilerState > 0) {
                this._setHeaterState(HeaterState.HEATING.getValue());
            } else {
                this._setHeaterState(HeaterState.STANDBY.getValue());
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
        }

        if (this.readOnly == false) {
            // Handle EnableSignal.
            boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
            int exceptionalStateValue = 0;
            boolean exceptionalStateActive = false;
            if (this.useExceptionalState) {
                exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (exceptionalStateActive) {
                    exceptionalStateValue = this.getExceptionalStateValue();
                    if (exceptionalStateValue <= 0) {
                        // Turn off Chp when ExceptionalStateValue = 0.
                        turnOnHeater = false;
                    } else {
                        // When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
                        turnOnHeater = true;
                        if (exceptionalStateValue > 100) {
                            exceptionalStateValue = 100;
                        } else if (exceptionalStateValue < 0) {
                            exceptionalStateValue = 0;
                        }
                        try {
                            this.setHeatingPowerPercentSetpoint(exceptionalStateValue);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't write in Channel " + e.getMessage());
                        }
                    }
                }
            }

            // Switch heater on or off.
            if (turnOnHeater) {
                try {
                    this.getDevicePowerMode().setNextWriteValue(1);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            } else {
                try {
                    this.getDevicePowerMode().setNextWriteValue(0);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
        }

        if (this.printInfoToLog) {
            this.logInfo(this.log, "--Gasboiler Viessmann Vitotronic 100--");
            this.logInfo(this.log, "Power percent set point (write mode only): " + this.getHeatingPowerPercentSetpoint());
            this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
            this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
            this.logInfo(this.log, "Operation mode: " + this.getDeviceOperationMode().value());
            this.logInfo(this.log, "Operating hours tier1: " + this.getOperatingHoursTier1().value());
            this.logInfo(this.log, "Operating hours tier2: " + this.getOperatingHoursTier2().value());
            this.logInfo(this.log, "Boiler start counter: " + this.getBoilerStarts().value());
            this.logInfo(this.log, "Heater state: " + this.getHeaterState());
            this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
        }
    }
}
