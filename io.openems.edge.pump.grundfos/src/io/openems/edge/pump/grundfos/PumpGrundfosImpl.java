package io.openems.edge.pump.grundfos;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.genibus.api.Genibus;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.PumpCommandsTask;
import io.openems.edge.bridge.genibus.api.task.PumpReadTask8bit;
import io.openems.edge.bridge.genibus.api.task.PumpReadTaskAscii;
import io.openems.edge.bridge.genibus.api.task.PumpWriteTask16bitOrMore;
import io.openems.edge.bridge.genibus.api.task.PumpWriteTask8bit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.pump.grundfos.api.PumpGrundfos;
import io.openems.edge.pump.grundfos.api.PumpMode;
import io.openems.edge.pump.grundfos.api.PumpType;
import io.openems.edge.pump.grundfos.api.WarnBits;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This module reads the most important variables via Genibus from a Grundfos pump and maps them OpenEMS channems.
 *
 * Even though the manual lists commands for Genibus to do a multipump setup, it is not possible to do so. These
 * commands are not functional and are not included because of that.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Pump.Grundfos",
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
        })
public class PumpGrundfosImpl extends AbstractOpenemsComponent implements OpenemsComponent, PumpGrundfos, EventHandler {

    private AtomicReference<Genibus> genibusId = new AtomicReference<Genibus>(null);
    private Genibus genibus;

    @Reference
    protected ConfigurationAdmin cm;

    private PumpType pumpType;
    private WarnBits warnBits;
    private PumpDevice pumpDevice;
    private boolean pumpWink;
    private boolean broadcast = false;
    private boolean changeAddress;
    private double newAddress;
    private boolean isMagna3;
    private boolean doOnce = false;

    /* Setup of a multipump system is not possible with genibus.
    private TpModeSetting tpMode;
    */

    private final Logger log = LoggerFactory.getLogger(PumpGrundfosImpl.class);

    public PumpGrundfosImpl() {
        super(OpenemsComponent.ChannelId.values(), PumpGrundfos.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Genibus", config.genibusBridgeId()) == false) {
            this.genibus = this.genibusId.get();
        }

        this.isMagna3 = config.isMagna3();
        //allocatePumpType(config.pumpType());
        this.allocatePumpType("Magna3");
        this.pumpWink = config.pumpWink();
        this.broadcast = config.broadcast();
        this.changeAddress = config.changeAddress();
        this.newAddress = config.newAddress();
        this.doOnce = false;

        /* Setup of a multipump system is not possible with genibus.
        tpMode = config.tpMode();
        */

        if (this.broadcast) {
            this.createTaskList(super.id(), 254);
        } else {
            this.createTaskList(super.id(), config.pumpAddress());
        }
        this.pumpFlashLed();
    }

    private void allocatePumpType(String pumpType) {
        switch (pumpType) {
            case "Magna3":
                this.pumpType = PumpType.MAGNA_3;
                this.warnBits = WarnBits.MAGNA_3;
                break;
        }
    }

