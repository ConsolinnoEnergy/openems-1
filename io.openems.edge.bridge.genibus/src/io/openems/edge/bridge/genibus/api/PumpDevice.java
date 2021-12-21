package io.openems.edge.bridge.genibus.api;

import io.openems.common.channel.Unit;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.api.task.HeadClass4and5;
import io.openems.edge.common.taskmanager.TasksManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the representation of a Genibus device for the Genibus bridge. It holds all the necessary information
 * about the device like the address, buffer size, connection status, etc. Most importantly, it holds a list of all the
 * data items (= Genibus tasks) the bridge should request from the device.
 */

public class PumpDevice {

    /**
     * The Parent device id.
     */
    private final String parentDeviceId;

    /**
     * TaskManager that contains all Genibus tasks.
     */
    private final TasksManager<GenibusTask> taskManager = new TasksManager<>();

    /**
     * The Genibus task queue. The Genibus bridge processes tasks in the order in which they appear in this queue.
     */
    private final List<GenibusTask> taskQueue = new ArrayList<>();

    /**
     * The list of all priority ’once’ Genibus tasks that can do ’INFO’. These tasks need to be executed two times, once
     * to do INFO and once to do GET.
     */
    private final List<GenibusTask> onceTasksWithInfo = new ArrayList<>();

    /**
     * The read buffer size in bytes of the device. A request telegram cannot be larger than this.
     * The value ’70’ is from a Magna3 pump. I assume this is the lower limit, since that doesn't even fit a full APDU.
     * A telegram with one full APDU is 71 bytes (telegram header 4, CRC 2, APDU header 2, APDU max data 63).
     * The value can be requested over Genibus from the device, buf_len (0, 2). The OpenEMS module for the device should
     * read buf_len and then write that value to this class using ’setDeviceReadBufferLengthBytes()’.
     */
    private int deviceReadBufferLengthBytes = 70;

    /**
     * The send buffer size in bytes of the device. A response telegram cannot be larger than this.
     * The value ’102’ is from a Magna3 pump, found by trial and error. So far no Genibus data item exists to read this
     * value from the device. Because of that there is no setter method to change this value. Instead, it is assumed
     * that the send buffer will not be smaller than the read buffer, and not smaller than the 102 bytes of the Magna3.
     * If the read buffer value is changed to a value bigger than 102, the send buffer will be increased to the same
     * value.
     */
    private int deviceSendBufferLengthBytes = 102;

    /**
     * The variable ’lowPrioTasksPerCycle’ lets you tune how fast the ’low’ priority tasks are executed, compared to the
     * ’high’ tasks. A higher number means faster execution, up to the same execution speed as high priority tasks.
     * Execution speed is ’lowPrioTasksPerCycle’ divided by the total amount of ’low’ tasks. If there are 4 ’low’ tasks,
     * setting ’lowPrioTasksPerCycle=2’ will mean ’low’ tasks execute at half the rate as ’high’ tasks.
     * The controller will execute all ’high’ and ’low’ tasks once per cycle if there is enough time. A reduced execution
     * speed of ’low’ priority tasks happens only when there is not enough time.
     *
     * <p>In detail: Commands are sent to the pump device from a task queue. Task priority decides how this queue is filled.
     * When the queue is empty, all ’high’ tasks are added, plus the amount ’lowPrioTasksPerCycle’ of ’low’ tasks. This
     * is the ’standard’ refill. The queue is refilled so that tasks will not execute more than once per cycle. If the
     * ’standard’ refill has executed and there is still enough time in the cycle, the queue will fill with tasks that
     * were not already in the ’standard’ refill, i.e. all the other ’low’ tasks. After that, the controller will idle.</p>
     */
    private final int lowPrioTasksPerCycle;

