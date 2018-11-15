package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class UnsignedDoublewordElement extends AbstractDoubleWordElement<Long> {

	public UnsignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	public UnsignedDoublewordElement wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Integer.toUnsignedLong(buff.getInt(0));
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}
}
