package io.openems.edge.bridge.genibus;

import io.openems.edge.bridge.genibus.api.Genibus;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.protocol.ApplicationProgramDataUnit;
import io.openems.edge.bridge.genibus.protocol.Telegram;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a service to communicate with Grundfos pumps using the genibus protocol.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Genibus", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
        })
public class GenibusImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, Genibus {

    @Reference
    ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(GenibusImpl.class);
    private boolean debug;

    private final GenibusWorker worker = new GenibusWorker(this);
    protected int workerStartDelay;
    protected int workerRunIdentifier = 0;
    protected long timestampExecuteWrite;

    private static final int BROADCAST_ADDRESS = 254;

    protected String portName;
    protected boolean connectionOk = true;  // Start with true because this boolean is also used to track if an error message should be sent.
    protected int timeoutIncreaseMs;
    protected static final int GENIBUS_TIMEOUT_MS = 60;
    private Cycle cycle;

    protected ConnectionHandler connectionHandler;

    public GenibusImpl() {
        super(OpenemsComponent.ChannelId.values());
        this.connectionHandler = new ConnectionHandler(this);
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.debug = config.debug();
        this.portName = config.portName();
        this.timeoutIncreaseMs = Math.min(config.timeoutIncreaseMs(), 400);
        this.timeoutIncreaseMs = Math.max(this.timeoutIncreaseMs, 0);
        this.connectionOk = this.connectionHandler.startConnection(this.portName);

        int cycleTime = Cycle.DEFAULT_CYCLE_TIME;
        this.cpm.getAllComponents().stream().filter(component -> component instanceof Cycle).findAny().ifPresent(component -> {
                    this.cycle = (Cycle) component;
                }
        );
        if (this.cycle != null) {
            cycleTime = (this.cycle.getCycleTime());
        }
        if (this.isEnabled()) {
            this.worker.setCycleTime(cycleTime);
            this.worker.activate(config.id());
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.worker.deactivate();
        this.connectionHandler.stop();
    }


    /**
     * Estimate the time in milliseconds it will take to send and receive the answer to the given telegram. This is
     * used to adjust the Genibus timeout parameter based on the telegram size.
     * ’emptyTelegramTime’ is the time to send and receive the answer to an empty telegram. If the telegram is not empty,
     * we can assume that each byte more than an empty telegram (request and answer) increases the time by a fixed amount.
     * This increase in time per byte is called ’millisecondsPerByte’. It is measured and stored in the Genibus device
     * class.
     * The amount of bytes for the request telegram is known. The bytes of the answer telegram however can vary, so only
     * an upper estimate is possible. Because of that, the calculated time is also only an estimate.
     *
     * @param telegram the telegram.
     * @return the time in milliseconds.
     */
    private int estimateTelegramAnswerTime(Telegram telegram) {
        int emptyTelegramTime = telegram.getPumpDevice().getEmptyTelegramTime();
        /* ’telegram.getLength()’ returns 2 for an empty telegram. Subtract 2, because we want the additional bytes
           compared to an empty telegram. */
        int additionalBytesEstimate = Byte.toUnsignedInt(telegram.getLength()) - 2
                + telegram.getAnswerTelegramPduLengthEstimate();
        return (int) (emptyTelegramTime + (additionalBytesEstimate * telegram.getPumpDevice().getMillisecondsPerByte()));
    }

    /**
     * Handles the telegram. This is called by the GenibusWorker forever() method.
     * This method sends the request telegram and processes the response telegram. The request telegram is needed to
     * identify the data in the response. The response has no addresses for the tasks. Instead, data in the response is
     * transmitted in the exact same order as it was requested. The response data is then parsed by using the task list
     * of the request telegram. The method also contains several checks to make sure the response telegram is actually
     * the answer to the request telegram.
     *
     * @param telegram the telegram to send to the Genibus device.
     */
    protected void handleTelegram(Telegram telegram) {

        /* When testing on a raspberry pi I saw weird random connection problems. I guess the pi has occasional hangups
           that cause an execution delay. Adding more time to the timeout solved this problem. Adding 100 ms helped, but
           to eliminate the problem 100% adding 200 ms was needed. Running OpenEMS on a laptop did not have that issue.
           Adjustable by config with parameter ’timeoutIncreaseMs’ */
        int telegramEstimatedTimeMillis = this.estimateTelegramAnswerTime(telegram) + this.timeoutIncreaseMs;

        // Send telegram, response is stored as responseTelegram.
        Telegram responseTelegram = this.connectionHandler.writeTelegram(telegramEstimatedTimeMillis, telegram, this.debug);

        /* No answer received -> error handling
           This will happen if the pump is switched off. Assume that is the case. Reset the device, so once data is
           again received from this address, it is handled like a new pump (which might be the case).
           A reset means all priority once tasks are sent again and all INFO is requested again. So if any of these were
           in this failed telegram, they are not omitted. */
        if (responseTelegram == null) {
            this.logWarn(this.log, "No answer on GENIbus from device " + telegram.getPumpDevice().getPumpDeviceId());
            telegram.getPumpDevice().setConnectionOk(false);
            telegram.getPumpDevice().resetDevice();
            return;
        }

        /* Store the length of the response telegram in the request telegram. The value is stored in the request telegram,
           because the GenibusWorker has access to that, but not the actual response telegram. Together with the
           transmission time, the GenibusWorker can use that value to calculate ’millisecondsPerByte’ and store it in
           PumpDevice.java.*/
        telegram.setAnswerTelegramPduLength(responseTelegram.getLength() - 2);

        int requestTelegramAddress = Byte.toUnsignedInt(telegram.getDestinationAddress());
        int responseTelegramAddress = Byte.toUnsignedInt(responseTelegram.getSourceAddress());

        // Broadcast (= request address 254) will have different address in response. Let that through.
        if (requestTelegramAddress != responseTelegramAddress && requestTelegramAddress != BROADCAST_ADDRESS) {
            this.logWarn(this.log, "Telegram address mismatch! Request telegram was sent to address " + requestTelegramAddress
                    + ", but response telegram is from address " + responseTelegramAddress + ". This is not the right response telegram. Cannot process, resetting connection.");
            telegram.getPumpDevice().setConnectionOk(false);
            telegram.getPumpDevice().resetDevice();
            return;
        }

        List<ApplicationProgramDataUnit> responseApdu = responseTelegram.getProtocolDataUnit().getApplicationProgramDataUnitList();

        // Check if the response telegram has the same number of apdu as the sent telegram. If not, something went wrong.
        if (responseApdu.size() != telegram.getTelegramTaskList().size()) {
            this.logWarn(this.log, "Telegram apdu list mismatch! Number of sent apdu: " + telegram.getTelegramTaskList().size()
                    + ", received apdu: " + responseApdu.size() + ". This is not the right response telegram. Cannot process, resetting connection.");
            telegram.getPumpDevice().setConnectionOk(false);
            telegram.getPumpDevice().resetDevice();
            return;
        }

        telegram.getPumpDevice().setConnectionOk(true);
        telegram.getPumpDevice().setConnectionTimeoutCounter(0);

        //if (debug) { this.logInfo(this.log, "--Reading Response--"); }

        AtomicInteger listCounter = new AtomicInteger();
        listCounter.set(0);
        telegram.getTelegramTaskList().forEach((key, taskList) -> {
            int requestHeadClass = key / 100;
            int answerHeadClass = responseApdu.get(listCounter.get()).getHeadClass();
            int answerAck = responseApdu.get(listCounter.get()).getHeadOsAckForRequest();

            if (requestHeadClass != answerHeadClass) {
                this.logWarn(this.log, "Telegram mismatch! Wrong apdu Head Class: sent " + requestHeadClass
                        + ", received " + answerHeadClass + ". This is not the right response telegram. Cannot process, resetting connection.");
                telegram.getPumpDevice().setConnectionOk(false);
                telegram.getPumpDevice().resetDevice();
                return;
            }
            // answerAck is an error code. answerAck == 0 means ’everything ok’.
            if (answerAck != 0) {
                // Parse answerAck error code.
                switch (answerAck) {
                    case 1:
                        this.logWarn(this.log, "Apdu error for Head Class " + answerHeadClass
                                + ": Data Class unknown, reply APDU data field is empty");
                        break;
                    case 2:
                        byte[] data = responseApdu.get(listCounter.get()).getCompleteApduAsByteArray(this.log);
                        this.logWarn(this.log, "Apdu error for Head Class " + answerHeadClass
                                + ": Data Item ID unknown, reply APDU data field contains first unknown ID - "
                                + Byte.toUnsignedInt(data[2]));
                        break;
                    case 3:
                        this.logWarn(this.log, "Apdu error for Head Class " + answerHeadClass
                                + ": Operation illegal or Data Class write buffer is full, APDU data field is empty");
                        break;
                }
            } else {
                byte[] data = responseApdu.get(listCounter.get()).getCompleteApduAsByteArray(this.log);

                // For the GenibusTask list --> index
                int taskCounter = 0;

                // On correct position get the header.
                List<ApplicationProgramDataUnit> requestApdu = telegram.getProtocolDataUnit().getApplicationProgramDataUnitList();
                int requestOs = requestApdu.get(listCounter.get()).getHeadOsAckForRequest();

                //if (debug) { this.logInfo(this.log, "Apdu " + (listCounter.get() + 1) + ", Apdu byte number: " + data.length + ", Apdu identifier: " + key + ", requestOs: " + requestOs + ", Tasklist length: " + taskList.size()); }

                if (requestOs != 2) {   // 2 = SET, contains no data in reply.
                    /*
                    if (debug) {
                        this.logInfo(this.log, "" + Byte.toUnsignedInt(data[0]));
                        this.logInfo(this.log, "" + Byte.toUnsignedInt(data[1]));
                    }
                    */
                    for (int byteCounter = 2; byteCounter < data.length; ) {
                        //if (debug) { this.logInfo(this.log, "" + Byte.toUnsignedInt(data[byteCounter])); }

                        GenibusTask geniTask = taskList.get(taskCounter);

                        // Read ASCII. Just one ASCII task per apdu.
                        if (geniTask.getHeader() == 7) {
                            for (int i = 2; i < data.length; i++) {
                                geniTask.processResponse(data[i]);
                            }
                            break;
                        }

                        if (requestOs == 3) {
                            //vi bit 4
                            int vi = (data[byteCounter] & 0x10);
                            //bo bit 5
                            int bo = (data[byteCounter] & 0x20);
                            //sif on bit 0 and 1
                            int sif = (data[byteCounter] & 0x03);
                            //only 1 byte of data
                            if (sif == 0 || sif == 1) { // No UNIT, ZERO and RANGE for sif 1 and 2, so INFO is just 1 byte.
                                geniTask.setOneByteInformation(vi, bo, sif);
                                // Tasks with more than 8 bit still only need 1 byte INFO.
                                byteCounter += 1;
                            } else {    // Full 4 byte INFO
                                /* Multi byte tasks have a 4 byte INFO for the hi byte and 1 byte info for the following lo bytes.
                                   However, the INFO for the lo bytes is not needed. It just says it is a lo byte, but we know that already.
                                   All the information needed is in INFO of the hi byte, so only that is requested. */
                                if (byteCounter >= data.length - 3) {
                                    this.logWarn(this.log, "Incorrect Data Length to SIF-->prevented Out of Bounds Exception");
                                    break;
                                }
                                geniTask.setFourByteInformation(vi, bo, sif,
                                        data[byteCounter + 1], data[byteCounter + 2], data[byteCounter + 3]);
                                // Full INFO is 4 bytes, so increase by 4.
                                byteCounter += 4;
                            }
                            if (this.debug) {
                                this.logInfo(this.log, geniTask.printInfo());
                            }
                        } else {
                            // If task is more than 8 bit, read more than one byte.
                            int byteAmount = geniTask.getDataByteSize();
                            if (byteCounter >= data.length - (byteAmount - 1)) {
                                this.logWarn(this.log, "Error reading data from response telegram. Apdu does not contain the expected number of bytes.");
                                break;
                            }
                            for (int i = 0; i < byteAmount; i++) {
                                geniTask.processResponse(data[byteCounter]);
                                byteCounter++;
                            }
                        }
                        taskCounter++;
                        if (taskList.size() <= taskCounter) {
                            break;
                        }
                    }
                }
            }
            listCounter.getAndIncrement();
        });
    }


    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE.equals(event.getTopic())) {
            this.timestampExecuteWrite = System.currentTimeMillis();
            this.workerStartDelay = 0;
            this.workerRunIdentifier++;
            if (this.workerRunIdentifier > 10) {
                this.workerRunIdentifier = 0;
            }
            this.worker.triggerNextRun();

            /* The execution of one iteration of the GenibusWorker's forever() method may take longer than one OpenEMS
               cycle, especially if a Genibus device is not responding and the code has to wait for the answer timeout.
               The GenibusWorker is an AbstractCycleWorker, and as such calling triggerNextRun() will only start a new
               run if the forever() method is finished when triggerNextRun() is called. If the GenibusWorker is not
               finished on the first call of triggerNextRun(), later calls are added to allow a delayed start and keep
               the worker from idling. Otherwise, if it barely missed the first triggerNextRun() it would idle the rest
               of the cycle. */
            int delayedStartLoopCounter = 4 + (this.timeoutIncreaseMs / 50);
            for (int i = 0; i < delayedStartLoopCounter; i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (this.workerRunIdentifier == this.worker.getRunIdentifier()) {
                    /* The worker collects the value of ’workerRunIdentifier’ at the start of it's run. Compare value to
                       see if the next run has started or not. Exit loop if run has started.*/
                    break;
                }
                this.workerStartDelay += 50;
                this.worker.triggerNextRun();
            }
        }
    }


    /**
     * Add a device to the Genibus bridge.
     * @param pumpDevice the device.
     */
    @Override
    public void addDevice(PumpDevice pumpDevice) {
        this.worker.addDevice(pumpDevice);
    }

    /**
     * Remove a device from the Genibus bridge.
     * @param deviceId the device.
     */
    @Override
    public void removeDevice(String deviceId) {
        this.worker.removeDevice(deviceId);
    }

    /**
     * Log a debug message to the Genibus bridge.
     * @param log     the Logger instance.
     * @param message the message.
     */
    @Override
    public void logDebug(Logger log, String message) {
        super.logDebug(log, message);
    }

    /**
     * Log a info message to the Genibus bridge.
     * @param log     the Logger instance.
     * @param message the message.
     */
    @Override
    public void logInfo(Logger log, String message) {
        super.logInfo(log, message);
    }

    /**
     * Log a warn message to the Genibus bridge.
     * @param log     the Logger instance.
     * @param message the message.
     */
    @Override
    public void logWarn(Logger log, String message) {
        super.logWarn(log, message);
    }

    /**
     * Log an error message to the Genibus bridge.
     * @param log     the Logger instance.
     * @param message the message.
     */
    @Override
    public void logError(Logger log, String message) {
        super.logError(log, message);
    }

    /**
     * If debug mode is enabled or not.
     * @return the debug boolean
     */
    public boolean getDebug() {
        return this.debug;
    }
}