    /**
     * The parameter ’millisecondsPerByte’ together with ’emptyTelegramTime’ is used to estimate the time an exchange of
     * telegrams (request and response) for this device will take. The parameter ’emptyTelegramTime’ measures how long
     * an exchange of empty telegrams takes, in milliseconds. The parameter ’millisecondsPerByte’ then indicates how much
     * each byte added to the request or response telegram increases that exchange time, again in milliseconds.
     * Both parameters are measured during runtime. To smooth out variation, they store multiple measured values in an
     * array. The return value is then the average of the array.
     *
     * <p>The formula for the telegram exchange time is then:
     * emptyTelegramTime + additionalByteEstimate * millisecondsPerByte
     * An empty telegram has a PDU of size 0 bytes. ’additionalByteEstimate’ is then the PDU size of the request telegram,
     * + the estimated PDU size of the response telegram. ’emptyTelegramTime’ and ’millisecondsPerByte’ should be constant,
     * so the time of a telegram exchange can be calculated from the byte count in the PDUs.</p>
     *
     * <p>This timing information is used for two things:
     * - Adjusting the telegram response timeout to the telegram size. The timeout length cannot be too long, otherwise
     *   one pump that is not responding (because of power outage, error, wrong address, etc.) will severely slow down
     *   communication to other pumps on the same Genibus bridge.
     * - Genibus & OpenEMS cycle synchronisation. Near the end of the OpenEMS cycle, the length of a telegram will be
     *   limited so that the request/response exchange is done before the cycle ends.</p>
     */
    private final double[] millisecondsPerByte = {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0};    // Rough estimate as initial value. Exact value measured at runtime.
    private int arrayTracker = 0;   // Used to track in which array position the last measurement was put.

    /* Value measured with a laptop is 33 ms. Leaflet timing seems to be slower, so setting a more conservative 40 ms as
       initial value. Exact value measured at runtime. */
    private final int[] emptyTelegramTime = {40, 40, 40, 40, 40};
    private int arrayTracker2 = 0;   // Used to track in which array position the last measurement was put.

    /**
     * If the pump is in pressure set point mode, ref_rem is mapped to the pressure sensor range. ref_rem is a % value,
     * so to calculate the actual pressure set point the range of the pressure sensor needs to be known. The pressure
     * sensor has a Genibus data item, and calling INFO on that data item gives the pressure sensor range. For the pumps
     * MGE and Magna3 the data item h (2, 37) works. The Magna3 can also use h_diff (2, 23). It is assumed the data item
     * is head class 2, so ’pressureSensorTaskAddress’ is the ID (also called task address in OpenEMS).
     * The response to INFO on that data item is then stored in ’pressureSensorMin’ and ’pressureSensorRange', converted
     * to the OpenEMS unit specified by ’sensorUnit’.
     * ’ref_rem=0%’ is then ’pressureSensorMin’, while ’ref_rem=100%’ is ’pressureSensorMin + pressureSensorRange’.
     */
    private final int pressureSensorTaskAddress;
    private final Unit sensorUnit;
    private double pressureSensorMin = 0;
    private double pressureSensorRange = 0;

    private final int genibusAddress;
    private boolean connectionOk = true;    // Initialize with true, to avoid "no connection" message on startup.
    private long timestamp;    // Used to track when the last telegram was sent to this device.
    private boolean allLowPrioTasksAdded;
    private boolean addAllOnceTasks = true;

    private boolean firstTelegram = true;
    private boolean emptyTelegramSent = false;
    private int connectionTimeoutCounter = 0;
    private String warningMessage;

    /**
     * Initialize the pump device object. The main purpose of this class is to hold a list of all the tasks the Genibus
     * bridge should request from the device.
     *
     * @param parentDeviceId the device ID of the OpenEMS module creating this PumpDevice
     * @param genibusAddress the Genibus address of the target device
     * @param pressureSensorTaskAddress the ID (also called task address in OpenEMS) of the pressure sensor Genibus data
     *                                  item. For pumps MGE and Magna3, 37 works.
     * @param sensorUnit the OpenEMS Unit you want ’pressureSensorMin’ and ’pressureSensorRange’ to have.
     * @param lowPrioTasksPerCycle this parameter lets you tune how fast the low priority tasks are executed, compared
     *                             to high priority tasks. A higher number means faster execution.
     * @param tasks the Genibus tasks.
     */
    public PumpDevice(String parentDeviceId, int genibusAddress, int pressureSensorTaskAddress, Unit sensorUnit, int lowPrioTasksPerCycle, GenibusTask... tasks) {
        this.genibusAddress = genibusAddress;
        this.pressureSensorTaskAddress = pressureSensorTaskAddress;
        this.sensorUnit = sensorUnit;
        this.parentDeviceId = parentDeviceId;
        this.lowPrioTasksPerCycle = lowPrioTasksPerCycle;
        for (GenibusTask task : tasks) {
            this.addTask(task);
        }
        this.timestamp = System.currentTimeMillis() - 1000;
    }

