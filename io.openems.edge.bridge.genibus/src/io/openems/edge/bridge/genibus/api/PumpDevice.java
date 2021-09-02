package io.openems.edge.bridge.genibus.api;

import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.api.task.GenibusWriteTask;
import io.openems.edge.common.taskmanager.TasksManager;

import java.util.ArrayList;
import java.util.List;

public class PumpDevice {

    /**
     * The Parent component.
     */
    private final String pumpDeviceId;

    /**
     * TaskManager that contains all tasks.
     */
    private final TasksManager<GenibusTask> taskManager = new TasksManager<>();

    /**
     * Queue of tasks that should be sent to the device. This list is filled by getting tasks from the task manager.
     * When a telegram is created, the tasks are taken from this queue. When a task is picked it is removed from the
     * queue. A telegram has limited capacity, so it may happen that tasks remain in the queue after a telegram is
     * created. The tasks then stay in the queue until they can be placed in a telegram.
     */
    private final List<GenibusTask> taskQueue = new ArrayList<>();

    /**
     * This list is for priority once tasks that also have INFO. They need two executions to get the complete
     * information. They are placed in this list for the second execution.
     */
    private final List<GenibusTask> onceTasksWithInfo = new ArrayList<>();

    private int deviceReadBufferLengthBytes = 70;
    private final int deviceSendBufferLengthBytes = 102;
    private final int genibusAddress;
    private final int lowPrioTasksPerCycle;
    private boolean connectionOk = true;    // Initialize with true, to avoid "no connection" message on startup.
    private long timestamp;
    private boolean allLowPrioTasksAdded;
    private boolean addAllOnceTasks = true;
    private double[] millisecondsPerByte = {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0};    // Rough estimate. Exact value measured at runtime.
    private int arrayTracker = 0;

    // Value measured with a laptop is 33 ms. Leaflet timing seems to be slower, so setting a more conservative 40 ms.
    private int[] emptyTelegramTime = {40, 40, 40, 40, 40};
    private int arrayTracker2 = 0;

    private boolean firstTelegram = true;
    private boolean emptyTelegramSent = false;
    private int timeoutCounter = 0;

    // These are needed to calculate the setpoint, since GENIbus setpoints are relative to the sensor range.
    private double pressureSensorMinBar = 0;
    private double pressureSensorRangeBar = 0;

    public PumpDevice(String deviceId, int genibusAddress, int lowPrioTasksPerCycle, GenibusTask... tasks) {
        this.genibusAddress = genibusAddress;
        this.pumpDeviceId = deviceId;
        this.lowPrioTasksPerCycle = lowPrioTasksPerCycle;
        for (GenibusTask task : tasks) {
            this.addTask(task);
        }
        this.timestamp = System.currentTimeMillis() - 1000;
    }

    /**
     * Add a task for this device.
     * @param task a Genibus task.
     */
    public void addTask(GenibusTask task) {
        task.setPumpDevice(this);
        this.taskManager.addTask(task);
    }

    /**
     * Get the task manager.
     * @return the task manager.
     */
    public TasksManager<GenibusTask> getTaskManager() {
        return this.taskManager;
    }

    /**
     * Get the task queue.
     * @return the task queue.
     */
    public List<GenibusTask> getTaskQueue() {
        return this.taskQueue;
    }

    /**
     * Get once tasks that can do ’info’.
     * @return once tasks that can do ’info’.
     */
    public List<GenibusTask> getOnceTasksWithInfo() {
        return this.onceTasksWithInfo;
    }

    /**
     * Set the device read buffer length. Can only set values above 70.
     * @param value the device read buffer length.
     */
    public void setDeviceReadBufferLengthBytes(int value) {
        // 70 is minimum buffer length.
        if (value >= 70) {
            this.deviceReadBufferLengthBytes = value;
        }
    }

    /**
     * Gets the read buffer length (number of bytes) of this GENIbus device. The buffer length is the maximum length a
     * telegram can have that is sent to this device. If this buffer overflows, the device won't answer.
     * @return the read buffer length (number of bytes).
     */
    public int getDeviceReadBufferLengthBytes() {
        return this.deviceReadBufferLengthBytes;
    }

