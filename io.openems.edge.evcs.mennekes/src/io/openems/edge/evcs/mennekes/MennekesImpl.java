package io.openems.edge.evcs.mennekes;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
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
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.mennekes.api.Mennekes;
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

/**
 * This Provides the Mennekes EVCS Modbus TCP implementation.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "MennekesImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class MennekesImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, Mennekes, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private int minPower;
    private int maxPower;
    private int[] phases;
    private MennekesWriteHandler writeHandler;
    private MennekesReadHandler readHandler;
    private EvcsPower evcsPower;

    public MennekesImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                Mennekes.ChannelId.values());
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
        this._setMinimumHardwarePower(6 * 230);
        this._setMaximumPower(this.maxPower);
        this._setMaximumHardwarePower(32 * 230);
        this._setMinimumPower(this.minPower);
        this._setPowerPrecision(1 * 230);
        this._setIsPriority(config.priority());
        this.readHandler = new MennekesReadHandler(this);
        this.writeHandler = new MennekesWriteHandler(this);
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
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

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                //--------------------Read-Register-Task---------------------\\
                new FC3ReadRegistersTask(100, Priority.HIGH,
                        m(Mennekes.ChannelId.FIRMWARE_VERSION,
                                new UnsignedDoublewordElement(100),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(105, Priority.HIGH,
                        m(Mennekes.ChannelId.ERROR_CODE_1,
                                new UnsignedDoublewordElement(105),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(107, Priority.HIGH,
                        m(Mennekes.ChannelId.ERROR_CODE_2,
                                new UnsignedDoublewordElement(107),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(109, Priority.HIGH,
                        m(Mennekes.ChannelId.ERROR_CODE_3,
                                new UnsignedDoublewordElement(109),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(111, Priority.HIGH,
                        m(Mennekes.ChannelId.ERROR_CODE_4,
                                new UnsignedDoublewordElement(111),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(120, Priority.HIGH,
                        m(Mennekes.ChannelId.PROTOCOL_VERSION,
                                new FloatDoublewordElement(120),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(122, Priority.HIGH,
                        m(Mennekes.ChannelId.VEHICLE_STATE,
                                new SignedWordElement(122),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(124, Priority.HIGH,
                        m(Mennekes.ChannelId.CP_AVAILABILITY,
                                new SignedWordElement(124),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(131, Priority.HIGH,
                        m(Mennekes.ChannelId.SAFE_CURRENT,
                                new UnsignedWordElement(131),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(132, Priority.HIGH,
                        m(Mennekes.ChannelId.COMM_TIMEOUT,
                                new UnsignedWordElement(132),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(200, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_ENERGY_L1,
                                new FloatDoublewordElement(200),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(202, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_ENERGY_L2,
                                new FloatDoublewordElement(202),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(204, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_ENERGY_L3,
                                new FloatDoublewordElement(204),
                                ElementToChannelConverter.DIRECT_1_TO_1)),


                new FC3ReadRegistersTask(206, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_POWER_L1,
                                new UnsignedDoublewordElement(206),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(208, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_POWER_L2,
                                new UnsignedDoublewordElement(208),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(210, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_POWER_L3,
                                new UnsignedDoublewordElement(210),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(212, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_CURRENT_L1,
                                new UnsignedDoublewordElement(212),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(214, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_CURRENT_L2,
                                new UnsignedDoublewordElement(214),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(216, Priority.HIGH,
                        m(Mennekes.ChannelId.METER_CURRENT_L3,
                                new UnsignedDoublewordElement(216),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(701, Priority.HIGH,
                        m(Mennekes.ChannelId.SCHEDULED_DEPARTURE_TIME,
                                new UnsignedDoublewordElement(701),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(703, Priority.HIGH,
                        m(Mennekes.ChannelId.SCHEDULED_DEPARTURE_DATE,
                                new UnsignedDoublewordElement(703),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(705, Priority.HIGH,
                        m(Mennekes.ChannelId.CHARGED_ENERGY,
                                new UnsignedWordElement(705),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(706, Priority.HIGH,
                        m(Mennekes.ChannelId.SIGNALED_CURRENT,
                                new UnsignedWordElement(706),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(707, Priority.HIGH,
                        m(Mennekes.ChannelId.START_TIME,
                                new UnsignedDoublewordElement(707),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(709, Priority.HIGH,
                        m(Mennekes.ChannelId.CHARGE_DURATION,
                                new UnsignedWordElement(709),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(710, Priority.HIGH,
                        m(Mennekes.ChannelId.END_TIME,
                                new UnsignedDoublewordElement(710),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(1000, Priority.HIGH,
                        m(Mennekes.ChannelId.CURRENT_LIMIT,
                                new UnsignedWordElement(1000),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                //--------------------Write-Register-Task---------------------\\

                new FC6WriteRegisterTask(131,
                        m(Mennekes.ChannelId.SAFE_CURRENT,
                                new UnsignedWordElement(131),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(132,
                        m(Mennekes.ChannelId.COMM_TIMEOUT,
                                new UnsignedWordElement(132),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(1000,
                        m(Mennekes.ChannelId.CURRENT_LIMIT,
                                new UnsignedWordElement(1000),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        );
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getChargePower().get() + " W | L1 " + this.getPowerL1() / 230 + " A | L2 " + this.getPowerL2() / 230 + " A | L3 " + this.getPowerL3() / 230 + " A";
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
        this.writeHandler.run();
        try {
            this.readHandler.run();
        } catch (Throwable throwable) {
            //
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