    @Deactivate
    public void deactivate() {
        Genibus genibus = this.genibusId.getAndSet(null);
        if (genibus != null) {
            genibus.removeDevice(super.id());
        }
        super.deactivate();
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setGenibus(Genibus genibus) {
        this.genibusId.set(genibus);
    }

    protected void unsetGenibus(Genibus genibus) {
        this.genibusId.compareAndSet(genibus, null);
        if (genibus != null) {
            genibus.removeDevice(super.id());
        }
    }

    private void pumpFlashLed() throws OpenemsError.OpenemsNamedException {
        if (this.pumpWink) {
            this.getWinkOnChannel().setNextWriteValue(true);
        } else {
            this.getWinkOffChannel().setNextWriteValue(true);
        }
    }

    /** Creates a PumpDevice object containing all the tasks the GENIbus should send to this device. The PumpDevice is
     * then added to the GENIbus bridge.
     *
     * <p>Tasks automatically decide if they are GET, SET or INFO. (If you don't know what that means, read the GENIbus specs.)
     * Right now only headclasses 0, 2, 3, 4, 5 and 7 are supported. Assuming a task is priority high:
     * - INFO is done when needed before any GET or SET.
     * - GET is done every cycle for all "Measured Data" tasks (= headclass 2), "Protocol Data" tasks (= headclass 0)
     *   and "ASCII" tasks (= headclass 7).
     * - SET is done every cycle for all "Command" tasks (= headclass 3), "Configuration Parameter" tasks (= headclass 4)
     *   and "Reference Value" tasks (= headclass 5) when there is a value in the "nextWrite" of the channel.
     *   The "nextWrite" of the channel is reset to "null" once the SET is executed. This means a SET is done just once.
     *   For repeated execution of SET, you need to repeatedly write in "nextWrite" of the channel.
     * - GET for headclass 4 and 5 (3 has no GET) is done once at the start, and then only after a SET to update the
     *   value. The result of the GET is written in the "nextValue" of the channel.
     * If the connection to a device is lost (pump switched off, serial connection unplugged), the controller will
     * attempt to reestablish the connection. If that succeeds, the device is treated as if it is a new device, meaning
     * all INFO is requested again, all once tasks done again etc.</p>
     *
     * <p>Suggestions for priority settings.
     * - Headclass 0 and 7:     once.
     * - Headclass 2:           high or low.
     * - Headclass 3, 4 and 5:  high.</p>
     *
     * <p>Tasks with more than 8 bit are handled as one task using 'PumpReadTask16bitOrMore()' and 'PumpWriteTask16bitOrMore()'.
     * The address to put is the one of the 'hi' value. The 'low' values are always on the consecutive addresses. You also
     * need to specify how many bytes the task has (16 bit = 2 bytes, 24 bit = 3 bytes, 32 bit = 4 bytes). The number of
     * bytes is equivalent to the number of addresses a task has (two addresses = one hi, one low; means 16 bit = 2 bytes).
     * Using 'PumpReadTask16bitOrMore()' and 'PumpWriteTask16bitOrMore()' with ’number of bytes = 1’ (=8 bit) is equivalent
     * to using 'PumpReadTask8bit()' and 'PumpWriteTask8bit()'.</p>
     *
     * <p>If the data of a task (read/write) has a unit, the data is automatically converted to the unit of the associated
     * OpenEMS channel, if one is give. If the channel does not have a unit or the channel unit is incompatible with the
     * unit transmitted by Genibus, the fallback code is to convert the data to the base unit (bar, °Celsius, watt, etc.)
     * of the one transmitted by Genibus. There is, however, currently no way to display that unit or an error message.
     *
     * representation (bar, °Celsius, watt, etc.). The unit can not be communicated, so it is not advised to
     * of the associated channel. Data conversion (if applicable) can then only be done correctly if the channel has a unit
     *      * conventions.
     *
     * <p>Data of a task is automatically converted according to the INFO of the task, but also according to OpenEMS
     * conventions. A pressure reading with unit "m" will be converted to "bar" in the OpenEMS channel. Temperature
     * readings will be converted to dC° in the channel. The channel unit is set accordingly.
     * For write tasks that are not boolean (headclass 4 and 5), the unit of INFO is used for the write as well.
     * Example: ref_rem (5, 1)
     * The unit of INFO is %, the range is 0% to 100%. The channel than has values between 0 and 1.0, and for sending a
     * SET with value 100%, write in the "nextWrite" of the channel "1.0".</p>
     *
     * <p>The tasks also allow for an optional "channel multiplier" as the last argument. This is a fixed value that is
     * used as a multiplier when reading a GET and as a divisor when writing a SET.</p>
     *
     * @param deviceId the OpenEMS device id.
     * @param pumpAddress the pump address.
     */
    private void createTaskList(String deviceId, int pumpAddress) {
        // Broadcast mode is just to find the address of a unit. Not suitable for sending commands.
        if (this.broadcast || this.changeAddress) {
            this.pumpDevice = new PumpDevice(deviceId, pumpAddress, 37, Unit.BAR, 4,
                    new PumpReadTask8bit(0, 2, getBufferLengthChannel(), "Standard", Priority.ONCE),
                    new PumpReadTask8bit(0, 3, getUnitBusModeChannel(), "Standard", Priority.ONCE),

                    new PumpReadTask8bit(2, 148, getUnitFamilyChannel(), "Standard", Priority.ONCE),
                    new PumpReadTask8bit(2, 149, getUnitTypeChannel(), "Standard", Priority.ONCE),
                    new PumpReadTask8bit(2, 150, getUnitVersionChannel(), "Standard", Priority.ONCE),

                    new PumpWriteTask8bit(4, 46, getUnitAddrChannel(), "Standard", Priority.HIGH),
                    new PumpWriteTask8bit(4, 47, getGroupAddrChannel(), "Standard", Priority.ONCE)
            );
            this.genibus.addDevice(this.pumpDevice);
            return;
        }

        /* Setup of a multipump system is not possible with genibus.
        if (mpSetup) {
            pumpDevice = new PumpDevice(deviceId, pumpAddress, 4,
                    new PumpReadTask8bit(2, 1, getMultipumpMembers(), "Standard", Priority.HIGH),

                    new PumpWriteTask8bit(4, 45, setMpMasterAddr(), "Standard", Priority.HIGH),
                    new PumpWriteTask8bit(4, 241, setTpMode(), "Standard", Priority.HIGH)
            );
            genibus.addDevice(pumpDevice);
            return;
        }
        */

        // The variable "lowPrioTasksPerCycle" lets you tune how fast the low priority tasks are executed. A higher
        // number means faster execution, up to the same execution speed as high priority tasks.
        // The controller will execute all high and low tasks once per cycle if there is enough time. A reduced execution
        // speed of low priority tasks happens only when there is not enough time.
        // There is also priority once, which as the name implies will be executed just once.
        //
        // What does "lowPrioTasksPerCycle" actually do?
        // Commands are sent to the pump device from a task queue. Each cycle, all the high tasks are added to the queue,
        // plus the amount "lowPrioTasksPerCycle" of low tasks. If the queue is empty before the cycle is finished, as
        // many low tasks as can still fit in this cycle will be executed as well. When all tasks have been executed
        // once this cycle, the controller will idle.
        // If there is not enough time, the execution rate of low tasks compared to high tasks depends on the total
        // amount of low tasks. The fastest execution rate (same as priority high) is reached when "lowPrioTasksPerCycle"
        // equals the total number of low tasks (value is capped at that number).
        // So if there are 10 low tasks and lowPrioTasksPerCycle=10, the low tasks behave like high tasks.
        // If in the same situation lowPrioTasksPerCycle=5, a priority low task is executed at half the rate of a
        // priority high task.
        this.pumpDevice = new PumpDevice(deviceId, pumpAddress, 37, Unit.BAR, 4,


                // Commands.
                // If true is sent to to conflicting channels at the same time (e.g. start and stop), the pump
                // device will act on the command that was sent first. The command list is executed from top to bottom
                // in the order they are listed here.
                new PumpCommandsTask(this.pumpType.getRemote(),
                        getRemoteChannel()),
                new PumpCommandsTask(this.pumpType.getStart(),
                        this.getStartChannel()),
                new PumpCommandsTask(this.pumpType.getStop(),
                        this.getStopChannel()),
                new PumpCommandsTask(this.pumpType.getMinMotorCurve(),
                        getMinMotorCurveChannel()),
                new PumpCommandsTask(this.pumpType.getMaxMotorCurve(),
                        getMaxMotorCurveChannel()),
                new PumpCommandsTask(this.pumpType.getConstFrequency(),
                        getConstFrequencyChannel()),
                new PumpCommandsTask(this.pumpType.getConstPressure(),
                        getConstPressureChannel()),
                new PumpCommandsTask(this.pumpType.getAutoAdapt(),
                        getAutoAdaptChannel()),
                new PumpCommandsTask(121, getWinkOnChannel()),
                new PumpCommandsTask(122, getWinkOffChannel()),

                // Read tasks priority once
                new PumpReadTask8bit(0, 2, getBufferLengthChannel(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(0, 3, getUnitBusModeChannel(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(2, 148, getUnitFamilyChannel(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(2, 149, getUnitTypeChannel(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(2, 150, getUnitVersionChannel(), "Standard", Priority.ONCE),

                // Read tasks priority high
                new PumpReadTask8bit(2, 48, getRefActChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(2, 49, getRefNormChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(2, 90, getControlSourceBitsChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getPloHeadClass(), this.pumpType.getPlo(), getPowerConsumptionChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.gethHeadClass(), this.pumpType.getH(), getPressureChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getqHeadClass(), this.pumpType.getQ(), getPercolationChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.gettWHeadClass(), this.pumpType.gettW(), getPumpedFluidTemperatureChannel(), "Standard", Priority.HIGH),
                // PumpReadTask has an optional channel multiplier. That is a double that is multiplied with the readout
                // value just before it is put in the channel. Here is an example of how to use this feature:
                // Apparently the unit returned by INFO is wrong. Unit type = 30 = 2*Hz, but the value is returned in Hz.
                // Could also be that the error is in the documentation and unit 30 is Hz and not Hz*2.
                new PumpReadTask8bit(this.pumpType.getfActHeadClass(), this.pumpType.getfAct(), getMotorFrequencyChannel(), "Standard", Priority.HIGH, 0.5),
                new PumpReadTask8bit(this.pumpType.getrMinHeadClass(), this.pumpType.getrMin(), getRminChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getrMaxHeadClass(), this.pumpType.getrMax(), getRmaxChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getControlModeHeadClass(), this.pumpType.getControlMode(), getControlModeBitsChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(2, 81, getActMode1BitsChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnCodeHeadClass(), this.pumpType.getWarnCode(), getWarnCodeChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getAlarmCodeHeadClass(), this.pumpType.getAlarmCode(), getAlarmCodeChannel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits1HeadClass(), this.pumpType.getWarnBits1(), getWarnBits1Channel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits2HeadClass(), this.pumpType.getWarnBits2(), getWarnBits2Channel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits3HeadClass(), this.pumpType.getWarnBits3(), getWarnBits3Channel(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits4HeadClass(), this.pumpType.getWarnBits4(), getWarnBits4Channel(), "Standard", Priority.HIGH),

                // Read tasks priority low
                new PumpReadTask8bit(this.pumpType.gethDiffHeadClass(), this.pumpType.gethDiff(), getDiffPressureHeadChannel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(this.pumpType.gettEheadClass(), this.pumpType.gettE(), getElectronicsTemperatureChannel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(this.pumpType.getImoHeadClass(), this.pumpType.getiMo(), getCurrentMotorChannel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 2, getTwinpumpStatusChannel(), "Standard", Priority.LOW),

                new PumpReadTask8bit(this.pumpType.getAlarmCodePumpHeadClass(), this.pumpType.getAlarmCodePump(), getAlarmCodePumpChannel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 163, getAlarmLog1Channel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 164, getAlarmLog2Channel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 165, getAlarmLog3Channel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 166, getAlarmLog4Channel(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 167, getAlarmLog5Channel(), "Standard", Priority.LOW),

                // Config parameters tasks.
                // Class 4 tasks should always be priority high. They have special code to decide if they are sent or not.
                // Since the values in them are static unless changed by the user, they are read once at the start and
                // then only after a write. If a class 4 task is never written to, it essentially behaves like priority
                // once. It should be priority high so any writes are executed immediately.
                new PumpWriteTask8bit(this.pumpType.gethConstRefMaxHeadClass(), this.pumpType.gethConstRefMax(),
                        getHconstRefMaxChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(this.pumpType.gethConstRefMinHeadClass(), this.pumpType.gethConstRefMin(),
                        getHconstRefMinChannel(), "Standard", Priority.HIGH),
                // The channel multiplier is also available for write tasks. For a GET it is a multiplier, for a SET it
                // is a divisor. Apparently all frequencies in the MAGNA3 are off by a factor of 2.
                new PumpWriteTask8bit(4, 30, getFupperChannel(), "Standard", Priority.HIGH, 0.5),
                new PumpWriteTask8bit(4, 34, getFminChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(4, 35, getFmaxChannel(), "Standard", Priority.HIGH),
                // Apparently all frequencies in the MAGNA3 are off by a factor of 2.
                new PumpWriteTask8bit(4, 31, getFnomChannel(), "Standard", Priority.HIGH, 0.5),
                new PumpWriteTask16bitOrMore(2, this.pumpType.gethMaxHiHeadClass(), this.pumpType.gethMaxHi(),
                        getMaxPressureChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask16bitOrMore(2, this.pumpType.getqMaxHiHeadClass(), this.pumpType.getqMaxHi(),
                        getPumpMaxFlowChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(4, 254, getHrangeChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(this.pumpType.getDeltaHheadClass(), this.pumpType.getDeltaH(), getPressureDeltaChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(4, 47, getGroupAddrChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(4, 241, setTpMode(), "Standard", Priority.HIGH),

                // Sensor configuration
                new PumpWriteTask8bit(4, 229, getSensor1FuncChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(4, 226, getSensor1ApplicChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(4, 208, getSensor1UnitChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask16bitOrMore(2, 4, 209, getSensor1MinChannel(), "Standard", Priority.HIGH),
                new PumpWriteTask16bitOrMore(2, 4, 211, getSensor1MaxChannel(), "Standard", Priority.HIGH),

                //new PumpReadTask8bit(2, 127, getSensorGsp(), "Standard", Priority.LOW),
                //new PumpWriteTask8bit(4, 238, setSensorGspFunc(), "Standard", Priority.LOW),

                // Reference values tasks
                new PumpWriteTask8bit(this.pumpType.getRefRemHeadClass(), this.pumpType.getRefRem(),
                        getRefRemChannel(), "Standard", Priority.HIGH),

                // Strings
                new PumpReadTaskAscii(8, getProductNumberChannel(), Priority.ONCE),
                new PumpReadTaskAscii(9, getSerialNumberChannel(), Priority.ONCE)

                /*
                // Multipump commands
                new PumpReadTask8bit(2, 1, getMultipumpMembers(), "Standard", Priority.HIGH),

                new PumpWriteTask8bit(4, 45, setMpMasterAddr(), "Standard", Priority.HIGH)
                // setTpMode() is already in.
                */
        );
        this.genibus.addDevice(this.pumpDevice);
    }

    @Override
    public PumpDevice getPumpDevice() {
        return this.pumpDevice;
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE.equals(event.getTopic())) {
            this.updateChannels();
        }
    }

    // Fill channels with data that are not directly read from the Genibus.
    private void updateChannels() {

        // Get connection status from pump, put it in the channel.
        getConnectionOkChannel().setNextValue(this.pumpDevice.isConnectionOk());

        // Update the read buffer length of the device.
        if (this.doOnce == false && getBufferLengthChannel().value().isDefined()) {
            this.doOnce = true;
            int bufferLength = (int)Math.round(getBufferLengthChannel().value().get());
            this.pumpDevice.setDeviceReadBufferLengthBytes(bufferLength);
        }

        // Parse ControlSource value to a string.
        if (getControlSourceBitsChannel().value().isDefined()) {
            int controlSourceBits = (int)Math.round(getControlSourceBitsChannel().value().get());
            int priorityBits = controlSourceBits & 0b1111;
            int activeSourceBits = controlSourceBits >> 4;
            String source;
            switch (activeSourceBits) {
                case 1:
                    source = "Panel";
                    break;
                case 2:
                    source = "Network (e.g. GENIbus)";
                    break;
                case 3:
                    source = "Handheld device (e.g. GENIlink/GENIair)";
                    break;
                case 4:
                    source = "External input (DI, Limit exceeded, AI Stop)";
                    break;
                case 5:
                    source = "Stop button";
                    break;
                default:
                    source = "unknown";
            }
            getControlSourceStringChannel().setNextValue("Command source: " + source + ", priority: " + priorityBits);


            PumpMode pumpMode = PumpMode.UNKNOWN;
            if (this.isMagna3) {
                // The following code was tested to work with a Magna3 pump, but did not Work with an MGE pump.
                // The MGE has different priority values. At const. press. the Magna3 has priority 10 while the MGE has
                // priority 6. Also, getActualControlModeBits() does not work on the MGE.

                // Parse ActualControlMode value to a string.
                if (getControlModeBitsChannel().value().isDefined()) {
                    int controlModeValue = (int)Math.round(getControlModeBitsChannel().value().get());
                    switch (priorityBits) {
                        case 7:
                            pumpMode = PumpMode.STOP;
                            break;
                        case 8:
                            pumpMode = PumpMode.CONST_FREQU_MAX;
                            break;
                        case 9:
                            pumpMode = PumpMode.CONST_FREQU_MIN;
                            break;
                        default:
                            switch (controlModeValue) {
                                case 0:
                                    pumpMode = PumpMode.CONST_PRESSURE;
                                    break;
                                case 1:
                                    pumpMode = PumpMode.PROP_PRESSURE;
                                    break;
                                case 2:
                                    pumpMode = PumpMode.CONST_FREQU;
                                    break;
                                case 5:
                                    pumpMode = PumpMode.AUTO_ADAPT;
                                    break;
                                case 6:
                                    pumpMode = PumpMode.CONST_TEMP;
                                    break;
                                case 7:
                                    pumpMode = PumpMode.CLOSED_LOOP_SENSOR_CONTROL;
                                    break;
                                case 8:
                                    pumpMode = PumpMode.CONST_FLOW;
                                    break;
                                case 9:
                                    pumpMode = PumpMode.CONST_LEVEL;
                                    break;
                                case 10:
                                    pumpMode = PumpMode.FLOW_ADAPT;
                                    break;
                                case 11:
                                    pumpMode = PumpMode.CONST_DIFF_PRESSURE;
                                    break;
                                case 12:
                                    pumpMode = PumpMode.CONST_DIFF_TEMP;
                                    break;
                            }
                    }
                }
            } else {

                // Parse ActualControlMode value to a string.
                if (getActMode1BitsChannel().value().isDefined()) {
                    int controlModeBits = (int)Math.round(getActMode1BitsChannel().value().get());
                    int operatingModes = controlModeBits & 0b111;
                    int controlModes = (controlModeBits >> 3) & 0b111;
                    boolean testMode = (controlModeBits >> 7) > 0;
                    switch (operatingModes) {
                        case 0:
                            //pumpMode = "Start";   // Don't need that, since there should never be just "start" alone.
                            break;
                        case 1:
                            pumpMode = PumpMode.STOP;
                            break;
                        case 2:
                            pumpMode = PumpMode.CONST_FREQU_MIN;
                            break;
                        case 3:
                            pumpMode = PumpMode.CONST_FREQU_MAX;
                            break;
                        case 7:
                            pumpMode = PumpMode.HAND_MODE;
                            break;
                    }
                    switch (controlModes) {
                        case 0:
                            pumpMode = PumpMode.CONST_PRESSURE;
                            break;
                        case 1:
                            pumpMode = PumpMode.PROP_PRESSURE;
                            break;
                        case 2:
                            pumpMode = PumpMode.CONST_FREQU;
                            break;
                        case 5:
                            pumpMode = PumpMode.AUTO_ADAPT_OR_FLOW_ADAPT;
                            break;
                        case 6:
                            pumpMode = PumpMode.OTHER;
                            break;
                    }
                    if (testMode) {
                        pumpMode = PumpMode.TEST;
                    }
                }
            }
            getControlModeStringChannel().setNextValue(pumpMode.getName());
        }

        // Parse unit family, type and version
        StringBuilder allInfo = new StringBuilder();
        if (getUnitFamilyChannel().value().isDefined()) {
            allInfo.append("Unit family: ");
            int unitFamily = (int)Math.round(getUnitFamilyChannel().value().get());
            switch (unitFamily) {
                case 1:
                    allInfo.append("UPE/MAGNA, ");
                    break;
                case 2:
                    allInfo.append("MGE, ");
                    break;
                case 38:
                    allInfo.append("MAGNA Multi-pump, ");
                    break;
                case 39:
                    allInfo.append("MGE Multi-pump, ");
                    break;
                default:
                    allInfo.append(unitFamily).append(", ");
                    break;
            }
        }
        if (getUnitTypeChannel().value().isDefined()) {
            allInfo.append("Unit type: ");
            int unitType = (int)Math.round(getUnitTypeChannel().value().get());
            switch (unitType) {
                case 10:
                    allInfo.append("MAGNA3, ");
                    break;
                case 7:
                    allInfo.append("MGE model H/I, ");
                    break;
                default:
                    allInfo.append(unitType).append(", ");
                    break;
            }
        }
        if (getUnitVersionChannel().value().isDefined()) {
            allInfo.append("Unit version: ");
            int unitVersion = (int)Math.round(getUnitVersionChannel().value().get());
            switch (unitVersion) {
                case 1:
                    allInfo.append("Naked MGE, ");
                    break;
                case 2:
                    allInfo.append("Multi stage without sensor (CRE), ");
                    break;
                case 3:
                    allInfo.append("Multi stage with sensor (CRE), ");
                    break;
                case 4:
                    allInfo.append("Single stage Series 1000 (LME), ");
                    break;
                case 5:
                    allInfo.append("Single stage Series 2000 (LME), ");
                    break;
                case 6:
                    allInfo.append("Single stage Collect Series 1000 (MGE motor with MAGNA3 hydraulic), ");
                    break;
                case 7:
                    allInfo.append("Single stage Collect Series 2000 (MGE motor with MAGNA3 hydraulic), ");
                    break;
                case 8:
                    allInfo.append("Home booster, ");
                    break;
                default:
                    allInfo.append(unitVersion).append(", ");
                    break;
            }
        }
        if (allInfo.length() > 2) {
            allInfo.delete(allInfo.length() - 2, allInfo.length());
            allInfo.append(".");
            getUnitInfoChannel().setNextValue(allInfo);
        }

        // Parse twinpump status value to a string.
        if (getTwinpumpStatusChannel().value().isDefined()) {
            int twinpumpStatusValue = (int)Math.round(getTwinpumpStatusChannel().value().get());
            String twinpumpStatusString;
            switch (twinpumpStatusValue) {
                case 0:
                    twinpumpStatusString = "Single pump. Not part of a multi pump.";
                    break;
                case 1:
                    twinpumpStatusString = "Twin-pump master. Contact to twin pump slave OK.";
                    break;
                case 2:
                    twinpumpStatusString = "Twin-pump master. No contact to twin pump slave.";
                    break;
                case 3:
                    twinpumpStatusString = "Twin-pump slave. Contact to twin pump master OK.";
                    break;
                case 4:
                    twinpumpStatusString = "Twin-pump slave. No contact to twin pump master.";
                    break;
                case 5:
                    twinpumpStatusString = "Self appointed twin-pump master. No contact to twin pump master.";
                    break;
                default:
                    twinpumpStatusString = "unknown";
            }
            getTwinpumpStatusStringChannel().setNextValue(twinpumpStatusString);
        }

        // Parse twinpump/multipump mode value to a string.
        if (setTpMode().value().isDefined()) {
            int twinpumpModeValue = (int)Math.round(setTpMode().value().get());
            String twinpumpModeString;
            switch (twinpumpModeValue) {
                case 0:
                    twinpumpModeString = "None, not part of a multi pump system.";
                    break;
                case 1:
                    twinpumpModeString = "Time alternating mode.";
                    break;
                case 2:
                    twinpumpModeString = "Load (power) alternating mode.";
                    break;
                case 3:
                    twinpumpModeString = "Cascade control mode.";
                    break;
                case 4:
                    twinpumpModeString = "Backup mode.";
                    break;
                default:
                    twinpumpModeString = "unknown";
            }
            getTpModeString().setNextValue(twinpumpModeString);
        }

        // Parse warn messages and put them all in one channel.
        StringBuilder allErrors = new StringBuilder();
        if (getAlarmCodePump().isDefined()) {   // Manual says this is just for multi pump setups.
            allErrors.append("Alarm code pump: " + getAlarmCodePump().get() + " \n");
        }
        if (getAlarmCode().isDefined()) {
            allErrors.append("Alarm code: " + getAlarmCode().get() + " \n");
        }
        if (getWarnCode().isDefined()) {
            allErrors.append("Warn code: " + getWarnCode().get() + " \n");
        }
        List<String> errorValue;
        if (getWarnBits1Channel().value().isDefined()) {
            int data = (int)Math.round(getWarnBits1Channel().value().get());
            errorValue = this.warnBits.getErrorBits1();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        if (getWarnBits2Channel().value().isDefined()) {
            int data = (int)Math.round(getWarnBits2Channel().value().get());
            errorValue = this.warnBits.getErrorBits2();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        if (getWarnBits3Channel().value().isDefined()) {
            int data = (int)Math.round(getWarnBits3Channel().value().get());
            errorValue = this.warnBits.getErrorBits3();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        if (getWarnBits4Channel().value().isDefined()) {
            int data = (int)Math.round(getWarnBits4Channel().value().get());
            errorValue = this.warnBits.getErrorBits4();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        _setWarnMessage(allErrors.toString());

        // Get warning messages related to genibus.
        String genibusWarningMessage = this.pumpDevice.getAndClearWarningMessage();
        if (genibusWarningMessage.equals("") == false) {
            this.logWarn(this.log, genibusWarningMessage);
        }

        if (this.broadcast) {
            boolean signalReceived = getConnectionOkChannel().value().isDefined() && getConnectionOkChannel().value().get();
            this.logInfo(this.log, "--GENIbus broadcast--");
            if (signalReceived == false) {
                this.logInfo(this.log, "No signal received so far.");
            } else {
                String genibusAddress = "null";
                if (getUnitAddrChannel().value().isDefined()) {
                    genibusAddress = "" + Math.round(getUnitAddrChannel().value().get());
                }
                String groupAddress = "null";
                if (getGroupAddr().isDefined()) {
                    groupAddress = "" + Math.round(getGroupAddr().get());
                }
                String bufferLength = "null";
                if (getBufferLengthChannel().value().isDefined()) {
                    bufferLength = "" + Math.round(getBufferLengthChannel().value().get());
                }
                String busMode = "null";
                if (getUnitBusModeChannel().value().isDefined()) {
                    busMode = "" + Math.round(getUnitBusModeChannel().value().get());
                }

                this.logInfo(this.log, "Pump found - " + getUnitInfoChannel().value().get());
                this.logInfo(this.log, "GENIbus address: " + genibusAddress);
                this.logInfo(this.log, "Group address: " + groupAddress);
                this.logInfo(this.log, "Buffer length: " + bufferLength);
                this.logInfo(this.log, "Bus mode: " + busMode);
            }
        } else if (this.changeAddress) {
            if (this.newAddress > 31 && this.newAddress < 232) {
                try {
                    getUnitAddrChannel().setNextWriteValue(this.newAddress);
                    this.logInfo(this.log, "Pump address changed. New address = " + (Math.round(this.newAddress)) + ".");
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.logError(this.log, "Address change failed!");
                    e.printStackTrace();
                }
            } else {
                this.logError(this.log, "Value for new address = " + (Math.round(this.newAddress)) + " is not in the valid range (32 - 231). "
                        + "Not executing address change!");
            }
            this.changeAddress = false;
        }
    }
}