    /**
     * Add a Genibus task for this device.
     *
     * @param task a Genibus task.
     */
    public void addTask(GenibusTask task) {
        task.setGenibusDevice(this);
        this.taskManager.addTask(task);
    }

    /**
     * Get the Genibus task manager. It contains all the Genibus tasks for this device.
     *
     * @return the task manager.
     */
    public TasksManager<GenibusTask> getTaskManager() {
        return this.taskManager;
    }

    /**
     * Get the Genibus task queue. The Genibus bridge processes tasks in the order in which they appear in this queue.
     *
     * @return the task queue.
     */
    public List<GenibusTask> getTaskQueue() {
        return this.taskQueue;
    }

    /**
     * Get all priority ’once’ Genibus tasks that can do ’INFO’. These tasks need to be executed two times, once to do
     * INFO and once to do GET.
     *
     * @return a list of all priority ’once’ tasks that can do INFO.
     */
    public List<GenibusTask> getOnceTasksWithInfo() {
        return this.onceTasksWithInfo;
    }

    /**
     * Set the device read buffer length. Can only set values above 70, which is assumed is the minimum value a device
     * can have. The lower limit is a safeguard to not be able to kill Genibus communications by setting the buffer size
     * too low.
     * This will also set the device send buffer length to the same value, if the value is above 102. It is reasonable
     * to assume the send buffer won't be smaller than the read buffer. The send buffer does not have it's own setter
     * method.
     *
     *
     * @param value the device read buffer length.
     */
    public void setDeviceReadBufferLengthBytes(int value) {
        // 70 is assumed minimum buffer length.
        if (value >= 70) {
            this.deviceReadBufferLengthBytes = value;
        }
        // Adjust send buffer as well.
        this.deviceSendBufferLengthBytes = Math.max(value, 102);
    }

    /**
     * Gets the read buffer length (number of bytes) of this Genibus device. The read buffer length is the maximum length
     * a telegram can have that is sent to this device. If this buffer overflows, the device won't answer.
     *
     * @return the read buffer length (number of bytes).
     */
    public int getDeviceReadBufferLengthBytes() {
        return this.deviceReadBufferLengthBytes;
    }

    /**
     * Gets the send buffer length (number of bytes) of this Genibus device. The send buffer length is the maximum length
     * a telegram can have that the device sends as a response telegram. This buffer can overflow when tasks are requested
     * that have more bytes in the response than in the request, such as INFO and ASCII.
     *
     * @return the send buffer length (number of bytes).
     */
    public int getDeviceSendBufferLengthBytes() {
        return this.deviceSendBufferLengthBytes;
    }

    /**
     * Get the Genibus address of this device.
     *
     * @return the Genibus address.
     */
    public int getGenibusAddress() {
        return this.genibusAddress;
    }

    /**
     * Get the number of low priority tasks to send per cycle. This value is only relevant when not all tasks can be
     * executed in one cycle.
     *
     * @return the number of low priority tasks to send per cycle.
     */
    public int getLowPrioTasksPerCycle() {
        return this.lowPrioTasksPerCycle;
    }

    /**
     * Get the OpenEMS device id of the module that created this PumpDevice.
     *
     * @return the pump device id.
     */
    public String getPumpDeviceId() {
        return this.parentDeviceId;
    }

