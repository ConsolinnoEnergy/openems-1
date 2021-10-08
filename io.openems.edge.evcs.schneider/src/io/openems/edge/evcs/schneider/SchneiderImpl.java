package io.openems.edge.evcs.schneider;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.schneider.api.Schneider;
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

import java.util.Arrays;


@Designate(ocd = Config.class, factory = true)
@Component(name = "SchneiderImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
/**
 * This Provides the Schneider EVCS Modbus TCP implementation.
 */
public class SchneiderImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, Schneider, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    private int minPower;
    private int maxPower;
    private int[] phases;
    private SchneiderWriteHandler writeHandler;
    private SchneiderReadHandler readHandler;

    private EvcsPower evcsPower;

    public SchneiderImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                Schneider.ChannelId.values());
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        this.minPower = config.minCurrent();
        this.maxPower = config.maxCurrent();
        this.phases = config.phases();
        if (!this.checkPhases()) {
            throw new ConfigurationException("Phase Configuration is not valid!", "Configuration must only contain 1,2 and 3.");
        }
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this._setMinimumHardwarePower(8 * 230);
        this._setIsPriority(config.priority());
        this._setMaximumPower(this.maxPower);
        this._setMinimumPower(this.minPower);
        this.readHandler = new SchneiderReadHandler(this);
        this.writeHandler = new SchneiderWriteHandler(this);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                //------------------Read_Only Register-------------------\\
                new FC3ReadRegistersTask(6, Priority.HIGH,
                        m(Schneider.ChannelId.CPW_STATE,
                                new SignedWordElement(6),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(9, Priority.HIGH,
                        m(Schneider.ChannelId.LAST_CHARGE_STATUS,
                                new SignedWordElement(9),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(20, Priority.HIGH,
                        m(Schneider.ChannelId.REMOTE_COMMAND_STATUS,
                                new SignedWordElement(20),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(23, Priority.HIGH,
                        m(Schneider.ChannelId.ERROR_STATUS_MSB,
                                new SignedWordElement(23),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(24, Priority.HIGH,
                        m(Schneider.ChannelId.ERROR_STATUS_LSB,
                                new SignedWordElement(24),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(30, Priority.HIGH,
                        m(Schneider.ChannelId.CHARGE_TIME,
                                new SignedWordElement(30),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(31, Priority.HIGH,
                        m(Schneider.ChannelId.CHARGE_TIME_2,
                                new SignedWordElement(31),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(301, Priority.HIGH,
                        m(Schneider.ChannelId.MAX_INTENSITY_SOCKET,
                                new UnsignedWordElement(301),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(350 + ((this.phases[0] * 2) - 2), Priority.HIGH,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_X_READ,
                                new FloatDoublewordElement(350 + ((this.phases[0] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(350 + ((this.phases[1] * 2) - 2), Priority.HIGH,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_2_READ,
                                new FloatDoublewordElement(350 + ((this.phases[1] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(350 + ((this.phases[2] * 2) - 2), Priority.HIGH,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_3_READ,
                                new FloatDoublewordElement(350 + ((this.phases[2] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(356, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_ENERGY_MSB_READ,
                                new SignedWordElement(356),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(357, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_ENERGY_LSB_READ,
                                new SignedWordElement(357),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(358, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_POWER_TOTAL_READ,
                                new FloatDoublewordElement(358),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(360, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L1_L2_VOLTAGE,
                                new FloatDoublewordElement(360),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(362, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L2_L3_VOLTAGE,
                                new FloatDoublewordElement(362),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(364, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L3_L1_VOLTAGE,
                                new FloatDoublewordElement(364),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(366, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L1_N_VOLTAGE,
                                new FloatDoublewordElement(366),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(368, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L2_N_VOLTAGE,
                                new FloatDoublewordElement(368),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(370, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L3_N_VOLTAGE,
                                new FloatDoublewordElement(370),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(933, Priority.HIGH,
                        m(Schneider.ChannelId.DEGRADED_MODE,
                                new UnsignedWordElement(933),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(2004, Priority.HIGH,
                        m(Schneider.ChannelId.SESSION_TIME,
                                new SignedWordElement(2004),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(2005, Priority.HIGH,
                        m(Schneider.ChannelId.SESSION_TIME_2,
                                new SignedWordElement(2005),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                //---------------------Write Register---------------------\\
                new FC6WriteRegisterTask(150,
                        m(Schneider.ChannelId.REMOTE_COMMAND,
                                new SignedWordElement(150),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(301,
                        m(Schneider.ChannelId.MAX_INTENSITY_SOCKET,
                                new UnsignedWordElement(301),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(932,
                        m(Schneider.ChannelId.REMOTE_CONTROLLER_LIFE_BIT,
                                new SignedWordElement(932),
                                ElementToChannelConverter.DIRECT_1_TO_1))
        );
    }

    /**
     * Checks if the Phase Configuration of the Config is valid.
     *
     * @return true if valid
     */
    private boolean checkPhases() {
        String phases = Arrays.toString(this.phases);
        return phases.contains("1") && phases.contains("2") && phases.contains("3") && this.phases.length == 3;
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getStationPowerTotal() + " W | L1 " + this.getStationIntensityPhaseX() + " A | L2 " + this.getStationIntensityPhase2() + " A | L3 " + this.getStationIntensityPhase3() + " A";
    }


    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }

    @Override
    public void handleEvent(Event event) {
        try {
            if (this.getRemoteControllerLifeBitChannel().getNextValue().isDefined() && this.getRemoteControllerLifeBitChannel().getNextValue().get() == 1) {
                this._setChargingstationCommunicationFailed(true);
            }
            this.setRemoteControllerLifeBit(1);
        } catch (OpenemsError.OpenemsNamedException e) {
            this._setChargingstationCommunicationFailed(true);
        }
        this.writeHandler.run();
        try {
            this.readHandler.run();
        } catch (Throwable throwable) {

        }
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower
     */
    public int getMinPower() {
        return this.minPower;
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower
     */
    public int getMaxPower() {
        return this.maxPower;
    }
}
