package io.openems.edge.kbr4f96;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadrupleWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.kbr4f96.api.KbrBaseModule;
import org.osgi.service.cm.ConfigurationAdmin;
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
 * This represents the Common Components of the KBR4F96.
 * NOTE: The Device itself is one coherent Unit. The split in Main and L Unit is simply to avoid code duplication.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.kbr4f96", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class KbrBaseModuleImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, KbrBaseModule, EventHandler {
    private final Logger log = LoggerFactory.getLogger(KbrBaseModuleImpl.class);

    private Boolean l1 = false;
    private Boolean l2 = false;
    private Boolean l3 = false;

    public KbrBaseModuleImpl() {
        super(OpenemsComponent.ChannelId.values(), KbrBaseModule.ChannelId.values());
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    protected ConfigurationAdmin cm;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
        this.setConfig(config);
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        return this.getWantedActivePowerChannel().value().orElse(-1.f) + "W";
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                /*
                new FC16WriteRegistersTask(53250,
                        m(KbrBaseModule.ChannelId.METERING_VOLTAGE_PRIMARY_TRANSDUCER,
                                new UnsignedWordElement(53250),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53252,
                        m(KbrBaseModule.ChannelId.METERING_VOLTAGE_SECONDARY_TRANSDUCER,
                                new UnsignedWordElement(53252),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53254,
                        m(KbrBaseModule.ChannelId.METERING_CURRENT_PRIMARY_TRANSDUCER,
                                new UnsignedWordElement(53254),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53256,
                        m(KbrBaseModule.ChannelId.METERING_CURRENT_SECONDARY_TRANSDUCER,
                                new UnsignedWordElement(53256),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC16WriteRegistersTask(53258,
                        m(KbrBaseModule.ChannelId.FREQUENCY,
                                new UnsignedWordElement(53258),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53260,
                        m(KbrBaseModule.ChannelId.MEASUREMENT_TIME,
                                new UnsignedWordElement(53260),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53262,
                        m(KbrBaseModule.ChannelId.ATTENUATION_VOLTAGE,
                                new UnsignedWordElement(53262),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53264,
                        m(KbrBaseModule.ChannelId.ATTENUATION_CURRENT,
                                new UnsignedWordElement(53264),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC16WriteRegistersTask(53266,
                        m(KbrBaseModule.ChannelId.SYNC_TYPE,
                                new UnsignedWordElement(53266),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53268,
                        m(KbrBaseModule.ChannelId.TARIFF_CHANGE,
                                new UnsignedWordElement(53268),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53270,
                        m(KbrBaseModule.ChannelId.LOW_TARIFF_ON,
                                new UnsignedWordElement(53270),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53272,
                        m(KbrBaseModule.ChannelId.LOW_TARIFF_OFF,
                                new UnsignedWordElement(53272),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC16WriteRegistersTask(53274,
                        m(KbrBaseModule.ChannelId.DAYLIGHTSAVINGS,
                                new UnsignedWordElement(53274),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53276,
                        m(KbrBaseModule.ChannelId.WINTER_TO_SUMMER,
                                new UnsignedWordElement(53276),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53278,
                        m(KbrBaseModule.ChannelId.SUMMER_TO_WINTER,
                                new UnsignedWordElement(53278),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53280,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_ACTIVE_HT_GAIN,
                                new UnsignedWordElement(53280),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC16WriteRegistersTask(53282,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_ACTIVE_NT_GAIN,
                                new UnsignedWordElement(53282),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53284,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_BLIND_HT_GAIN,
                                new UnsignedWordElement(53284),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53286,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_BLIND_NT_GAIN,
                                new UnsignedWordElement(53286),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                 */
                new FC16WriteRegistersTask(53288,
                        m(KbrBaseModule.ChannelId.CONFIG_NEW_SYSTEM_TIME,
                                new UnsignedDoublewordElement(53288),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(61441,
                        m(KbrBaseModule.ChannelId.INTERNAL_F001,
                                new UnsignedWordElement(61441),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(61442,
                        m(KbrBaseModule.ChannelId.INTERNAL_F002,
                                new UnsignedWordElement(61442),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(61443,
                        m(KbrBaseModule.ChannelId.INTERNAL_F003,
                                new UnsignedWordElement(61443),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(61444,
                        m(KbrBaseModule.ChannelId.INTERNAL_F004,
                                new UnsignedWordElement(61444),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(61445,
                        m(KbrBaseModule.ChannelId.INTERNAL_F005,
                                new UnsignedWordElement(61445),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(61446,
                        m(KbrBaseModule.ChannelId.INTERNAL_F006,
                                new UnsignedWordElement(61446),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC4ReadInputRegistersTask(176 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.GRID_FREQUENCY, new FloatDoublewordElement(
                                        176 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(178 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.NEUTRAL_POWER, new FloatDoublewordElement(
                                        178 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(180 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.AVERAGE_NEUTRAL_POWER, new FloatDoublewordElement(
                                        180 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(182 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.TOTAL_ACTIVE_POWER, new FloatDoublewordElement(
                                        182 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(184 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.TOTAL_IDLE_POWER, new FloatDoublewordElement(
                                        184 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(186 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.TOTAL_APPARENT_POWER, new FloatDoublewordElement(
                                        186 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(188 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.POWER_FACTOR, new FloatDoublewordElement(
                                        188 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(194 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.ERROR_STATUS, new FloatDoublewordElement(
                                        194 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(196 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.SYSTEM_TIME, new FloatDoublewordElement(
                                        196 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57346 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_ACTIVE_POWER_HT_GAIN, new FloatQuadrupleWordElement(
                                        57346 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57350 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_ACTIVE_POWER_NT_GAIN, new FloatQuadrupleWordElement(
                                        57350 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57354 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_IDLE_POWER_HT_GAIN, new FloatQuadrupleWordElement(
                                        57354 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57358 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_IDLE_POWER_NT_GAIN, new FloatQuadrupleWordElement(
                                        57358 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57362 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_ACTIVE_POWER_HT_LOSS, new FloatQuadrupleWordElement(
                                        57362 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57366 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_ACTIVE_POWER_NT_LOSS, new FloatQuadrupleWordElement(
                                        57366 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57370 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_IDLE_POWER_HT_LOSS, new FloatQuadrupleWordElement(
                                        57370 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(57374 - 1, Priority.HIGH,
                        m(KbrBaseModule.ChannelId.COUNTER_STATE_IDLE_POWER_NT_LOSS, new FloatQuadrupleWordElement(
                                        57374 - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1))
        /*
        ,
                new FC16WriteRegistersTask(53290,
                        m(KbrBaseModule.ChannelId.DEFAULT_RESPONSE_TIME,
                                new UnsignedWordElement(53290),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53292,
                        m(KbrBaseModule.ChannelId.BYTE_ORDER,
                                new UnsignedWordElement(53292),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53294,
                        m(KbrBaseModule.ChannelId.ENERGY_TYPE,
                                new UnsignedWordElement(53294),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53296,
                        m(KbrBaseModule.ChannelId.IMPULSE_TYPE,
                                new UnsignedWordElement(53296),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC16WriteRegistersTask(53298,
                        m(KbrBaseModule.ChannelId.IMPULSE_FACTOR,
                                new UnsignedWordElement(53298),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53300,
                        m(KbrBaseModule.ChannelId.IMPULSE_TIME,
                                new UnsignedWordElement(53300),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53302,
                        m(KbrBaseModule.ChannelId.RELAY_ONE_PULL_OFFSET,
                                new UnsignedWordElement(53302),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53304,
                        m(KbrBaseModule.ChannelId.RELAY_ONE_PUSH_OFFSET,
                                new UnsignedWordElement(53304),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC16WriteRegistersTask(53306,
                        m(KbrBaseModule.ChannelId.RELAY_TWO_PULL_OFFSET,
                                new UnsignedWordElement(53306),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53308,
                        m(KbrBaseModule.ChannelId.RELAY_TWO_PUSH_OFFSET,
                                new UnsignedWordElement(53308),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53310,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_ACTIVE_HT_LOSS,
                                new UnsignedWordElement(53310),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53312,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_ACTIVE_NT_LOSS,
                                new UnsignedWordElement(53312),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53314,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_BLIND_HT_LOSS,
                                new UnsignedWordElement(53314),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC16WriteRegistersTask(53316,
                        m(KbrBaseModule.ChannelId.INFINITE_COUNTER_BLIND_NT_LOSS,
                                new UnsignedWordElement(53316),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        */


        );
    }

    @Override
    public void handleEvent(Event event) {
        try {
            this.checkForCommands();
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Unable to check for new Commands.");
        }
    }


    /**
     * Checks if a new Command is set in the Channel and sets the internal Channel.
     *
     * @throws OpenemsError.OpenemsNamedException on error
     */
    private void checkForCommands() throws OpenemsError.OpenemsNamedException {
        if (getCommandReset().getNextValue().isDefined() && getCommandReset().getNextValue().get()) {
            getCommandReset().setNextValue(null);
            getInternalReset().setNextWriteValue((short) 42);
        }
        if (getCommandMax().getNextValue().isDefined() && getCommandMax().getNextValue().get()) {
            getCommandMax().setNextValue(null);
            getInternalMax().setNextWriteValue((short) 0);
        }
        if (getCommandMin().getNextValue().isDefined() && getCommandMin().getNextValue().get()) {
            getCommandMin().setNextValue(null);
            getInternalMin().setNextWriteValue((short) 0);
        }
        if (getCommandHT().getNextValue().isDefined()) {
            getInternalHT().setNextWriteValue(getCommandHT().getNextValue().get());
            getCommandHT().setNextValue(null);
        }
        if (getCommandNT().getNextValue().isDefined()) {
            getInternalNT().setNextWriteValue(getCommandNT().getNextValue().get());
            getCommandNT().setNextValue(null);
        }
        if (getCommandEraseFail().getNextValue().isDefined() && getCommandEraseFail().getNextValue().get()) {
            getCommandEraseFail().setNextValue(null);
            getInternalFail().setNextWriteValue((short) 0);
        }

    }

    /**
     * Sets the Configuration Channel/s based on the Config.
     * @param config Configuration
     */
    private void setConfig(Config config) {

        try {
            getNewConfigSystemTime().setNextWriteValue(config.systemTime());
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Unable to set Config.");
        }

    }

    /**
     * This keeps track of the connected modules, so there cant be duplicates.
     * @param number Number of the Phase
     * @return true if its allowed to connect
     */
    public boolean moduleCheckout(int number) {
        switch (number) {
            case 1:
                if (!this.l1) {
                    this.l1 = true;
                    return true;
                } else {
                    break;
                }
            case 2:
                if (!this.l2) {
                    this.l2 = true;
                    return true;
                } else {
                    break;
                }
            case 3:
                if (!this.l3) {
                    this.l3 = true;
                    return true;
                } else {
                    break;
                }
            default:
                return false;
        }
        return false;
    }

    /**
     * Removes the Module from the internal Value.
     * @param number Number of the Phase
     * @return true if it was removed.
     */
    public boolean moduleRemove(int number) {
        switch (number) {
            case 1:
                if (this.l1) {
                    this.l1 = false;
                    return true;
                } else {
                    break;
                }
            case 2:
                if (this.l2) {
                    this.l2 = false;
                    return true;
                } else {
                    break;
                }
            case 3:
                if (this.l3) {
                    this.l3 = false;
                    return true;
                } else {
                    break;
                }
            default:
                return false;
        }
        return false;
    }
}
