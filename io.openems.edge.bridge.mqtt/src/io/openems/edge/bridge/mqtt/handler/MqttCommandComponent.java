package io.openems.edge.bridge.mqtt.handler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.CommandWrapper;
import io.openems.edge.bridge.mqtt.api.MqttCommandType;
import io.openems.edge.bridge.mqtt.api.MqttCommands;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * The MqttCommandComponent provides a class that allows the usage of Commands, written in the MqttCommandType and MqttCommands.
 */
@Designate(ocd = CommandComponentConfig.class, factory = true)
@Component(name = "MqttCommandComponent",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MqttCommandComponent extends MqttOpenemsComponentConnector implements OpenemsComponent {

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    public MqttCommandComponent() {
        super(OpenemsComponent.ChannelId.values(), MqttComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, CommandComponentConfig config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException, MqttException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(), this.cpm, config.mqttBridgeId())) {
            this.configureMqtt(config);
        } else {
            throw new ConfigurationException("Something went wrong", "Somethings wrong in Activate method");
        }
    }

    @Modified
    void modified(ComponentContext context, CommandComponentConfig config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException, MqttException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        super.connectorDeactivate();
        this.configureMqtt(config);
    }

    private void configureMqtt(CommandComponentConfig config) throws MqttException, ConfigurationException, IOException, OpenemsError.OpenemsNamedException {
        super.setTelemetryComponent(config.otherComponentId(), this.cpm);

        super.setConfiguration(MqttType.COMMAND, config.subscriptionList(), null,
                config.payloads(), config.createdByOsgi(), config.mqttId(), this.cm, config.channelIdList().length,
                config.pathForJson(), config.payloadStyle(), config.configurationDone());
    }


    @Override
    public void reactToCommand() {
        if (super.mqttBridge.get() != null && super.otherComponent instanceof MqttCommands) {
            super.mqttBridge.get().getSubscribeTasks(super.id()).stream().filter(entry -> entry.getMqttType().equals(MqttType.COMMAND)).collect(Collectors.toList()).forEach(entry -> {
                if (entry instanceof MqttSubscribeTask) {
                    MqttSubscribeTask task = (MqttSubscribeTask) entry;
                    task.getCommandValues().forEach((key, value) -> {
                        if (this.mqttConfigurationComponent.valueLegit(value.getValue())) {
                            if (!this.mqttConfigurationComponent.expired(task, value)) {
                                this.reactToComponentCommand(key, value);
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * React to Command depending on the Component. MqttCommandType and Wrapper are given by calling "React to Command".
     *
     * @param key   MqttCommandType.
     * @param value Value of the MqttCommand.
     */
    private void reactToComponentCommand(MqttCommandType key, CommandWrapper value) {
        if (super.otherComponent instanceof MqttCommands) {
            MqttCommands commandChannel = (MqttCommands) super.otherComponent;
            try {
                switch (key) {
                    case SETPOWER:
                        commandChannel.getSetPower().setNextWriteValue(value.getValue());
                        break;
                    case SETPERFORMANCE:
                        commandChannel.getSetPerformance().setNextWriteValue(value.getValue());
                        break;
                    case SETSCHEDULE:
                        commandChannel.getSetSchedule().setNextWriteValue(value.getValue());
                        break;
                    case SETTEMPERATURE:
                        commandChannel.getSetTemperature().setNextWriteValue(value.getValue());
                        break;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.connectorDeactivate();
    }
}