    /**
     * Set a timestamp for this pump device. This is done once per cycle when the first telegram is sent to this device.
     * This information is used to track if a telegram has already been sent to this device this cycle.
     */
    public void setTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the last timestamp of this pump device.
     * This information is used to track if a telegram has already been sent to this device this cycle.
     *
     * @return the last timestamp of this pump device.
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * This method stores measured values for the ’millisecondsPerByte’ parameter, which is used to calculate telegram
     * request/response timing. The time it takes to send a telegram and receive the response is:
     * 'emptyTelegramTime + PduBytes * millisecondsPerByte'
     * ’emptyTelegramTime’ is the time in ms it takes to send and receive the response to an empty telegram (=PDU size 0
     * bytes). 'PduBytes' is then the combined byte count of the request and response telegram PDUs. ’emptyTelegramTime’
     * and ’millisecondsPerByte’ should be constant. If these are known, the time of a telegram exchange can be calculated
     * from the byte count in the PDUs.
     * Multiple measurements of ’millisecondsPerByte’ are stored in an array. Calling the value returns an average of
     * these measurements, to smooth out erratic measurement data. The measurement data is also clamped to the [0.5, 10]
     * interval, as a safeguard from runaway values rendering the Genibus bridge inoperable.
     *
     * @param millisecondsPerByte the number of milliseconds one byte adds to the telegram send and receive time.
     */
    public void setMillisecondsPerByte(double millisecondsPerByte) {
        // Save seven values so we can average and the value won't jump that much.
        if (millisecondsPerByte > 10) {
            millisecondsPerByte = 10;   // Failsafe.
        }
        if (millisecondsPerByte < 0.5) {
            millisecondsPerByte = 0.5;   // Failsafe.
        }
        this.millisecondsPerByte[this.arrayTracker] = millisecondsPerByte;
        this.arrayTracker++;
        int arrayLength = this.millisecondsPerByte.length;
        if (this.arrayTracker >= arrayLength) {
            this.arrayTracker = 0;
        }
    }

    /**
     * Gets the average value of measured values for the ’millisecondsPerByte’ parameter, which is used to calculate
     * telegram request/response timing. The time it takes to send a telegram and receive the response is:
     * 'emptyTelegramTime + PduBytes * millisecondsPerByte'
     * ’emptyTelegramTime’ is the time in ms it takes to send and receive the response to an empty telegram (=PDU size 0
     * bytes). 'PduBytes' is then the combined byte count of the request and response telegram PDUs. ’emptyTelegramTime’
     * and ’millisecondsPerByte’ should be constant. If these are known, the time of a telegram exchange can be calculated
     * from the byte count in the PDUs.
     * Multiple measurements of ’millisecondsPerByte’ are stored in an array. Calling the value returns an average of
     * these measurements, to smooth out erratic measurement data.
     *
     * @return the number of milliseconds one byte adds to the telegram send and receive time.
     */
    public double getMillisecondsPerByte() {
        double returnValue = 0;
        int arrayLength = this.millisecondsPerByte.length;
        for (double v : this.millisecondsPerByte) {
            returnValue += v;
        }
        return returnValue / arrayLength;   // Average
    }

    /**
     * This method stores measured values for the ’emptyTelegramTime’ parameter, which is used to calculate telegram
     * request/response timing. The time it takes to send a telegram and receive the response is:
     * 'emptyTelegramTime + PduBytes * millisecondsPerByte'
     * ’emptyTelegramTime’ is the time in ms it takes to send and receive the response to an empty telegram (=PDU size 0
     * bytes). 'PduBytes' is then the combined byte count of the request and response telegram PDUs. ’emptyTelegramTime’
     * and ’millisecondsPerByte’ should be constant. If these are known, the time of a telegram exchange can be calculated
     * from the byte count in the PDUs.
     * Multiple measurements of ’emptyTelegramTime’ are stored in an array. Calling the value returns an average of
     * these measurements, to smooth out erratic measurement data. The measurement data is also clamped to the [10, 100]
     * interval, as a safeguard from runaway values rendering the Genibus bridge inoperable.
     *
     * @param emptyTelegramTime the time in milliseconds it takes to send and receive the response to an empty telegram.
     */
    public void setEmptyTelegramTime(int emptyTelegramTime) {
        if (emptyTelegramTime > 100) {
            emptyTelegramTime = 100;   // Failsafe.
        }
        if (emptyTelegramTime < 10) {
            emptyTelegramTime = 10;   // Failsafe.
        }
        this.emptyTelegramTime[this.arrayTracker2] = emptyTelegramTime;
        this.arrayTracker2++;
        int arrayLength = this.emptyTelegramTime.length;
        if (this.arrayTracker2 >= arrayLength) {
            this.arrayTracker2 = 0;
        }
    }

