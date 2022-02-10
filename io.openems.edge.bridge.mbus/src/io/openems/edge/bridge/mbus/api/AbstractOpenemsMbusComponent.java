package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractOpenemsMbusComponent extends AbstractOpenemsComponent {

	protected final List<ChannelRecord> channelDataRecordsList = new ArrayList<ChannelRecord>();

	private Integer primaryAddress = null;
	private String moduleId;
	protected boolean dynamicDataAddress = false; // For M-Bus devices that have dynamic addresses of their data.

	protected AbstractOpenemsMbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
										   io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	public Integer getPrimaryAddress() {
		return this.primaryAddress;
	}

	public String getModuleId() {
		return moduleId;
	}

	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context        ComponentContext of this component. Receive it from
	 *                       parameter for @Activate
	 * @param id             ID of this component. Typically 'config.id()'
	 * @param alias          Human-readable name of this Component. Typically
	 *                       'config.alias()'. Defaults to 'id' if empty
	 * @param enabled        Whether the component should be enabled. Typically
	 *                       'config.enabled()'
	 * @param primaryAddress Primary address of the M-Bus device. Typically
	 *                       'config.primaryAddress'
	 * @param cm             An instance of ConfigurationAdmin. Receive it
	 *                       using @Reference
	 * @param mbusReference  The name of the @Reference setter method for the M-Bus
	 *                       bridge
	 * @param mbusId         The ID of the M-Bus bridge. Typically
	 *                       'config.mbusBridgeId()'
	 * @param pollingInterval         The polling interval in seconds.
	 *                                Set to 0 to poll every cycle.
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int primaryAddress,
							   ConfigurationAdmin cm, String mbusReference, String mbusId, int pollingInterval) {
		super.activate(context, id, alias, enabled);
		this.primaryAddress = primaryAddress;

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "mbus", mbusId)) {
			return true;
		}
		this.moduleId = id;
		BridgeMbus mbus = this.mbusId.get();
		if (this.isEnabled() && mbus != null) {
			this.addChannelDataRecords();
			mbus.addTask(moduleId, new MbusTask(mbus, this, pollingInterval));
		}
		return false;
	}

	// Second constructor with added errorMessageChannel, to create tasks that use the errorMessageChannel.
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int primaryAddress,
							   ConfigurationAdmin cm, String mbusReference, String mbusId, int pollingInterval,
							   StringReadChannel errorMessageChannel) {
		super.activate(context, id, alias, enabled);
		this.primaryAddress = primaryAddress;

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "mbus", mbusId)) {
			return true;
		}
		this.moduleId = id;
		BridgeMbus mbus = this.mbusId.get();
		if (this.isEnabled() && mbus != null) {
			this.addChannelDataRecords();
			mbus.addTask(moduleId, new MbusTask(mbus, this, pollingInterval, errorMessageChannel));
		}
		return false;
	}

	/**
	 * Define channels of the mbus device and the record position of its
	 * corresponding values or the datatype if the channel displays secondary
	 * address values.
	 */
	protected abstract void addChannelDataRecords();

	protected void deactivate() {
		super.deactivate();
		BridgeMbus mbus = this.mbusId.getAndSet(null);
		if (mbus != null) {
			mbus.removeTask(moduleId);
		}
	}

	private AtomicReference<BridgeMbus> mbusId = new AtomicReference<BridgeMbus>(null);

	/**
	 * Set the Mbus bridge. Should be called by @Reference
	 *
	 * @param mbus the BridgeMbus Reference
	 */
	protected void setMbus(BridgeMbus mbus) {
		this.mbusId.set(mbus);
	}

	/**
	 * Unset the Mbus bridge. Should be called by @Reference
	 *
	 * @param mbus the BridgeMbus Reference
	 */
	protected void unsetMbus(BridgeMbus mbus) {
		this.mbusId.compareAndSet(mbus, null);
		if (mbus != null) {
			mbus.removeTask(moduleId);;
		}
	}

	protected BridgeMbus getMBusBridge() {
		BridgeMbus mbus = this.mbusId.get();
		return mbus;
	}

	/**
	 * Some meters change the record positions in their data during runtime. This option accounts for that. When enabled,
	 * it checks for the correctness of the record position by comparing the unit of the channel with the unit of the
	 * data on that record position. If it is a mismatch, the records are searched to find a data item with matching unit.
	 *
	 * @return use dynamicDataAddress true/false
	 */
	public boolean isDynamicDataAddress() {
		return dynamicDataAddress;
	}

	/**
	 * If "dynamicDataAddress" is true, this method is called. It checks for the correctness of the record position by
	 * comparing the unit of the channel with the unit of the data on that record position. If it is a mismatch, the
	 * records are searched to find a data item with matching unit.
	 *
	 * @param data                   The data received from the WM-Bus device.
	 * @param channelDataRecordsList The list of channelDataRecords, where the addresses are stored. The method
	 *                               should modify the addresses in this list.
	 */
	public abstract void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList);
}
