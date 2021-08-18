package io.openems.edge.kbr4f96;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Kbr 4F96 Main", description = "Main module of the Kbr 4F96 Unit. Required for the L Units")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Kbr Unit.")
    String id() default "Kbr";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Kbr Unit.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId();
    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge")
    String modbusBridgeId();
/*
    @AttributeDefinition(name = "MeteringVoltagePrimaryTransducer",
            description = "Voltage in V of the Metering Voltage of the Primary Transducer. (1-10000000)")
    long meteringVoltagePrimaryTransducer() default 1;

    @AttributeDefinition(name = "MeteringVoltageSecondaryTransducer",
            description = "Voltage in V of the Metering Voltage of the Secondary Transducer. (1-600)")
    long meteringVoltageSecondaryTransducer() default 1;

    @AttributeDefinition(name = "MeteringCurrentPrimaryTransducer",
            description = "Current in A of the Metering Current of the Primary Transducer. (1-10000000)")
    long meteringCurrentPrimaryTransducer() default 1;

    @AttributeDefinition(name = "MeteringCurrentSecondaryTransducer",
            description = "Voltage in A of the Metering Current of the Secondary Transducer. (1-600)",
            options = {
            @Option(label = "1A", value = "1"),
            @Option(label = "5A", value = "5")
            })
    long meteringCurrentSecondaryTransducer() default 1;

    @AttributeDefinition(name = "FrequencyMode",
            description = "Frequency of the Grid",
            options = {
            @Option(label = "Automatic", value = "0"),
            @Option(label = "50Hz", value = "1"),
            @Option(label = "60Hz", value = "2")
    })
    long frequency() default 0;

    @AttributeDefinition(name = "MeasurementTimeDelta",
            description = "Time between Measurement for Average Power Level in minutes (0-255)")
    long measurementTime() default 10;

    @AttributeDefinition(name = "AttenuationVoltage",
            description = "Attenuation Voltage (0-9)")
    long attenuationVoltage() default 0;

    @AttributeDefinition(name = "AttenuationCurrent",
            description = "Attenuation Current (0-9)")
    long attenuationCurrent() default 0;

    @AttributeDefinition(name = "SynchronisationType",
            description = "Type of Synchronisation",
            options = {
            @Option(label = "Through internal Clock", value = "0"),
            @Option(label = "Through external Sync impulse", value = "1"),
            @Option(label = "Through Bus", value = "2"),
            @Option(label = "Through Tariff change", value = "3")
    })
    long syncType() default 0;

    @AttributeDefinition(name = "TariffChange",
            description = "Attenuation Current (0-9)",
            options = {
            @Option(label = "Through digital Input", value = "0"),
            @Option(label = "Through Bus", value = "1"),
            @Option(label = "Through time, saved in Device", value = "2")
    })
    long tariffChange() default 0;

    @AttributeDefinition(name = "LowTariffOnTime",
            description = "Time for activating the Low Tariff in Minutes of the Day (0-1440)")
    long lowTariffOn() default 0;

    @AttributeDefinition(name = "LowTariffOffTime",
            description = "Time for deactivating the Low Tariff in Minutes of the Day (0-1440)")
    long lowTariffOff() default 0;

    @AttributeDefinition(name = "DaylightSavingsOn",
            description = "Activate if you want to use Daylight Savings")
    boolean daylightSavings() default false;

    @AttributeDefinition(name = "SummerToWinter",
            description = "Month where Summertime switched to Wintertime (1-12)")
    long SummerToWinter() default 6;

    @AttributeDefinition(name = "WinterToSummer",
            description = "Month where Wintertime switched to Summertime (1-12)")
    long WinterToSummer() default 1;

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyHTGain",
            description = "Set Infinite Counter for Active energy HT Gain")
    float infiniteCounterActiveHtGain();

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyNTGain",
            description = "Set Infinite Counter for Active energy NT Gain")
    float infiniteCounterActiveNtGain();

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyHTGain",
            description = "Set Infinite Counter for Blind work HT Gain")
    float infiniteCounterBlindHtGain();

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyNTGain",
            description = "Set Infinite Counter for Blind work NT Gain")
    float infiniteCounterBlindNtGain();
*/
    @AttributeDefinition(name = "SystemTime",
            description = "Set System Time as Timestamp")
    long systemTime();
/*
    @AttributeDefinition(name = "DefaultResponseTime",
            description = "Set Default Response Time as Factor between 0-255 (255 = 25.5)")
    long defaultResponseTime() default 10;

    @AttributeDefinition(name = "ByteOrder",
            description = "Change Byte order of Modbus Output",
            options = {
                    @Option(label = "keep as Defined", value = "1"),
                    @Option(label = "invert", value = "0")
            })
    long byteOrder() default 1;

    @AttributeDefinition(name = "Energytype",
            description = "Set Type of Energy for the Sync/Tariff-change impulse (0-63)")
    long energyType() default 0;

    @AttributeDefinition(name = "ImpulseType",
            description = "Type of Impulse type at Impulse Output",
            options = {
                    @Option(label = "Proportional to Active Energy Gain", value = "0"),
                    @Option(label = "Proportional to Blind Energy Gain", value = "1"),
                    @Option(label = "Proportional to Blind Energy Loss", value = "2"),
                    @Option(label = "Proportional to Blind Energy Loss", value = "3")
            })
    long impulseType() default 0;

    @AttributeDefinition(name = "ImpulseFactor",
            description = "Factor of Impulse at Impulse Output (0-999999)")
    float impulseFactor() default 0;

    @AttributeDefinition(name = "ImpulseTime",
            description = "Time for on Impulse in 10 steps in ms  (30-990)")
    long impulseTime() default 30;

    @AttributeDefinition(name = "RelayOnePullOffset",
            description = "Pull Offset for Relay 1 in s (0-255)")
    long relayOnePullOffset() default 0;
    @AttributeDefinition(name = "RelayOnePushOffset",
            description = "Pull Offset for Relay 1 in s (0-255)")
    long relayOnePushOffset() default 0;
    @AttributeDefinition(name = "RelayTwoPullOffset",
            description = "Pull Offset for Relay 2 in s (0-255)")
    long relayTwoPullOffset() default 0;
    @AttributeDefinition(name = "RelayTwoPushOffset",
            description = "Push Offset for Relay 2 in s (0-255)")
    long relayTwoPushOffset() default 0;

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyHTLoss",
            description = "Set Infinite Counter for Active energy HT Loss")
    float infiniteCounterActiveHtLoss();

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyNTLoss",
            description = "Set Infinite Counter for Active energy NT Loss")
    float infiniteCounterActiveNtLoss();

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyHTLoss",
            description = "Set Infinite Counter for Blind work HT Loss")
    float infiniteCounterBlindHtLoss();

    @AttributeDefinition(name = "InfiniteCounterActiveEnergyNTLoss",
            description = "Set Infinite Counter for Blind work NT Loss")
    float infiniteCounterBlindNtLoss();


 */
    String webconsole_configurationFactory_nameHint() default "{id}";
}
