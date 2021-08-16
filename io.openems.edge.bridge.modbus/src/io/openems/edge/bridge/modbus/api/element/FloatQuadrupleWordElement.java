package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

/**
 * A FloatQuadrupleWordElement represents a Double value in an
 * {@link AbstractQuadrupleWordElement}.
 */

public class FloatQuadrupleWordElement extends AbstractQuadrupleWordElement<FloatQuadrupleWordElement, Double> {


    public FloatQuadrupleWordElement(int address) {
        super(OpenemsType.DOUBLE, address);
    }

    @Override
    protected FloatQuadrupleWordElement self() {
        return this;
    }

    protected Double fromByteBuffer(ByteBuffer buff) {
        return Double.valueOf(buff.getDouble());
    }

    protected ByteBuffer toByteBuffer(ByteBuffer buff, Double value) {
        return buff.putDouble(value.doubleValue());
    }

}
