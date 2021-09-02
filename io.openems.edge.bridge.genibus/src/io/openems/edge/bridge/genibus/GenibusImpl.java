package io.openems.edge.bridge.genibus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.genibus.api.Genibus;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.protocol.ApplicationProgramDataUnit;
import io.openems.edge.bridge.genibus.protocol.Telegram;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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


    private final Logger log = LoggerFactory.getLogger(GenibusImpl.class);
    private boolean debug;

    private final GenibusWorker worker = new GenibusWorker(this);

    protected String portName;
    protected boolean connectionOk = true;  // Start with true because this boolean is also used to track if an error message should be sent.

    protected ConnectionHandler connectionHandler;

    public GenibusImpl() {
        super(OpenemsComponent.ChannelId.values());
        this.connectionHandler = new ConnectionHandler(this);
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.debug = config.debug();
        this.portName = config.portName();
        this.connectionOk = this.connectionHandler.startConnection(this.portName);
        if (this.isEnabled()) {
            this.worker.activate(config.id());
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.worker.deactivate();
        this.connectionHandler.stop();
    }


    /**
     * Handles the telegram. This is called by the GenibusWorker forever() method.
     * This method sends the telegram (request) and processes the response telegram. The request
     * telegram is needed to identify the data in the response. The response has no addresses for the
     * tasks. Instead, data in the response is transmitted in the exact same order as it was requested.
     * The response data is then parsed by using the task list of the request telegram.
     * The method also contains several checks to make sure the response telegram is actually the
     * answer to the request telegram.
     * The cycletimeLeft parameter is used to calculate how long to wait for the response telegram before
     * timeout. This is important in a situation with multiple pumps where one or more pumps are not
     * responding. The timeout can not be too large, or the not responding pumps block operation of the
     * other pumps. If the timeout is too short, connection errors may happen during normal operation.
     *
     * @param telegram   telegram created beforehand.
     * @param cycletimeLeft     remaining time in ms of this OpenEMS cycle.
     *
     */
    protected void handleTelegram(Telegram telegram, long cycletimeLeft) {

        int emptyTelegramTime = telegram.getPumpDevice().getEmptyTelegramTime();
        int telegramByteLength = Byte.toUnsignedInt(telegram.getLength()) - 2 // Subtract crc
                + telegram.getAnswerTelegramLength() - 2;   // Stored value in answerTelegramLength is the estimated upper limit, not the actual value.

        // The time parameter in the "writeTelegram()" method is the timeout until the first bytes of a response, and
        // then again the timeout for the transmission of the answer. Tests have shown that the time until the first byte
        // of the answer is detected is much longer than the time from then to the end of the transmission, probably
        // because of buffering. For example a 109 ms telegram had 94 ms on the answer time clock and 10 ms on the
        // transmission time clock.
        // Judging from these tests, it seems best just to take the whole estimated telegram time as the timeout for the
        // answer time clock, + 60 ms (GENIbus timeout length, added in "handleResponse()" method). The same time is
        // then also used as timeout for the transmission time clock. Not accurate, but good enough. Just to have a not
        // too long timer that scales with the telegram length.
        int telegramEstimatedTimeMillis = (int) (emptyTelegramTime + telegramByteLength * telegram.getPumpDevice().getMillisecondsPerByte());

        // When testing on the leaflet I saw weird random connection problems. I guess the leaflet has hangups that
        // cause an execution delay. Adding more time to the timeout to counter this problem. Adding just 100 is not enough.
        if (cycletimeLeft > telegramEstimatedTimeMillis + 180) {
            telegramEstimatedTimeMillis += 100;
        }
        // Add even more time if there is enough time left in the cycle.
        if (cycletimeLeft > telegramEstimatedTimeMillis + 180) {
            telegramEstimatedTimeMillis += 100;
        }
        Telegram responseTelegram = this.connectionHandler.writeTelegram(telegramEstimatedTimeMillis, telegram, this.debug);

        // No answer received -> error handling
        // This will happen if the pump is switched off. Assume that is the case. Reset the device, so once data is
        // again received from this address, it is handled like a new pump (which might be the case).
        // A reset means all priority once tasks are sent again and all INFO is requested again. So if any of these were
        // in this failed telegram, they are not omitted.
        if (responseTelegram == null) {
            this.logWarn(this.log, "No answer on GENIbus from device " + telegram.getPumpDevice().getPumpDeviceId());
            telegram.getPumpDevice().setConnectionOk(false);
            telegram.getPumpDevice().resetDevice();
            return;
        }

        telegram.setAnswerTelegramLength(responseTelegram.getLength());

        int requestTelegramAddress = Byte.toUnsignedInt(telegram.getDestinationAddress());
        int responseTelegramAddress = Byte.toUnsignedInt(responseTelegram.getSourceAddress());

        // Broadcast (= request address 254) will have different address in response. Let that through.
        if (requestTelegramAddress != responseTelegramAddress && requestTelegramAddress != 254) {
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
        telegram.getPumpDevice().setTimeoutCounter(0);

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
            if (answerAck != 0) {
                switch (answerAck) {
                    case 1:
                        this.logWarn(this.log, "Apdu error for Head Class " + answerHeadClass
                                + ": Data Class unknown, reply APDU data field is empty");
                        break;
                    case 2:
                        byte[] data = responseApdu.get(listCounter.get()).getCompleteApduAsByteArray();
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
                byte[] data = responseApdu.get(listCounter.get()).getCompleteApduAsByteArray();

                //for the GenibusTask list --> index
                int taskCounter = 0;

                // on correct position get the header.
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
                            if (sif == 0 || sif == 1) {
                                geniTask.setOneByteInformation(vi, bo, sif);
                                // Support for 16bit tasks
                                byteCounter += geniTask.getDataByteSize();
                                //only 4byte data
                            } else {
                                // Multi byte tasks have a 4 byte INFO for the hi byte and 1 byte info for the folllowing lo bytes
                                if (byteCounter >= data.length - 2 - geniTask.getDataByteSize()) {
                                    this.logWarn(this.log, "Incorrect Data Length to SIF-->prevented Out of Bounds Exception");
                                    break;
                                }
                                geniTask.setFourByteInformation(vi, bo, sif,
                                        data[byteCounter + 1], data[byteCounter + 2], data[byteCounter + 3]);
                                //bc of 4 byte data additional 3 byte incr. (or more for 16+ bit tasks)
                                byteCounter += 3 + geniTask.getDataByteSize();
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
            this.worker.triggerNextRun();
        }
    }


    /**
     * Adds a pumpDevice to the GENIbus.
     * @param pumpDevice the PumpDevice object.
     */
    @Override
    public void addDevice(PumpDevice pumpDevice) {
        this.worker.addDevice(pumpDevice);
    }

    @Override
    public void removeDevice(String deviceId) {
        this.worker.removeDevice(deviceId);
    }

    @Override
    public void logDebug(Logger log, String message) {
        super.logDebug(log, message);
    }

    @Override
    public void logInfo(Logger log, String message) {
        super.logInfo(log, message);
    }

    @Override
    public void logWarn(Logger log, String message) {
        super.logWarn(log, message);
    }

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