    /**
     * Gets the send buffer length (number of bytes) of this GENIbus device. The buffer length is the maximum length a
     * telegram can have that the device sends as answer telegram. This buffer can overflow when tasks are sent that
     * have more return byte than send byte such as INFO and ASCII.
     * @return the send buffer length (number of bytes).
     */
    public int getDeviceSendBufferLengthBytes() {
        return this.deviceSendBufferLengthBytes;
    }

    /**
     * Get the Genibus address.
     * @return the Genibus address.
     */
    public int getGenibusAddress() {
        return this.genibusAddress;
    }

    /**
     * Get the number of low priority tasks to send per cycle.
     * @return the number of low priority tasks to send per cycle.
     */
    public int getLowPrioTasksPerCycle() {
        return this.lowPrioTasksPerCycle;
    }

    /**
     * Get the pump device id.
     * @return the pump device id.
     */
    public String getPumpDeviceId() {
        return this.pumpDeviceId;
    }

    /**
     * Set a timestamp for this pump device. This is done once per cycle when the first telegram is sent to this pump.
     * This information is used to track which pump has already received a telegram this cycle.
     */
    public void setTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the last timestamp of this pump device.
     * This information is used to track which pump has already received a telegram this cycle.
     * @return the last timestamp of this pump device.
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * This method is used to store timing information about telegram send and receive. The timing information is
     * used to estimate if a telegram can still fit in a cycle or not.
     * To send and receive a telegram takes roughly 33 ms + 3 ms per byte of the APDU. The 33 ms is for an empty telegram.
     * For a more accurate timing the ms per byte is measured and stored with this method. The method saves the last
     * three values so an average can be calculated.
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
        if (this.arrayTracker >= 7) {
            this.arrayTracker = 0;
        }
    }

    /**
     * Gets the time in ms that is added to a telegram process (send and receive) per byte in the apdu. An empty
     * telegram takes ~33 ms to send and receive, each byte in the apdus adds "this" amount of ms to the process.
     *
     * @return the number of milliseconds one byte adds to the telegram send and receive time.
     */
    public double getMillisecondsPerByte() {
        // Average over the seven entries.
        double returnValue = 0;
        for (int i = 0; i < 5; i++) {
            returnValue += this.millisecondsPerByte[i];
        }
        return returnValue / 7;
    }

    /**
     * Set the time it takes to send and receive the response to an empty telegram. Used to calculate the timings of
     * telegrams.
     *
     * @param emptyTelegramTime the time it takes to send and receive the response to an empty telegram.
     */
    public void setEmptyTelegramTime(int emptyTelegramTime) {
        // Save five values so we can average and the value won't jump that much.
        if (emptyTelegramTime > 100) {
            emptyTelegramTime = 100;   // Failsafe.
        }
        if (emptyTelegramTime < 10) {
            emptyTelegramTime = 10;   // Failsafe.
        }
        this.emptyTelegramTime[this.arrayTracker2] = emptyTelegramTime;
        this.arrayTracker2++;
        if (this.arrayTracker2 >= 5) {
            this.arrayTracker2 = 0;
        }
    }

    /**
     * Set the time it takes to send and receive the response to an empty telegram.
     *
     * @return the time it takes to send and receive the response to an empty telegram.
     */
    public int getEmptyTelegramTime() {
        // Average over the five entries.
        int returnValue = 0;
        for (int i = 0; i < 5; i++) {
            returnValue += this.emptyTelegramTime[i];
        }
        return returnValue / 5;
    }

    /**
     * If an empty telegram been sent to this device.
     * @return true for yes and false for no.
     */
    public boolean isEmptyTelegramSent() {
        return this.emptyTelegramSent;
    }

    /**
     * Set the empty telegram sent boolean.
     * @param emptyTelegramSent the value for the empty telegram sent boolean.
     */
    public void setEmptyTelegramSent(boolean emptyTelegramSent) {
        this.emptyTelegramSent = emptyTelegramSent;
    }

    /**
     * Set the ’allLowPrioTasksAdded’ boolean.
     * @param allLowPrioTasksAdded the value for the ’allLowPrioTasksAdded’ boolean.
     */
    public void setAllLowPrioTasksAdded(boolean allLowPrioTasksAdded) {
        this.allLowPrioTasksAdded = allLowPrioTasksAdded;
    }

    /**
     * Get the ’allLowPrioTasksAdded’ boolean.
     * @return the ’allLowPrioTasksAdded’ boolean.
     */
    public boolean isAllLowPrioTasksAdded() {
        return this.allLowPrioTasksAdded;
    }

    /**
     * Get the ’pressureSensorMinBar’ value. This is needed to calculate the value of the transmitted measurement data.
     * @return the ’pressureSensorMinBar’ value.
     */
    public double getPressureSensorMinBar() {
        return this.pressureSensorMinBar;
    }

    /**
     * Set the ’pressureSensorMinBar’ value. This is needed to calculate the value of the transmitted measurement data.
     * @param pressureSensorMinBar the ’pressureSensorMinBar’ value.
     */
    public void setPressureSensorMinBar(double pressureSensorMinBar) {
        this.pressureSensorMinBar = pressureSensorMinBar;
    }

    /**
     * Get the ’pressureSensorRangeBar’ value. This is needed to calculate the value of the transmitted measurement data.
     * @return the ’pressureSensorRangeBar’ value.
     */
    public double getPressureSensorRangeBar() {
        return this.pressureSensorRangeBar;
    }

    /**
     * Set the ’pressureSensorRangeBar’ value. This is needed to calculate the value of the transmitted measurement data.
     * @param pressureSensorRangeBar the ’pressureSensorRangeBar’ value.
     */
    public void setPressureSensorRangeBar(double pressureSensorRangeBar) {
        this.pressureSensorRangeBar = pressureSensorRangeBar;
    }

    /**
     * Get the ’connectionOk’ value.
     * @return the ’connectionOk’ value.
     */
    public boolean isConnectionOk() {
        return this.connectionOk;
    }

    /**
     * Set the ’connectionOk’ value.
     * @param connectionOk the ’connectionOk’ value.
     */
    public void setConnectionOk(boolean connectionOk) {
        this.connectionOk = connectionOk;
    }

    /**
     * Get the ’addAllOnceTasks’ value.
     * @return the ’addAllOnceTasks’ value.
     */
    public boolean isAddAllOnceTasks() {
        return this.addAllOnceTasks;
    }

    /**
     * Set the ’addAllOnceTasks’ value.
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
            if (task instanceof GenibusWriteTask) {
                ((GenibusWriteTask) task).setSendGet(1);
            }
        });
        this.addAllOnceTasks = true;
        this.pressureSensorMinBar = 0;
        this.pressureSensorRangeBar = 0;
        this.deviceReadBufferLengthBytes = 70;
        this.millisecondsPerByte[0] = 2.0;
        this.millisecondsPerByte[1] = 2.0;
        this.millisecondsPerByte[2] = 2.0;
        this.millisecondsPerByte[3] = 2.0;
        this.millisecondsPerByte[4] = 2.0;
        this.millisecondsPerByte[5] = 2.0;
        this.millisecondsPerByte[6] = 2.0;
    }

    /**
     * Get the ’firstTelegram’ value.
     * @return the ’firstTelegram’ value.
     */
    public boolean isFirstTelegram() {
        return this.firstTelegram;
    }

    /**
     * Set the ’firstTelegram’ value.
     * @param firstTelegram the ’firstTelegram’ value.
     */
    public void setFirstTelegram(boolean firstTelegram) {
        this.firstTelegram = firstTelegram;
    }

    /**
     * Get the ’timeoutCounter’ value.
     * @return the ’timeoutCounter’ value.
     */
    public int getTimeoutCounter() {
        return this.timeoutCounter;
    }

    /**
     * Set the ’timeoutCounter’ value.
     * @param timeoutCounter the ’timeoutCounter’ value.
     */
    public void setTimeoutCounter(int timeoutCounter) {
        this.timeoutCounter = timeoutCounter;
    }
}