    /**
     * Gets the average value of measured values for the ’emptyTelegramTime’ parameter, which is used to calculate
     * telegram request/response timing. The time it takes to send a telegram and receive the response is:
     * 'emptyTelegramTime + PduBytes * millisecondsPerByte'
     * ’emptyTelegramTime’ is the time in ms it takes to send and receive the response to an empty telegram (=PDU size 0
     * bytes). 'PduBytes' is then the combined byte count of the request and response telegram PDUs. ’emptyTelegramTime’
     * and ’millisecondsPerByte’ should be constant. If these are known, the time of a telegram exchange can be calculated
     * from the byte count in the PDUs.
     * Multiple measurements of ’emptyTelegramTime’ are stored in an array. Calling the value returns an average of
     * these measurements, to smooth out erratic measurement data.
     *
     * @return the time it takes to send and receive the response to an empty telegram.
     */
    public int getEmptyTelegramTime() {
        int returnValue = 0;
        int arrayLength = this.emptyTelegramTime.length;
        for (int j : this.emptyTelegramTime) {
            returnValue += j;
        }
        return returnValue / arrayLength;   // Average
    }

    /**
     * If an empty telegram been sent to this device, this cycle.
     *
     * @return true for yes and false for no.
     */
    public boolean isEmptyTelegramSent() {
        return this.emptyTelegramSent;
    }

    /**
     * Set the ’emptyTelegramSent’ boolean. Indicates if an empty telegram has been sent to this device, this cycle
     * (true) or not (false).
     *
     * @param emptyTelegramSent the value for the ’emptyTelegramSent’ boolean.
     */
    public void setEmptyTelegramSent(boolean emptyTelegramSent) {
        this.emptyTelegramSent = emptyTelegramSent;
    }

    /**
     * If all low priority tasks have been added to the task queue, this cycle.
     *
     * @return true for yes and false for no.
     */
    public boolean isAllLowPrioTasksAdded() {
        return this.allLowPrioTasksAdded;
    }

    /**
     * Set the ’allLowPrioTasksAdded’ boolean. Indicates if all low priority tasks have been added to the task queue,
     * this cycle (true) or not (false).
     *
     * @param allLowPrioTasksAdded the value for the ’allLowPrioTasksAdded’ boolean.
     */
    public void setAllLowPrioTasksAdded(boolean allLowPrioTasksAdded) {
        this.allLowPrioTasksAdded = allLowPrioTasksAdded;
    }

    /**
     * Get the task address from which the pressure sensor INFO is taken. In pressure set point mode, ref_rem maps to the
     * range indicated by that INFO.
     *
     * @return the task address.
     */
    public int getPressureSensorTaskAddress() {
        return this.pressureSensorTaskAddress;
    }

    /**
     * Get the OpenEMS Unit of the pressure sensor values ’pressureSensorMin’ and ’pressureSensorRange’.
     *
     * @return the Unit
     */
    public Unit getSensorUnit() {
        return this.sensorUnit;
    }

    /**
     * Get the ’pressureSensorMin’ value. In pressure set point mode, this is needed to calculate the set point.
     * The unit of the value can be queried with ’getSensorUnit()’.
     *
     * @return the ’pressureSensorMinBar’ value.
     */
    public double getPressureSensorMin() {
        return this.pressureSensorMin;
    }

    /**
     * Set the ’pressureSensorMin’ value. In pressure set point mode, this is needed to calculate the set point.
     * The unit of the value can be queried with ’getSensorUnit()’.
     *
     * @param pressureSensorMinBar the ’pressureSensorMinBar’ value.
     */
    public void setPressureSensorMin(double pressureSensorMinBar) {
        this.pressureSensorMin = pressureSensorMinBar;
    }

    /**
     * Get the ’pressureSensorRange’ value. In pressure set point mode, this is needed to calculate the set point.
     * The unit of the value can be queried with ’getSensorUnit()’.
     *
     * @return the ’pressureSensorRangeBar’ value.
     */
    public double getPressureSensorRange() {
        return this.pressureSensorRange;
    }

    /**
     * Set the ’pressureSensorRange’ value. In pressure set point mode, this is needed to calculate the set point.
     * The unit of the value can be queried with ’getSensorUnit()’.
     *
     * @param pressureSensorRangeBar the ’pressureSensorRangeBar’ value.
     */
    public void setPressureSensorRange(double pressureSensorRangeBar) {
        this.pressureSensorRange = pressureSensorRangeBar;
    }

