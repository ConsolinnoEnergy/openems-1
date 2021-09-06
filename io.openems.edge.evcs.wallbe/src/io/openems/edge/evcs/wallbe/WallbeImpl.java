package io.openems.edge.evcs.wallbe;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.wallbe.api.Wallbe;
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
@Component(name = "WallbeImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class WallbeImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, Wallbe, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private int minPower;
    private int maxPower;
    private int[] phases;
    private WallbeWriteHandler writeHandler;
    private WallbeReadHandler readHandler;
    private EvcsPower evcsPower;

    public WallbeImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                Wallbe.ChannelId.values());
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
        this.readHandler = new WallbeReadHandler(this);
        this.writeHandler = new WallbeWriteHandler(this);
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
                new FC4ReadInputRegistersTask(100, Priority.HIGH,
                        m(Wallbe.ChannelId.WALLBE_STATUS,
                                new StringWordElement(100, 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(102, Priority.HIGH,
                        m(Wallbe.ChannelId.LOAD_TIME,
                                new UnsignedDoublewordElement(102),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(104, Priority.HIGH,
                        m(Wallbe.ChannelId.DIP_SWITCHES,
                                new UnsignedWordElement(104),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(105, Priority.HIGH,
                        m(Wallbe.ChannelId.FIRMWARE_VERSION,
                                new UnsignedDoublewordElement(105),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(114, Priority.HIGH,
                        m(Wallbe.ChannelId.CURRENT_L1,
                                new SignedDoublewordElement(114),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(116, Priority.HIGH,
                        m(Wallbe.ChannelId.CURRENT_L2,
                                new SignedDoublewordElement(116),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(118, Priority.HIGH,
                        m(Wallbe.ChannelId.CURRENT_L3,
                                new SignedDoublewordElement(118),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(120, Priority.HIGH,
                        m(Wallbe.ChannelId.APPARENT_POWER,
                                new SignedDoublewordElement(120),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(132, Priority.HIGH,
                        m(Wallbe.ChannelId.ENERGY,
                                new UnsignedDoublewordElement(132),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(528,
                        m(Wallbe.ChannelId.MAXIMUM_CHARGE_CURRENT,
                                new SignedWordElement(528),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC5WriteCoilTask(400,
                        (ModbusCoilElement) m(Wallbe.ChannelId.CHARGE_ENABLE,
                                new CoilElement(400),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        );
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getChargePower().get() + " W | L1 " + this.getCurrentL1() + " A | L2 " + this.getCurrentL2() + " A | L3 " + this.getCurrentL3() + " A";
    }

    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    @Override
    public void handleEvent(Event event) {
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
