package io.openems.edge.controller.api.modbus.custom.tcp;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.AbstractModbusApi;
import io.openems.edge.controller.api.modbus.ModbusApi;
import io.openems.edge.controller.api.modbus.readwrite.ModbusApiReadWrite;
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
import org.osgi.service.metatype.annotations.Designate;

/**
 * This is the extension for AbstractModbusApi to operate in TCP mode with custom Modbus mapping.
 * A direct copy of ModbusTcpApiReadWriteImpl except for the custom Modbus mapping.
 */

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp.Custom", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModbusTcpApiCustomImpl extends AbstractModbusApi
		implements ModbusApiReadWrite, ModbusApi, Controller, OpenemsComponent, JsonApi {

	private int port = DEFAULT_PORT_TCP;
	private int maxConcurrentConnections = DEFAULT_MAX_CONCURRENT_CONNECTIONS;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected Meta metaComponent = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addComponent(OpenemsComponent component) {
		super.addComponent(component);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	public ModbusTcpApiCustomImpl() {
		super("Modbus/TCP-Api Custom", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				ModbusApiReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, this.cpm, this.metaComponent,
				config.component_ids(), config.apiTimeout(), Integer.toString(config.port()));
		this.port = config.port();
		this.maxConcurrentConnections = config.maxConcurrentConnections();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Creates the Modbus slave.
	 *
	 * @return the {@link ModbusSlave}
	 */
	@Override
	protected com.ghgande.j2mod.modbus.slave.ModbusSlave createModbusSlave() throws ModbusException {
		return ModbusSlaveFactory.createTCPSlave(this.port, this.maxConcurrentConnections);
	}

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
	}
}