package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * An UnsignedWordElement represents an Integer value in an
 * {@link AbstractWordElement}.
 */
public class ScaledUnsignedWordElement extends AbstractWordElement<ScaledUnsignedWordElement, Double> {

	private final int scaleBase10;

	public ScaledUnsignedWordElement(int address,int scaleBase10) {
		super(OpenemsType.DOUBLE, address);
		this.scaleBase10 = scaleBase10;
	}

	@Override
	protected ScaledUnsignedWordElement self() {
		return this;
	}

	protected Double fromByteBuffer(ByteBuffer buff) {
		return Short.toUnsignedInt(buff.getShort(0))*Math.pow(10, scaleBase10);
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Double value) {
		return buff.putShort(value.shortValue());
	}


}
