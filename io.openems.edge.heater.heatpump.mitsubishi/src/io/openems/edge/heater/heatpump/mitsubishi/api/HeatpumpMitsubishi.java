package io.openems.edge.heater.heatpump.mitsubishi.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.HeatpumpSmartGrid;

/**
 * Channels for the Mitsubishi heat pump.
 */

public interface HeatpumpMitsubishi extends HeatpumpSmartGrid {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers, read only.

        /**
         * Error code.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8_ERROR_CODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Outside temperature. Signed.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR58_OUTSIDE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //HR60_FLOW_TEMPERATURE -> Heater FLOW_TEMPERATURE.

        //IR62_RETURN_TEMPERATURE -> Heater RETURN_TEMPERATURE.



        // Holding Registers, read/write.

        /**
         * System on/off.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 3
         *      <li> State 0: Off
         *      <li> State 1: On
         *      <li> State 2: Emergency operation, read only (Notbetrieb)
         *      <li> State 3: Test run, read only (Testlauf)
         * </ul>
         */
        HR25_SYSTEM_ON_OFF(Doc.of(SystemOnOff.values()).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Status code of the heater parsed to a string.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        ;


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Input Registers, Read only.

    /**
     * Gets the Channel for {@link ChannelId#IR8_ERROR_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getErrorCodeChannel() {
        return this.channel(ChannelId.IR8_ERROR_CODE);
    }

    /**
     * Gets the error code.
     * See {@link ChannelId#IR8_ERROR_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getErrorCode() {
        return this.getErrorCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR58_OUTSIDE_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOutsideTempChannel() {
        return this.channel(ChannelId.IR58_OUTSIDE_TEMP);
    }

    /**
     * Gets the outside temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR58_OUTSIDE_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOutsideTemp() {
        return this.getOutsideTempChannel().value();
    }


    // Holding Registers, read/write.

    /**
     * Gets the Channel for {@link ChannelId#HR25_SYSTEM_ON_OFF}.
     *
     * @return the Channel
     */
    default EnumWriteChannel getSystemOnOffChannel() {
        return this.channel(ChannelId.HR25_SYSTEM_ON_OFF);
    }

    /**
     * Get the system on/off value.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 3
     *      <li> State 0: Off
     *      <li> State 1: On
     *      <li> State 2: Emergency operation, read only (Notbetrieb)
     *      <li> State 3: Test run, read only (Testlauf)
     * </ul>
     * See {@link ChannelId#HR25_SYSTEM_ON_OFF}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSystemOnOff() {
        return this.getSystemOnOffChannel().value();
    }

    /**
     * Set the system on/off value.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 1
     *      <li> State 0: Off
     *      <li> State 1: On
     * </ul>ul>
     * See {@link ChannelId#HR25_SYSTEM_ON_OFF}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSystemOnOff(int value) throws OpenemsNamedException {
        if (value >= 0 && value <= 1) {
            this.getSystemOnOffChannel().setNextWriteValue(value);
        }
    }

    /**
     * Set the system on/off value.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 1
     *      <li> State 0: Off
     *      <li> State 1: On
     * </ul>ul
     * See {@link ChannelId#HR25_SYSTEM_ON_OFF}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSystemOnOff(Integer value) throws OpenemsNamedException {
        if (value >= 0 && value <= 1) {
            this.getSystemOnOffChannel().setNextWriteValue(value);
        }
    }

    /**
     * Set the system on/off value.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 1
     *      <li> State 0: Off
     *      <li> State 1: On
     * </ul>ul
     * See {@link ChannelId#HR25_SYSTEM_ON_OFF}.
     *
     * @param mode the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSystemOnOff(SystemOnOff mode) throws OpenemsNamedException {
        if (mode == SystemOnOff.OFF || mode == SystemOnOff.ON) {
            this.getSystemOnOffChannel().setNextWriteValue(mode.getValue());
        }
    }


    // Non Modbus channels.

    /**
     * Gets the Channel for {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel
     */
    default StringReadChannel getStatusMessageChannel() {
        return this.channel(ChannelId.STATUS_MESSAGE);
    }

    /**
     * Gets the status message.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getStatusMessage() {
        return this.getStatusMessageChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#STATUS_MESSAGE} Channel.
     *
     * @param value the next value
     */
    default void _setStatusMessage(String value) {
        this.getStatusMessageChannel().setNextValue(value);
    }
}
