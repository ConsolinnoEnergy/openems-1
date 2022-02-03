package io.openems.edge.sensor.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.sensor.signal.api.SignalSensor;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "ModbusSignalSensor", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property =
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class ModbusSignalSensor extends AbstractOpenemsModbusComponent implements OpenemsComponent, SignalSensor, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;
    private Config config;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Has an Error occurred.
         *
         * <ul>
         * <li>Interface: SignalSensor
         * <li>Type: Boolean
         * </ul>
         */
        SIGNAL_ACTIVE_READ_MODBUS(Doc.of(OpenemsType.BOOLEAN));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    private Channel<Boolean> getSignalActiveModbusChannel() {
        return this.channel(ChannelId.SIGNAL_ACTIVE_READ_MODBUS);
    }


    public ModbusSignalSensor() {
        super(OpenemsComponent.ChannelId.values(),
                SignalSensor.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this.getSignalType().setNextValue(this.config.signalType().name());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC1ReadCoilsTask(this.config.address(), Priority.HIGH,
                        m(ChannelId.SIGNAL_ACTIVE_READ_MODBUS, new CoilElement(this.config.address()))));

    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.getSignalActiveModbusChannel().value().isDefined()) {
                boolean signalActive = this.config.inverted() != this.getSignalActiveModbusChannel().value().get();
                this.signalActive().setNextValue(signalActive);
            }
        }
    }
}