package io.openems.edge.evcs.schneider;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;


@Designate(ocd = Config.class, factory = true)
@Component(name = "SchneiderImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class SchneiderImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, Schneider, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    private int minPower;
    private int maxPower;
    private int[] phases;
    private SchneiderReadWorker readWorker;
    private SchneiderWriteHandler writeHandler;


    public SchneiderImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                Schneider.ChannelId.values());
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
        this._setMaximumPower(this.maxPower);
        this._setMinimumPower(this.minPower);
        this.readWorker = new SchneiderReadWorker(this);
        this.writeHandler = new SchneiderWriteHandler(this);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this, new FC6WriteRegisterTask(6,
                m(Schneider.ChannelId.CPW_STATE,
                        new SignedWordElement(6),
                        ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(9,
                        m(Schneider.ChannelId.LAST_CHARGE_STATUS,
                                new SignedWordElement(9),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(20,
                        m(Schneider.ChannelId.REMOTE_COMMAND_STATUS,
                                new SignedWordElement(20),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(23,
                        m(Schneider.ChannelId.ERROR_STATUS_MSB,
                                new SignedWordElement(23),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(24,
                        m(Schneider.ChannelId.ERROR_STATUS_LSB,
                                new SignedWordElement(24),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(30,
                        m(Schneider.ChannelId.CHARGE_TIME,
                                new SignedWordElement(30),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(31,
                        m(Schneider.ChannelId.CHARGE_TIME_2,
                                new SignedWordElement(31),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(301,
                        m(Schneider.ChannelId.MAX_INTENSITY_SOCKET,
                                new UnsignedWordElement(301),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(350,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_X_READ,
                                new SignedDoublewordElement(350),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(352,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_2_READ,
                                new SignedDoublewordElement(352),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(354,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_3_READ,
                                new SignedDoublewordElement(354),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(356,
                        m(Schneider.ChannelId.STATION_ENERGY_MSB_READ,
                                new SignedWordElement(356),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(357,
                        m(Schneider.ChannelId.STATION_ENERGY_LSB_READ,
                                new SignedWordElement(357),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(358,
                        m(Schneider.ChannelId.STATION_POWER_TOTAL_READ,
                                new SignedDoublewordElement(358),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(933,
                        m(Schneider.ChannelId.DEGRADED_MODE,
                                new UnsignedWordElement(933),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(2004,
                        m(Schneider.ChannelId.SESSION_TIME,
                                new SignedWordElement(2004),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(2005,
                        m(Schneider.ChannelId.SESSION_TIME_2,
                                new SignedWordElement(2005),
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
        return "";
    }


    @Override
    public EvcsPower getEvcsPower() {
        return null;
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }

    @Override
    public void handleEvent(Event event) {
        this.writeHandler.run();

    }

    public int getMinPower() {
        return this.minPower;
    }
    public int getMaxPower() {
        return this.maxPower;
    }
}