    /**
     * Get the ’connectionOk’ value. The connection is not ok when the communication to the device is not working, or
     * when the received telegrams can not be parsed.
     *
     * @return the ’connectionOk’ value.
     */
    public boolean isConnectionOk() {
        return this.connectionOk;
    }

    /**
     * Set the ’connectionOk’ value. The connection is not ok when the communication to the device is not working, or
     * when the received telegrams can not be parsed.
     *
     * @param connectionOk the ’connectionOk’ value.
     */
    public void setConnectionOk(boolean connectionOk) {
        this.connectionOk = connectionOk;
    }

    /**
     * If all priority once tasks have been added to the task queue.
     *
     * @return true for yes and false for no.
     */
    public boolean isAddAllOnceTasks() {
        return this.addAllOnceTasks;
    }

    /**
     * Set the ’addAllOnceTasks’ boolean. Indicates if all priority once tasks have been added to the task queue (true)
     * or not (false).
     *
     * @param addAllOnceTasks the ’addAllOnceTasks’ value.
     */
    public void setAddAllOnceTasks(boolean addAllOnceTasks) {
        this.addAllOnceTasks = addAllOnceTasks;
    }

    /**
     * Reset the device, meaning delete all data saved so far for this device. The device will behave as if the program
     * had just been started. Some information is only requested once from the device at startup. This is the only way
     * to make the program request this information again from the device.
     * This is used in case of connection loss. The pump might have been turned off and back on and changed it's settings
     * while doing so. Or another pump is now using this address.
     */
    public void resetDevice() {
        this.taskManager.getAllTasks().forEach(task -> {
            task.resetInfo();
            if (task instanceof HeadClass4and5) {
                ((HeadClass4and5) task).setExecuteGet(true);
            }
        });
        this.addAllOnceTasks = true;
        this.pressureSensorMin = 0;
        this.pressureSensorRange = 0;
        this.deviceReadBufferLengthBytes = 70;
        this.deviceSendBufferLengthBytes = 102;

        int arrayLength1 = this.millisecondsPerByte.length;
        for (int i = 0; i < arrayLength1; i++) {
            this.millisecondsPerByte[i] = 2.0;
        }

        int arrayLength2 = this.emptyTelegramTime.length;
        for (int i = 0; i < arrayLength2; i++) {
            this.emptyTelegramTime[i] = 40;
        }
    }

    /**
     * If this is the first (non empty) telegram after a connection loss. Queried during telegram creation, when deciding
     * which tasks to put in the telegram.
     *
     * @return true for yes and false for no.
     */
    public boolean isFirstTelegramAfterConnectionLoss() {
        return this.firstTelegram;
    }

    /**
     * Set the ’firstTelegram’ boolean. Indicates that the next (non empty) telegram to this device will be the first
     * after a connection loss (true) or not (false).
     *
     * @param firstTelegram the ’firstTelegram’ value.
     */
    public void setFirstTelegramAfterConnectionLoss(boolean firstTelegram) {
        this.firstTelegram = firstTelegram;
    }

    /**
     * Get the ’connectionTimeoutCounter’ value. Counts the subsequent failed attempts to receive a response telegram,
     * capped at 3.
     *
     * @return the ’connectionTimeoutCounter’ value.
     */
    public int getConnectionTimeoutCounter() {
        return this.connectionTimeoutCounter;
    }

    /**
     * Set the ’connectionTimeoutCounter’ value. Counts the subsequent failed attempts to receive a response telegram,
     * capped at 3.
     *
     * @param connectionTimeoutCounter the ’connectionTimeoutCounter’ value.
     */
    public void setConnectionTimeoutCounter(int connectionTimeoutCounter) {
        this.connectionTimeoutCounter = connectionTimeoutCounter;
    }

    /**
     * Store a warning message.
     *
     * @param message the message as a string.
     */
    public void setWarningMessage(String message) {
        if (this.warningMessage.length() < 1000) {  // Overflow protection.
            if (this.warningMessage.equals("")) {
                this.warningMessage = message;
            } else {
                this.warningMessage = this.warningMessage + " " + message;
            }
        }
    }

    /**
     * Collect the warning messages and clear the storage variable.
     *
     * @return the warning messages as a string.
     */
    public String getAndClearWarningMessage() {
        String returnMessage = this.warningMessage;
        this.warningMessage = "";
        return returnMessage;
    }
}
