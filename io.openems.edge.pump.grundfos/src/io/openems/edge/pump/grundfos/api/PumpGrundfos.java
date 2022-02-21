package io.openems.edge.pump.grundfos.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.bridge.genibus.api.PumpDevice;

public interface PumpGrundfos extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Head class 0, protocol data. Can do only GET. Read only.

        /**
         * Length of communication buffer. How many bytes can be sent to this pump in one telegram. Minimum is 70,
         * which is one nearly full APDU of 62 bytes (full APDU is 63 bytes), 2 bytes APDU header, 4 bytes telegram
         * header and 2 bytes CRC.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: Protocol Data, (0, 2) buf_len
         * </ul>
         */
        BUF_LEN(Doc.of(OpenemsType.DOUBLE)),

        /**
         * In which bus mode (slave, master) is the device.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: boolean
         *      <li> Magna3: Protocol Data, (0, 3) unit_bus_mode
         * </ul>
         */
        UNIT_BUS_MODE(Doc.of(OpenemsType.DOUBLE)),


        // Head class 2, measured data. Can do GET and INFO. Read only.

        /**
         * Multipump members. Indicating presence of pump 1-8. 8 bit value, where each bit stands for one pump.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 1) multi_pump_members
         * </ul>
         */
        MULTI_PUMP_MEMBERS(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Twinpump status.
         * 0: Single pump. Not part of a multi pump
         * 1: Twin-pump master. Contact to twin pump slave OK
         * 2: Twin-pump master. No contact to twin pump slave
         * 3: Twin-pump slave. Contact to twin pump master OK
         * 4: Twin-pump slave. No contact to twin pump master
         * 5: Self appointed twin-pump master. No contact to twin pump master
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 2) tp_status
         * </ul>
         */
        TP_STATUS(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Twinpump status, parsed to a string.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         * </ul>
         */
        TP_STATUS_STRING(Doc.of(OpenemsType.STRING)),

        /**
         * Differential pressure head.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Bar
         *      <li> Magna3: 8 bit Measured Data, (2, 23) h_diff
         * </ul>
         */
        DIFFERENTIAL_PRESSURE_HEAD(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR)),

        /**
         * Electronics temperature.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Decimal degree Celsius
         *      <li> Magna3: 8 bit Measured Data, (2, 28) t_e
         * </ul>
         */
        ELECTRONICS_TEMPERATURE(Doc.of(OpenemsType.DOUBLE).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Current motor.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Ampere
         *      <li> Magna3: 8 bit Measured Data, (2, 30) i_mo
         * </ul>
         */
        MOTOR_ELECTRIC_CURRENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.AMPERE)),

        /**
         * Relative speed/frequency applied to motor.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Hertz
         *      <li> Magna3: 8 bit Measured Data, (2, 32) f_act
         * </ul>
         */
        MOTOR_FREQUENCY(Doc.of(OpenemsType.DOUBLE).unit(Unit.HERTZ)),

        /**
         * Power Consumption.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Watt
         *      <li> Magna3: 8 bit Measured Data, (2, 34) p_lo
         * </ul>
         *
         */
        POWER_CONSUMPTION(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT)),

        /**
         * Pressure/Head/level.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Bar
         *      <li> Magna3: 8 bit Measured Data, (2, 37) h
         * </ul>
         */
        PRESSURE(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR)),

        /**
         * Pump percolation.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Cubic meter per hour (m³/h)
         *      <li> Magna3: 8 bit Measured Data, (2, 39) q
         * </ul>
         */
        PERCOLATION(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR)),

        /**
         * Currently used set point.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 48) ref_act
         * </ul>
         */
        REF_ACT(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Normalized set point. Unit depends on control mode and changes when control mode changes, so channel can't
         * have a unit.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 49) ref_norm
         * </ul>
         */
        REF_NORM(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Temperature of the pumped fluid.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Decimal degree Celsius
         *      <li> Magna3: 8 bit Measured Data, (2, 58) t_w
         * </ul>
         */
        PUMPED_FLUID_TEMPERATURE(Doc.of(OpenemsType.DOUBLE).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Minimum allowed reference setting.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 76) r_min
         * </ul>
         */
        R_MIN(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Maximum allowed reference setting.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 77) r_max
         * </ul>
         */
        R_MAX(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Actual mode status No. 1 bits. Variable used by older pump models to transmit the operating and control modes.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 81) act_mode1
         * </ul>
         */
        ACT_MODE1(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Control source bits.
         * Currently active control source. From which source the pump is currently taking commands.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 90) contr_source
         * </ul>
         */
        CONTR_SOURCE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Currently active control source. From which source the pump is currently taking commands.
         * The control source bits parsed to a text message.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         * </ul>
         */
        CONTR_SOURCE_STRING(Doc.of(OpenemsType.STRING)),

        /**
         * Control mode bits. Variable used by newer pump models to transmit the control mode.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 112) control_mode
         * </ul>
         *
         */
        CONTROL_MODE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * The control mode bits parsed to a text message.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         * </ul>
         */
        CONTROL_MODE_STRING(Doc.of(OpenemsType.STRING)),

        /**
         * Grundfos sensor pressure measurement, GSP.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Bar
         *      <li> Magna3: 8 bit Measured Data, (2, 127) grf_sensor_press
         * </ul>
         */
        GRF_SENSOR_PRESS(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Unit family code.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 148) unit_family
         * </ul>
         */
        UNIT_FAMILY(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Unit type code.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 149) unit_type
         * </ul>
         */
        UNIT_TYPE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Unit version code.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 150) unit_version
         * </ul>
         */
        UNIT_VERSION(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Unit family, type and version parsed to a string.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         * </ul>
         */
        UNIT_INFO_STRING(Doc.of(OpenemsType.STRING)),

        /**
         * Alarm Code Pump. Manual says this is just for setups with multiple pumps.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 154) alarm_code_pump
         * </ul>
         *
         */
        ALARM_CODE_PUMP(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Warn Code.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 156) warn_code
         * </ul>
         */
        WARN_CODE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Alarm Code.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 158) alarm_code
         * </ul>
         */
        ALARM_CODE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Warn Bits 1-4.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 159-162) warn_bits1,2,3,4
         * </ul>
         */
        WARN_BITS_1(Doc.of(OpenemsType.DOUBLE)),
        WARN_BITS_2(Doc.of(OpenemsType.DOUBLE)),
        WARN_BITS_3(Doc.of(OpenemsType.DOUBLE)),
        WARN_BITS_4(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Warn message. Warn Bits cumulative channel. Contains the messages from all warn bits.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         * </ul>
         */
        WARN_MESSAGE(Doc.of(OpenemsType.STRING)),

        /**
         * Alarm log 1-5. Contains the code for the last 5 logged alarms. Newest is in 1.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Measured Data, (2, 163-167) alarm_log_1,2,3,4,5
         * </ul>
         */
        ALARM_LOG_1(Doc.of(OpenemsType.DOUBLE)),
        ALARM_LOG_2(Doc.of(OpenemsType.DOUBLE)),
        ALARM_LOG_3(Doc.of(OpenemsType.DOUBLE)),
        ALARM_LOG_4(Doc.of(OpenemsType.DOUBLE)),
        ALARM_LOG_5(Doc.of(OpenemsType.DOUBLE)),


        // Head class 3, commands. Can only do SET. Write only.

        /**
         * Stops the motor.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 5) STOP
         * </ul>
         */
        STOP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Starts the Motor.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 6) START
         * </ul>
         */
        START(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Switch to Remote Mode.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 7) REMOTE
         * </ul>
         */
        REMOTE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Switch to control mode const. Frequency.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 22) CONST_FREQ
         * </ul>
         *
         */
        CONST_FREQUENCY(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Switch to control mode const. Pressure.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 24) CONST_PRESS
         * </ul>
         */
        CONST_PRESSURE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Sets the motor running on min curve.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 25) MIN
         * </ul>
         *
         */
        MIN_MOTOR_CURVE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Sets the motor running on max curve.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 26) MAX
         * </ul>
         */
        MAX_MOTOR_CURVE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Sets the motor in control mode AutoAdapt.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 52) AUTO_ADAPT
         * </ul>
         *
         */
        AUTO_ADAPT(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Turn on center LED flashing.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 121) wink_on
         * </ul>
         *
         */
        WINK_ON(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Turn off center LED flashing.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         *      <li> Magna3: Commands, (3, 122) wink_off
         * </ul>
         *
         */
        WINK_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),


        // Head class 4, configuration parameters. Can do GET, SET and INFO. Read and write.

        /**
         * Pump rotation frequency f_upper. Highest motor speed/frequency, only for factory change.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Hertz
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 30) f_upper
         * </ul>
         */
        FREQUENCY_F_UPPER(Doc.of(OpenemsType.DOUBLE).unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),

        /**
         * Pump rotation frequency f_nom. Nominal speed/frequency (name plate).
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Hertz
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 31) f_nom
         * </ul>
         */
        FREQUENCY_F_NOM(Doc.of(OpenemsType.DOUBLE).unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),

        /**
         * User selected min frequency f_min. Unit is ’percent of f_nom’. Used by Low frequency stop function as Low
         * flow freq. limit.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Percent
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 34) f_min
         * </ul>
         */
        FREQUENCY_F_MIN(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * User selected max frequency f_max. Unit is ’percent of f_nom’.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Percent
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 35) f_max
         * </ul>
         */
        FREQUENCY_F_MAX(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Pump GENIbus address.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 46) unit_addr
         * </ul>
         */
        UNIT_ADDR(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Pump GENIbus group address.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 47) group_addr
         * </ul>
         */
        GROUP_ADDR(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Constant Pressure Mode minimum reference. INFO reads unit = 30 = 1%, min = 0, range = 100.
         * Values for this parameter are 0% - 100% (write 0 - 100 in channel), where % is % of the range interval of the
         * pressure sensor. The range interval of the pressure sensor is the one transmitted by INFO for h (pumps MGE
         * and Magna) or h_diff (pump Magna).
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Percent
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 83) h_const_ref_min
         * </ul>
         */
        H_CONST_REF_MIN(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Constant Pressure Mode maximum reference.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Percent
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 84) h_const_ref_max
         * </ul>
         */
        H_CONST_REF_MAX(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Low flow stop dead band relative to actual setpoint.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Percent
         *      <li> Magna3: 8bit Configuration Parameters, (4, 101) delta_h
         * </ul>
         */
        SET_PRESSURE_DELTA(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),

        /**
         * Pump maximum head/pressure.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Bar
         *      <li> Magna3: 16 bit split into two 8 bit Configuration Parameters, (4, 103) h_max_hi and (4, 104) h_max_lo
         * </ul>
         */
        SET_MAX_PRESSURE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).unit(Unit.BAR)),

        /**
         * Pump maximum flow.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Cubic meters per hour (m³/h)
         *      <li> Magna3: 16 bit split into two 8 bit Configuration Parameters, (4, 105) q_max_hi and (4, 106) q_max_lo
         * </ul>
         */
        SET_PUMP_MAX_FLOW(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR).accessMode(AccessMode.READ_WRITE)),

        // Sensor configuration

        /**
         * Analogue input 1 unit. Value 0-22, see table 8.2 on page 45 in manual.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 208) ana_in_1_unit
         * </ul>
         */
        ANA_IN_1_UNIT(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Analogue input 1 minimum range value.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 16 bit split into two 8 bit Configuration Parameters, (4, 209) ana_in_1_min_hi and (4, 210) ana_in_1_min_lo
         * </ul>
         */
        ANA_IN_1_MIN(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Analogue input 1 maximum range value.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 16 bit split into two 8 bit Configuration Parameters, (4, 211) ana_in_1_max_hi and (4, 212) ana_in_1_max_lo
         * </ul>
         */
        ANA_IN_1_MAX(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Analogue input 1 application. Which values is this sensor mapped to? For example h_diff.
         * Value 0-255, see table 8.3 on page 47 in manual.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 226) ana_in_1_applic
         * </ul>
         */
        ANA_IN_1_APPLIC(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Analogue input 1 function. Enum with values 0-3.
         * 0: Not active
         * 1: Control loop feedback -> sys_fb
         * 2: Reference influence: F(ana_in_1) -> ref_att
         * 3: Other (extra measurement)
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 229) ana_in_1_func
         * </ul>
         */
        ANA_IN_1_FUNC(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Grundfos pressure sensor function.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 238) grf_sensor_press_func
         * </ul>
         */
        GRF_SENSOR_PRESS_FUNC(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Twinpump/Multipump mode.
         * 0: None, not part of a multi pump
         * 1: Time alternating mode
         * 2: Load (power) alternating mode
         * 3: Cascade control mode
         * 4: Backup mode
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Magna3: 8 bit Configuration Parameters, (4, 241) tp_mode
         * </ul>
         */
        TP_MODE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        /**
         * Twinpump/Multipump mode parsed to a string.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         * </ul>
         */
        TP_MODE_STRING(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),

        /**
         * Maximum pressure range.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Bar
         *      <li> Magna3: 8 bit Measured Data, (4, 254) h_range
         * </ul>
         */
        H_RANGE(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR).accessMode(AccessMode.READ_WRITE)),


        // Head class 5, reference values. Can do GET, SET and INFO. Read and write.

        /**
         * Remote Reference (GENIbus set point). Unit is percent, so a value of 50 in the channel means 50%.
         * Range is 0 - 100.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Double
         *      <li> Unit: Percent
         *      <li> Magna3: 8 bit Reference Values, (5, 1) ref_rem
         * </ul>
         */
        REF_REM(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),


        // Head class 7, character strings (ASCII). Can do only GET. Read only.

        /**
         * Device product number.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         *      <li> Magna3: ASCII Value, (7, 8) device_prod_no
         * </ul>
         */
        DEVICE_PROD_NO(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Device serial number in production.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: String
         *      <li> Magna3: ASCII Value, (7, 9) serial_no
         * </ul>
         */
        SERIAL_NO(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),


        // Other, not GENIbus.

        /**
         * Connection status. Is the controller currently receiving data from the pump or not.
         * <ul>
         *      <li> Interface: PumpGrundfosChannels
         *      <li> Type: Boolean
         * </ul>
         */
        CONNECTION_OK(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }


    /**
     * Gets the Channel for {@link ChannelId#BUF_LEN}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getBufferLengthChannel() {
        return this.channel(ChannelId.BUF_LEN);
    }

    /**
     * Gets the length of the communication buffer. How many bytes can be sent to this pump in one telegram. Minimum is
     * 70, which is one nearly full APDU of 62 bytes (full APDU is 63 bytes), 2 bytes APDU header, 4 bytes telegram
     * header and 2 bytes CRC.
     * See {@link ChannelId#BUF_LEN}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getBufferLength() {
        return this.getBufferLengthChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#UNIT_BUS_MODE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getUnitBusModeChannel() {
        return this.channel(ChannelId.UNIT_BUS_MODE);
    }

    /**
     * In which bus mode (slave, master) is the device.
     * See {@link ChannelId#UNIT_BUS_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getUnitBusMode() {
        return this.getUnitBusModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#MULTI_PUMP_MEMBERS}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getMultipumpMembersChannel() {
        return this.channel(ChannelId.MULTI_PUMP_MEMBERS);
    }
    
    /**
     * Get the multipump members. Indicating presence of pump 1-8. 8 bit value, where each bit stands for one pump.
     * See {@link ChannelId#MULTI_PUMP_MEMBERS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getMultipumpMembers() {
        return this.getMultipumpMembersChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#TP_STATUS}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getTwinpumpStatusChannel() {
        return this.channel(ChannelId.TP_STATUS);
    }

    /**
     * Gets the twinpump status.
     * 0: Single pump. Not part of a multi pump
     * 1: Twin-pump master. Contact to twin pump slave OK
     * 2: Twin-pump master. No contact to twin pump slave
     * 3: Twin-pump slave. Contact to twin pump master OK
     * 4: Twin-pump slave. No contact to twin pump master
     * 5: Self appointed twin-pump master. No contact to twin pump master
     * See {@link ChannelId#TP_STATUS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getTwinpumpStatus() {
        return this.getTwinpumpStatusChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#TP_STATUS_STRING}.
     *
     * @return the Channel
     */
    default StringReadChannel getTwinpumpStatusStringChannel() {
        return this.channel(ChannelId.TP_STATUS_STRING);
    }

    /**
     * Gets the twinpump status, parsed to a string.
     * See {@link ChannelId#TP_STATUS_STRING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getTwinpumpStatusString() {
        return this.getTwinpumpStatusStringChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DIFFERENTIAL_PRESSURE_HEAD}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getDiffPressureHeadChannel() {
        return this.channel(ChannelId.DIFFERENTIAL_PRESSURE_HEAD);
    }

    /**
     * Gets the differential pressure head, unit is bar.
     * See {@link ChannelId#DIFFERENTIAL_PRESSURE_HEAD}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getDiffPressureHead() {
        return this.getDiffPressureHeadChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ELECTRONICS_TEMPERATURE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getElectronicsTemperatureChannel() {
        return this.channel(ChannelId.ELECTRONICS_TEMPERATURE);
    }

    /**
     * Gets the electronics temperature, unit is decimal degree Celsius.
     * See {@link ChannelId#ELECTRONICS_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getElectronicsTemperature() {
        return this.getElectronicsTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#MOTOR_ELECTRIC_CURRENT}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getCurrentMotorChannel() {
        return this.channel(ChannelId.MOTOR_ELECTRIC_CURRENT);
    }

    /**
     * Gets the pump motor current, unit is ampere.
     * See {@link ChannelId#MOTOR_ELECTRIC_CURRENT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getCurrentMotor() {
        return this.getCurrentMotorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#MOTOR_FREQUENCY}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getMotorFrequencyChannel() {
        return this.channel(ChannelId.MOTOR_FREQUENCY);
    }

    /**
     * Gets the relative speed/frequency applied to the pump motor, unit is hertz.
     * See {@link ChannelId#MOTOR_FREQUENCY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getMotorFrequency() {
        return this.getMotorFrequencyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_CONSUMPTION}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getPowerConsumptionChannel() {
        return this.channel(ChannelId.POWER_CONSUMPTION);
    }

    /**
     * Gets the power consumption of the pump, unit is watt.
     * See {@link ChannelId#POWER_CONSUMPTION}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPowerConsumption() {
        return this.getPowerConsumptionChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PRESSURE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getPressureChannel() {
        return this.channel(ChannelId.PRESSURE);
    }

    /**
     * Gets the pressure/head/level of the pump, unit is bar.
     * See {@link ChannelId#PRESSURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPressure() {
        return this.getPressureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PERCOLATION}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getPercolationChannel() {
        return this.channel(ChannelId.PERCOLATION);
    }

    /**
     * Gets the percolation of the pump, unit is cubic meter per hour (m³/h).
     * See {@link ChannelId#PERCOLATION}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPercolation() {
        return this.getPercolationChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#REF_ACT}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getRefActChannel() {
        return this.channel(ChannelId.REF_ACT);
    }

    /**
     * Gets the currently used set point.
     * See {@link ChannelId#REF_ACT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getRefAct() {
        return this.getRefActChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#REF_NORM}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getRefNormChannel() {
        return this.channel(ChannelId.REF_NORM);
    }

    /**
     * Gets the normalized set point. Unit depends on the control mode and changes when control mode changes, so the
     * channel can't have a unit since channel units can't be dynamic.
     * See {@link ChannelId#REF_NORM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getRefNorm() {
        return this.getRefNormChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PUMPED_FLUID_TEMPERATURE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getPumpedFluidTemperatureChannel() {
        return this.channel(ChannelId.PUMPED_FLUID_TEMPERATURE);
    }

    /**
     * Gets the temperature of the pumped fluid, unit is decimal degree Celsius.
     * See {@link ChannelId#PUMPED_FLUID_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPumpedFluidTemperature() {
        return this.getPumpedFluidTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#R_MIN}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getRminChannel() {
        return this.channel(ChannelId.R_MIN);
    }

    /**
     * Gets the minimum allowed reference setting.
     * See {@link ChannelId#R_MIN}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getRmin() {
        return this.getRminChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#R_MAX}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getRmaxChannel() {
        return this.channel(ChannelId.R_MAX);
    }

    /**
     * Gets the maximum allowed reference setting.
     * See {@link ChannelId#R_MAX}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getRmax() {
        return this.getRmaxChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ACT_MODE1}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getActMode1BitsChannel() {
        return this.channel(ChannelId.ACT_MODE1);
    }

    /**
     * Get the actual mode status no. 1 bits. Variable used by older pump models to transmit the operating and control
     * modes.
     * See {@link ChannelId#ACT_MODE1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getActMode1Bits() {
        return this.getActMode1BitsChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONTR_SOURCE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getControlSourceBitsChannel() {
        return this.channel(ChannelId.CONTR_SOURCE);
    }

    /**
     * Get the control source bits, indicating the currently active control source.
     * From which source the pump is currently taking commands.
     * See {@link ChannelId#CONTR_SOURCE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getControlSourceBits() {
        return this.getControlSourceBitsChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONTR_SOURCE_STRING}.
     *
     * @return the Channel
     */
    default StringReadChannel getControlSourceStringChannel() {
        return this.channel(ChannelId.CONTR_SOURCE_STRING);
    }

    /**
     * Get the control source bits, parsed to a string. These indicate from which source the pump is currently taking
     * commands.
     * See {@link ChannelId#CONTR_SOURCE_STRING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getControlSourceString() {
        return this.getControlSourceStringChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getControlModeBitsChannel() {
        return this.channel(ChannelId.CONTROL_MODE);
    }

    /**
     * Get the control mode bits. Variable used by newer pump models to transmit the control mode.
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getControlModeBits() {
        return this.getControlModeBitsChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONTROL_MODE_STRING}.
     *
     * @return the Channel
     */
    default StringReadChannel getControlModeStringChannel() {
        return this.channel(ChannelId.CONTROL_MODE_STRING);
    }

    /**
     * Get the control mode bits, parsed to a string. Depending on pump model, parses the actual mode status no. 1 bits
     * or the control mode bits and parses them to a string.
     * See {@link ChannelId#CONTROL_MODE_STRING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getControlModeString() {
        return this.getControlModeStringChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GRF_SENSOR_PRESS}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getGrfSensorPressChannel() {
        return this.channel(ChannelId.GRF_SENSOR_PRESS);
    }

    /**
     * Gets the Grundfos sensor pressure measurement, GSP. Unit is bar.
     * See {@link ChannelId#GRF_SENSOR_PRESS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getGrfSensorPress() {
        return this.getGrfSensorPressChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#UNIT_FAMILY}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getUnitFamilyChannel() {
        return this.channel(ChannelId.UNIT_FAMILY);
    }

    /**
     * Gets the unit family code.
     * See {@link ChannelId#UNIT_FAMILY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getUnitFamily() {
        return this.getUnitFamilyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#UNIT_TYPE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getUnitTypeChannel() {
        return this.channel(ChannelId.UNIT_TYPE);
    }

    /**
     * Gets the unit type code.
     * See {@link ChannelId#UNIT_TYPE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getUnitType() {
        return this.getUnitTypeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#UNIT_VERSION}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getUnitVersionChannel() {
        return this.channel(ChannelId.UNIT_VERSION);
    }

    /**
     * Gets the unit version code.
     * See {@link ChannelId#UNIT_VERSION}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getUnitVersion() {
        return this.getUnitVersionChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#UNIT_INFO_STRING}.
     *
     * @return the Channel
     */
    default StringReadChannel getUnitInfoChannel() {
        return this.channel(ChannelId.UNIT_INFO_STRING);
    }

    /**
     * Get the unit family, type and version parsed to a string.
     * See {@link ChannelId#UNIT_INFO_STRING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getUnitInfo() {
        return this.getUnitInfoChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_CODE_PUMP}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmCodePumpChannel() {
        return this.channel(ChannelId.ALARM_CODE_PUMP);
    }

    /**
     * Gets the alarm code pump. The manual says this is just for setups with multiple pumps.
     * See {@link ChannelId#ALARM_CODE_PUMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmCodePump() {
        return this.getAlarmCodePumpChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARN_CODE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getWarnCodeChannel() {
        return this.channel(ChannelId.WARN_CODE);
    }

    /**
     * Gets the warn code.
     * See {@link ChannelId#WARN_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getWarnCode() {
        return this.getWarnCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_CODE}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmCodeChannel() {
        return this.channel(ChannelId.ALARM_CODE);
    }

    /**
     * Gets the alarm code.
     * See {@link ChannelId#ALARM_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmCode() {
        return this.getAlarmCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARN_BITS_1}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getWarnBits1Channel() {
        return this.channel(ChannelId.WARN_BITS_1);
    }

    /**
     * Get the warn bits 1.
     * See {@link ChannelId#WARN_BITS_1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getWarnBits1() {
        return this.getWarnBits1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARN_BITS_2}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getWarnBits2Channel() {
        return this.channel(ChannelId.WARN_BITS_2);
    }

    /**
     * Get the warn bits 2.
     * See {@link ChannelId#WARN_BITS_2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getWarnBits2() {
        return this.getWarnBits2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARN_BITS_3}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getWarnBits3Channel() {
        return this.channel(ChannelId.WARN_BITS_3);
    }

    /**
     * Get the warn bits 3.
     * See {@link ChannelId#WARN_BITS_3}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getWarnBits3() {
        return this.getWarnBits3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARN_BITS_4}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getWarnBits4Channel() {
        return this.channel(ChannelId.WARN_BITS_4);
    }

    /**
     * Get the warn bits 4.
     * See {@link ChannelId#WARN_BITS_4}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getWarnBits4() {
        return this.getWarnBits4Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARN_MESSAGE}.
     *
     * @return the Channel
     */
    default StringReadChannel getWarnMessageChannel() {
        return this.channel(ChannelId.WARN_MESSAGE);
    }

    /**
     * Gets the warn message. Contains the parsed messages from warn bits 1 to 4.
     * See {@link ChannelId#WARN_MESSAGE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getWarnMessage() {
        return this.getWarnMessageChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#WARN_MESSAGE} Channel.
     *
     * @param value the next value
     */
    default void _setWarnMessage(String value) {
        this.getWarnMessageChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_LOG_1}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmLog1Channel() {
        return this.channel(ChannelId.ALARM_LOG_1);
    }

    /**
     * Gets the alarm log 1. Archive of recently triggered alarms, newest is in 1.
     * See {@link ChannelId#ALARM_LOG_1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmLog1() {
        return this.getAlarmLog1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_LOG_2}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmLog2Channel() {
        return this.channel(ChannelId.ALARM_LOG_2);
    }

    /**
     * Gets the alarm log 2. Archive of recently triggered alarms, newest is in 1.
     * See {@link ChannelId#ALARM_LOG_2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmLog2() {
        return this.getAlarmLog2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_LOG_3}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmLog3Channel() {
        return this.channel(ChannelId.ALARM_LOG_3);
    }

    /**
     * Gets the alarm log 3. Archive of recently triggered alarms, newest is in 1.
     * See {@link ChannelId#ALARM_LOG_3}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmLog3() {
        return this.getAlarmLog3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_LOG_4}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmLog4Channel() {
        return this.channel(ChannelId.ALARM_LOG_4);
    }

    /**
     * Gets the alarm log 4. Archive of recently triggered alarms, newest is in 1.
     * See {@link ChannelId#ALARM_LOG_4}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmLog4() {
        return this.getAlarmLog4Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ALARM_LOG_5}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getAlarmLog5Channel() {
        return this.channel(ChannelId.ALARM_LOG_5);
    }

    /**
     * Gets the alarm log 5. Archive of recently triggered alarms, newest is in 1.
     * See {@link ChannelId#ALARM_LOG_5}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getAlarmLog5() {
        return this.getAlarmLog5Channel().value();
    }


    // Head class 3, commands. No getter, because you can't read commands.

    /**
     * Gets the Channel for {@link ChannelId#STOP}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getStopChannel() {
        return this.channel(ChannelId.STOP);
    }

    /**
     * Stops the motor.
     * See {@link ChannelId#STOP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setStop(boolean value) throws OpenemsNamedException {
        this.getStopChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#START}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getStartChannel() {
        return this.channel(ChannelId.START);
    }

    /**
     * Starts the motor.
     * See {@link ChannelId#START}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setStart(boolean value) throws OpenemsNamedException {
        this.getStartChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#REMOTE}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getRemoteChannel() {
        return this.channel(ChannelId.REMOTE);
    }

    /**
     * Switch to remote mode.
     * See {@link ChannelId#REMOTE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setRemote(boolean value) throws OpenemsNamedException {
        this.getRemoteChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CONST_FREQUENCY}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getConstFrequencyChannel() {
        return this.channel(ChannelId.CONST_FREQUENCY);
    }

    /**
     * Switch to control mode constant frequency.
     * See {@link ChannelId#CONST_FREQUENCY}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setConstFrequency(boolean value) throws OpenemsNamedException {
        this.getConstFrequencyChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CONST_PRESSURE}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getConstPressureChannel() {
        return this.channel(ChannelId.CONST_PRESSURE);
    }

    /**
     * Switch to control mode constant pressure.
     * See {@link ChannelId#CONST_PRESSURE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setConstPressure(boolean value) throws OpenemsNamedException {
        this.getConstPressureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MIN_MOTOR_CURVE}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getMinMotorCurveChannel() {
        return this.channel(ChannelId.MIN_MOTOR_CURVE);
    }

    /**
     * Sets the motor running on min curve.
     * See {@link ChannelId#MIN_MOTOR_CURVE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setMinMotorCurve(boolean value) throws OpenemsNamedException {
        this.getMinMotorCurveChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MAX_MOTOR_CURVE}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getMaxMotorCurveChannel() {
        return this.channel(ChannelId.MAX_MOTOR_CURVE);
    }

    /**
     * Sets the motor running on max curve.
     * See {@link ChannelId#MAX_MOTOR_CURVE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setMaxMotorCurve(boolean value) throws OpenemsNamedException {
        this.getMaxMotorCurveChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#AUTO_ADAPT}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getAutoAdaptChannel() {
        return this.channel(ChannelId.AUTO_ADAPT);
    }

    /**
     * Sets the motor in control mode AutoAdapt.
     * See {@link ChannelId#AUTO_ADAPT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setAutoAdapt(boolean value) throws OpenemsNamedException {
        this.getAutoAdaptChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#WINK_ON}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getWinkOnChannel() {
        return this.channel(ChannelId.WINK_ON);
    }

    /**
     * Turn on center LED flashing.
     * See {@link ChannelId#WINK_ON}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setWinkOn(boolean value) throws OpenemsNamedException {
        this.getWinkOnChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#WINK_OFF}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getWinkOffChannel() {
        return this.channel(ChannelId.WINK_OFF);
    }

    /**
     * Turn off center LED flashing.
     * See {@link ChannelId#WINK_OFF}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setWinkOff(boolean value) throws OpenemsNamedException {
        this.getWinkOffChannel().setNextWriteValue(value);
    }


    // Head class 4, config parameters.

    /**
     * Gets the Channel for {@link ChannelId#FREQUENCY_F_UPPER}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getFupperChannel() {
        return this.channel(ChannelId.FREQUENCY_F_UPPER);
    }

    /**
     * Gets the pump rotation frequency f_upper. Highest motor speed/frequency, only for factory change. Unit is hertz.
     * See {@link ChannelId#FREQUENCY_F_UPPER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getFupper() {
        return this.getFupperChannel().value();
    }

    // No setter for f_upper because apparently that value should not be changed.

    /**
     * Gets the Channel for {@link ChannelId#FREQUENCY_F_NOM}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getFnomChannel() {
        return this.channel(ChannelId.FREQUENCY_F_NOM);
    }

    /**
     * Gets the pump rotation frequency f_nom. Nominal speed/frequency (name plate). Unit is hertz.
     * See {@link ChannelId#FREQUENCY_F_NOM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getFnom() {
        return this.getFnomChannel().value();
    }

    /**
     * Set the pump rotation frequency f_nom. Nominal speed/frequency (name plate). Unit is hertz.
     * See {@link ChannelId#FREQUENCY_F_NOM}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFnom(double value) throws OpenemsNamedException {
        this.getFnomChannel().setNextWriteValue(value);
    }

    /**
     * Set the pump rotation frequency f_nom. Nominal speed/frequency (name plate). Unit is hertz.
     * See {@link ChannelId#FREQUENCY_F_NOM}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFnom(Double value) throws OpenemsNamedException {
        this.getFnomChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#FREQUENCY_F_MIN}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getFminChannel() {
        return this.channel(ChannelId.FREQUENCY_F_MIN);
    }

    /**
     * Gets the user selected min frequency f_min. Unit is ’percent of f_nom’. Used by Low frequency stop function as
     * Low flow freq. limit.
     * See {@link ChannelId#FREQUENCY_F_MIN}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getFmin() {
        return this.getFminChannel().value();
    }

    /**
     * Set the user selected min frequency f_min. Unit is ’percent of f_nom’. Used by Low frequency stop function as
     * Low flow freq. limit.
     * See {@link ChannelId#FREQUENCY_F_MIN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFmin(double value) throws OpenemsNamedException {
        this.getFminChannel().setNextWriteValue(value);
    }

    /**
     * Set the user selected min frequency f_min. Unit is ’percent of f_nom’. Used by Low frequency stop function as
     * Low flow freq. limit.
     * See {@link ChannelId#FREQUENCY_F_MIN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFmin(Double value) throws OpenemsNamedException {
        this.getFminChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#FREQUENCY_F_MAX}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getFmaxChannel() {
        return this.channel(ChannelId.FREQUENCY_F_MAX);
    }

    /**
     * Gets the user selected max frequency f_max. Unit is ’percent of f_nom’.
     * See {@link ChannelId#FREQUENCY_F_MAX}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getFmax() {
        return this.getFmaxChannel().value();
    }

    /**
     * Set the user selected max frequency f_max. Unit is ’percent of f_nom’.
     * See {@link ChannelId#FREQUENCY_F_MAX}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFmax(double value) throws OpenemsNamedException {
        this.getFmaxChannel().setNextWriteValue(value);
    }

    /**
     * Set the user selected max frequency f_max. Unit is ’percent of f_nom’.
     * See {@link ChannelId#FREQUENCY_F_MAX}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFmax(Double value) throws OpenemsNamedException {
        this.getFmaxChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#UNIT_ADDR}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getUnitAddrChannel() {
        return this.channel(ChannelId.UNIT_ADDR);
    }

    /**
     * Gets the Genibus unit address of the pump.
     * See {@link ChannelId#UNIT_ADDR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getUnitAddr() {
        return this.getUnitAddrChannel().value();
    }

    /**
     * Sets the Genibus unit address of the pump.
     * See {@link ChannelId#UNIT_ADDR}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setUnitAddr(double value) throws OpenemsNamedException {
        this.getUnitAddrChannel().setNextWriteValue(value);
    }

    /**
     * Sets the Genibus unit address of the pump.
     * See {@link ChannelId#UNIT_ADDR}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setUnitAddr(Double value) throws OpenemsNamedException {
        this.getUnitAddrChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#GROUP_ADDR}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getGroupAddrChannel() {
        return this.channel(ChannelId.GROUP_ADDR);
    }

    /**
     * Gets the Genibus group address of the pump.
     * See {@link ChannelId#UNIT_ADDR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getGroupAddr() {
        return this.getGroupAddrChannel().value();
    }

    /**
     * Sets the Genibus group address of the pump.
     * See {@link ChannelId#UNIT_ADDR}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setGroupAddr(double value) throws OpenemsNamedException {
        this.getGroupAddrChannel().setNextWriteValue(value);
    }

    /**
     * Sets the Genibus group address of the pump.
     * See {@link ChannelId#UNIT_ADDR}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setGroupAddr(Double value) throws OpenemsNamedException {
        this.getGroupAddrChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#H_CONST_REF_MIN}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getHconstRefMinChannel() {
        return this.channel(ChannelId.H_CONST_REF_MIN);
    }

    /**
     * Gets the constant pressure mode minimum reference. Unit is percent of the pressure sensor range.
     * See {@link ChannelId#H_CONST_REF_MIN}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getHconstRefMin() {
        return this.getHconstRefMinChannel().value();
    }

    /**
     * Sets the constant pressure mode minimum reference. Unit is percent of the pressure sensor range.
     * See {@link ChannelId#H_CONST_REF_MIN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHconstRefMin(double value) throws OpenemsNamedException {
        this.getHconstRefMinChannel().setNextWriteValue(value);
    }

    /**
     * Sets the constant pressure mode minimum reference. Unit is percent of the pressure sensor range.
     * See {@link ChannelId#H_CONST_REF_MIN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHconstRefMin(Double value) throws OpenemsNamedException {
        this.getHconstRefMinChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#H_CONST_REF_MAX}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getHconstRefMaxChannel() {
        return this.channel(ChannelId.H_CONST_REF_MAX);
    }

    /**
     * Gets the constant pressure mode maximum reference. Unit is percent of the pressure sensor range.
     * See {@link ChannelId#H_CONST_REF_MAX}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getHconstRefMax() {
        return this.getHconstRefMaxChannel().value();
    }

    /**
     * Sets the constant pressure mode maximum reference. Unit is percent of the pressure sensor range.
     * See {@link ChannelId#H_CONST_REF_MAX}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHconstRefMax(double value) throws OpenemsNamedException {
        this.getHconstRefMaxChannel().setNextWriteValue(value);
    }

    /**
     * Sets the constant pressure mode maximum reference. Unit is percent of the pressure sensor range.
     * See {@link ChannelId#H_CONST_REF_MAX}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHconstRefMax(Double value) throws OpenemsNamedException {
        this.getHconstRefMaxChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_PRESSURE_DELTA}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getPressureDeltaChannel() {
        return this.channel(ChannelId.SET_PRESSURE_DELTA);
    }

    /**
     * Gets the low flow stop dead band relative to actual setpoint. Unit is percent.
     * See {@link ChannelId#SET_PRESSURE_DELTA}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPressureDelta() {
        return this.getPressureDeltaChannel().value();
    }

    /**
     * Sets the low flow stop dead band relative to actual setpoint. Unit is percent.
     * See {@link ChannelId#SET_PRESSURE_DELTA}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setPressureDeltaChannel(double value) throws OpenemsNamedException {
        this.getPressureDeltaChannel().setNextWriteValue(value);
    }

    /**
     * Sets the low flow stop dead band relative to actual setpoint. Unit is percent.
     * See {@link ChannelId#SET_PRESSURE_DELTA}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setPressureDeltaChannel(Double value) throws OpenemsNamedException {
        this.getPressureDeltaChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_MAX_PRESSURE}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getMaxPressureChannel() {
        return this.channel(ChannelId.SET_MAX_PRESSURE);
    }

    /**
     * Gets the pump maximum head/pressure. Unit is bar.
     * See {@link ChannelId#SET_MAX_PRESSURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getMaxPressure() {
        return this.getMaxPressureChannel().value();
    }

    /**
     * Sets the pump maximum head/pressure. Unit is bar.
     * See {@link ChannelId#SET_MAX_PRESSURE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setMaxPressure(double value) throws OpenemsNamedException {
        this.getMaxPressureChannel().setNextWriteValue(value);
    }

    /**
     * Sets the pump maximum head/pressure. Unit is bar.
     * See {@link ChannelId#SET_MAX_PRESSURE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setMaxPressure(Double value) throws OpenemsNamedException {
        this.getMaxPressureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_PUMP_MAX_FLOW}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getPumpMaxFlowChannel() {
        return this.channel(ChannelId.SET_PUMP_MAX_FLOW);
    }

    /**
     * Gets the pump maximum flow. Unit is cubic meters per hour (m³/h).
     * See {@link ChannelId#SET_PUMP_MAX_FLOW}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPumpMaxFlow() {
        return this.getPumpMaxFlowChannel().value();
    }

    /**
     * Sets the pump maximum flow. Unit is cubic meters per hour (m³/h).
     * See {@link ChannelId#SET_PUMP_MAX_FLOW}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setPumpMaxFlow(double value) throws OpenemsNamedException {
        this.getPumpMaxFlowChannel().setNextWriteValue(value);
    }

    /**
     * Sets the pump maximum flow. Unit is cubic meters per hour (m³/h).
     * See {@link ChannelId#SET_PUMP_MAX_FLOW}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setPumpMaxFlow(Double value) throws OpenemsNamedException {
        this.getPumpMaxFlowChannel().setNextWriteValue(value);
    }


    // Sensor configuration
    default DoubleWriteChannel getSensor1FuncChannel() {
        return this.channel(ChannelId.ANA_IN_1_FUNC);
    }

    default DoubleWriteChannel getSensor1ApplicChannel() {
        return this.channel(ChannelId.ANA_IN_1_APPLIC);
    }

    default DoubleWriteChannel getSensor1UnitChannel() {
        return this.channel(ChannelId.ANA_IN_1_UNIT);
    }

    default DoubleWriteChannel getSensor1MinChannel() {
        return this.channel(ChannelId.ANA_IN_1_MIN);
    }

    default DoubleWriteChannel getSensor1MaxChannel() {
        return this.channel(ChannelId.ANA_IN_1_MAX);
    }

    default DoubleWriteChannel getSensorGspFuncChannel() {
        return this.channel(ChannelId.GRF_SENSOR_PRESS_FUNC);
    }

    default DoubleWriteChannel getHrangeChannel() {
        return this.channel(ChannelId.H_RANGE);
    }


    // Head class 5, reference values.

    /**
     * Gets the Channel for {@link ChannelId#REF_REM}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getRefRemChannel() {
        return this.channel(ChannelId.REF_REM);
    }

    /**
     * Gets the remote reference set point. Unit is percent, percent of what depends on the control mode.
     * See {@link ChannelId#REF_REM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getRefRem() {
        return this.getRefRemChannel().value();
    }

    /**
     * Sets the remote reference set point. Unit is percent, percent of what depends on the control mode.
     * See {@link ChannelId#REF_REM}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setRefRem(double value) throws OpenemsNamedException {
        this.getRefRemChannel().setNextWriteValue(value);
    }

    /**
     * Sets the remote reference set point. Unit is percent, percent of what depends on the control mode.
     * See {@link ChannelId#REF_REM}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setRefRem(Double value) throws OpenemsNamedException {
        this.getRefRemChannel().setNextWriteValue(value);
    }

    default DoubleWriteChannel setTpMode() {
        return this.channel(ChannelId.TP_MODE);
    }

    default StringReadChannel getTpModeString() {
        return this.channel(ChannelId.TP_MODE_STRING);
    }

    /**
     * Gets the Channel for {@link ChannelId#DEVICE_PROD_NO}.
     *
     * @return the Channel
     */
    default StringReadChannel getProductNumberChannel() {
        return this.channel(ChannelId.DEVICE_PROD_NO);
    }

    /**
     * Gets the device product number.
     * See {@link ChannelId#DEVICE_PROD_NO}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getProductNumber() {
        return this.getProductNumberChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#SERIAL_NO}.
     *
     * @return the Channel
     */
    default StringReadChannel getSerialNumberChannel() {
        return this.channel(ChannelId.SERIAL_NO);
    }

    /**
     * Gets the device serial number in production.
     * See {@link ChannelId#SERIAL_NO}.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getSerialNumber() {
        return this.getSerialNumberChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#REF_REM}.
     *
     * @return the Channel
     */
    default Channel<Boolean> getConnectionOkChannel() {
        return this.channel(ChannelId.CONNECTION_OK);
    }

    /**
     * Gets the status of the Genibus connection of the pump. ’true’ means the connection is ok, ’false’ means the
     * connection is lost.
     * See {@link ChannelId#CONNECTION_OK}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getConnectionOk() {
        return this.getConnectionOkChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CONNECTION_OK} Channel.
     *
     * @param value the next write value
     */
    default void _setConnectionOk(boolean value) {
        this.getConnectionOkChannel().setNextValue(value);
    }

    public PumpDevice getPumpDevice();

}



