package io.openems.edge.evcs.compleo;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.compleo.api.Compleo;
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
@Component(name = "CompleoImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class CompleoImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, Compleo, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private int minPower;
    private int maxPower;
    private int[] phases;
    private CompleoWriteHandler writeHandler;
    private CompleoReadHandler readHandler;
    private EvcsPower evcsPower;

    public CompleoImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                Compleo.ChannelId.values());
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
        this._setMinimumHardwarePower(6 * GridVoltage.V_230_HZ_50.getValue());
        this._setMaximumPower(this.maxPower);
        this._setMaximumHardwarePower(32 * GridVoltage.V_230_HZ_50.getValue());
        this._setMinimumPower(this.minPower);
        this._setPowerPrecision(GridVoltage.V_230_HZ_50.getValue());
        this._setIsPriority(config.priority());
        this.readHandler = new CompleoReadHandler(this);
        this.writeHandler = new CompleoWriteHandler(this);
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
    public String debugLog() {
        return "Total: " + this.getChargePower().get() + " W | L1 " + this.getCurrentL1() + " A | L2 " + this.getCurrentL2() + " A | L3 " + this.getCurrentL3() + " A";
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(0, Priority.HIGH,
                        m(Compleo.ChannelId.MAX_POWER,
                                new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1, Priority.HIGH,
                        m(Compleo.ChannelId.STATUS,
                                new UnsignedWordElement(1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(2, Priority.HIGH,
                        m(Compleo.ChannelId.POWER,
                                new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(3, Priority.HIGH,
                        m(Compleo.ChannelId.CURRENT_L1,
                                new SignedWordElement(3),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(4, Priority.HIGH,
                        m(Compleo.ChannelId.CURRENT_L2,
                                new SignedWordElement(4),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(5, Priority.HIGH,
                        m(Compleo.ChannelId.CURRENT_L3,
                                new SignedWordElement(5),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(8, Priority.HIGH,
                        m(Compleo.ChannelId.ENERGY_SESSION,
                                new SignedWordElement(8),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(0,
                        m(Compleo.ChannelId.MAX_POWER,
                                new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        );

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
