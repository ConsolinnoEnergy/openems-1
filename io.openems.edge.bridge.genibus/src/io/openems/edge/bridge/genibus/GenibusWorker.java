package io.openems.edge.bridge.genibus;

import com.google.common.base.Stopwatch;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.api.task.GenibusWriteTask;
import io.openems.edge.bridge.genibus.api.task.HeadClass4and5;
import io.openems.edge.bridge.genibus.protocol.ApplicationProgramDataUnit;
import io.openems.edge.bridge.genibus.protocol.Telegram;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.taskmanager.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * This worker organizes the contents of the telegrams to send to the devices and which device to send them to.
 */
class GenibusWorker extends AbstractCycleWorker {

    private final Logger log = LoggerFactory.getLogger(GenibusWorker.class);
    private final Stopwatch cycleStopwatch = Stopwatch.createUnstarted();
    private final LinkedBlockingDeque<Telegram> telegramQueue = new LinkedBlockingDeque<>();
    private final ArrayList<PumpDevice> deviceList = new ArrayList<>();
    private int currentDeviceCounter = 0;
    private final GenibusImpl parent;
    private long cycleTimeMs = Cycle.DEFAULT_CYCLE_TIME;   // Also measured at runtime.
    private long timeSinceLastTimestamp;
    private int delayCurrentRun;
    private int delayLastRun;
    private int runIdentifier = 0;

    private static final int HEAD_CLASS_PROTOCOL_DATA = 0;
    private static final int HEAD_CLASS_MEASURED_DATA = 2;
    private static final int HEAD_CLASS_COMMANDS = 3;
    private static final int HEAD_CLASS_CONFIGURATION_PARAMETERS = 4;
    private static final int HEAD_CLASS_REFERENCE_VALUES = 5;
    private static final int HEAD_CLASS_CHARACTER_STRINGS = 7;

    // If a device is not responding, try that many times before deciding a device is unreachable.
    private static final int RETRY_COUNTER = 3;

    // If a device is unreachable, try connecting again in that interval.
    private static final int RETRY_INTERVAL_MS = 5000;

    protected GenibusWorker(GenibusImpl parent) {
        this.parent = parent;
    }

    /**
     * Creates a telegram for the current device (if possible) and increments ’currentDeviceCounter’. That means this
     * method iterates over ’deviceList’ with each call. The telegram is built from a task list of priority high, low
     * and once tasks. This list is created by the module that implements the Genibus device.
     *
     * <p>Picking tasks for the telegram works as follows: from the list of tasks, all high tasks are added to taskQueue.
     * Then a number of low tasks is added to tasksQueue. How many low tasks are added is defined by the Genibus device
     * module. Then as many tasks as possible are taken from tasksQueue and added to the telegram. Tasks added to the
     * telegram are removed from tasksQueue. If there are still tasks in taskQueue once the telegram is full, they
     * remain there and will be processed next time a telegram is created for that device. The queue is refilled as
     * needed and checks are in place to prevent the queue from getting too big. All priority once tasks are added to
     * the queue on the first run after the high tasks.</p>
     *
     * <p>All tasks are added to the taskQueue at most once per cycle. If this method is called multiple times in a cycle
     * and iterates over the same device multiple times, it may happen that the taskQueue will be empty because all
     * possible tasks have already been executed this cycle. In that case no telegram will be created, and the method
     * just increases ’currentDeviceCounter’ to switch to the next device.</p>
     */
    protected void createTelegram() {
        PumpDevice currentDevice;
        synchronized (this.deviceList) {
            if (this.deviceList.isEmpty()) {
                if (this.parent.getDebug()) {
                    this.parent.logInfo(this.log, "No devices registered to the GENIbus bridge.");
                }
                return;
            }
            currentDevice = this.deviceList.get(0);

            // This handles the switching to the next device. Selects the next device in the list, unless that device is offline.
            for (int i = 0; i < this.deviceList.size(); i++) {
                if (this.deviceList.size() <= this.currentDeviceCounter) {
                    this.currentDeviceCounter = 0;
                }
                currentDevice = this.deviceList.get(this.currentDeviceCounter);
                this.currentDeviceCounter++;

                /* If a device is not responding, don't try every cycle to prevent clogging up the Genibus bridge.
                   The timeoutCounter is 0 when the device is responding. If the device has not responded RETRY_COUNTER
                   times in a row, wait RETRY_INTERVAL_MS milliseconds before trying again. Otherwise skip the device. */
                if (currentDevice.getConnectionTimeoutCounter() >= RETRY_COUNTER) {
                    long timeSinceLastTry = System.currentTimeMillis() - currentDevice.getTimestamp();
                    if (timeSinceLastTry > RETRY_INTERVAL_MS || timeSinceLastTry < 0) {  // < 0 for overflow protection.
                        break;
                    }
                    if (i + 1 == this.deviceList.size()) {  // Last loop iteration.
                        /* You land here when all devices are not not responding and have been checked in the last 5
                           seconds. Iterate once more over the device list to get the device that was first selected. */
                        if (this.deviceList.size() <= this.currentDeviceCounter) {
                            this.currentDeviceCounter = 0;
                        }
                        currentDevice = this.deviceList.get(this.currentDeviceCounter);
                        this.currentDeviceCounter++;
                    }
                } else {
                    break;
                }
            }
        }

        /* A timestamp is saved on the first telegram creation for this device in a cycle. The timestamp saved is not the
           current time, but the time of the event TOPIC_CYCLE_EXECUTE_WRITE. The timestamp is used to track if it is the
           first telegram to this device this cycle or not. */
        this.timeSinceLastTimestamp = System.currentTimeMillis() - currentDevice.getTimestamp();
        // Rollover protection. Just in case this software actually runs that long...
        if (this.timeSinceLastTimestamp < 0) {
            this.timeSinceLastTimestamp = this.cycleTimeMs;
        }

        /* If connection to pump is lost (pump turned off for example), send an empty telegram to test if the pump is
           back online. If an answer is received, isConnectionOk() returns true next time and normal execution will resume. */
        if (currentDevice.isConnectionOk() == false) {
            if (this.timeSinceLastTimestamp > this.cycleTimeMs - 50) {  // Send once per cycle.
                currentDevice.setTimestamp(this.parent.timestampExecuteWrite);
                currentDevice.setFirstTelegramAfterConnectionLoss(true);
                if (this.parent.getDebug()) {
                    this.parent.logInfo(this.log, "Sending empty telegram to test if device " + currentDevice.getPumpDeviceId() + " is online.");
                }
                this.sendEmptyTelegram(currentDevice);
            }
            return;
        }

        /* Remaining bytes that can be put in the request telegram. More bytes than that will result in a buffer overflow
           in the device and there will be no answer. Calculated by getting the buffer length of the device and subtracting
           the telegram header (4 bytes) and the crc (2 bytes). */
        int requestTelegramRemainingBytes = currentDevice.getDeviceReadBufferLengthBytes() - 6;

        /* Remaining bytes that can be put in the response telegram. More bytes than that will result in a buffer overflow
           in the device and there will be no answer. A buffer overflow in the response is possible because tasks like INFO
           and ASCII can have more bytes in the response than in the request telegram.
           Calculated by getting the buffer length of the device and subtracting the telegram header (4 bytes) and the
           crc (2 bytes). */
        int responseTelegramRemainingBytes = currentDevice.getDeviceSendBufferLengthBytes() - 6;

        this.fillTaskQueue(currentDevice, requestTelegramRemainingBytes);

        // Get taskQueue from the device. Tasks that couldn't fit in the last telegram stayed in the queue.
        List<GenibusTask> tasksQueue = currentDevice.getTaskQueue();

        if (tasksQueue.isEmpty()) {
            /* Nothing left to do. Might as well send an empty telegram and measure the timing, since that value is used
               when adjusting timeout based on telegram size. Don't do that when it's a broadcast. */
            if (currentDevice.isEmptyTelegramSent() == false && currentDevice.getGenibusAddress() != 254) {
                currentDevice.setEmptyTelegramSent(true);
                this.sendEmptyTelegram(currentDevice);
                return;
            }

            // Now there is really nothing useful left to do. No tasks could be found that were not already executed this cycle.
            if (this.parent.getDebug()) {
                this.parent.logInfo(this.log, "No tasks left this cycle for pump " + currentDevice.getPumpDeviceId()
                        + ". Time in cycle: " + (this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayCurrentRun) + " ms.");
            }
            return;
        }

        if (this.parent.getDebug()) {
            this.parent.logInfo(this.log, "--Telegram Builder--");
            this.parent.logInfo(this.log, "Number of pumps in list: " + this.deviceList.size()
                    + ", current pump number: " + this.currentDeviceCounter + ", current pump id: " + currentDevice.getPumpDeviceId()
                    + ", GENIbus address: " + currentDevice.getGenibusAddress()
                    + ", Time in cycle: " + (this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayCurrentRun) + " ms.");
            /* currentDeviceCounter has already been incremented at this point, but that is fine. First pump in the
               list is now displayed as "pump number 1", while the corresponding deviceCounter number would be 0. */
        }

        /* This list contains all tasks for the current telegram. The key is a 3 digit decimal number called the
           apduIdentifier. The 100 digit is the HeadClass of the apdu, the 10 digit is the operation
           (0=get, 2=set, 3=info), the 1 digit is a counter starting at 0. The counter allows to have more than one apdu
           of a given type. Since an apu (request and answer) is limited to 63 bytes, several apdu of the same type
           might be needed to fit all tasks. */
        Map<Integer, ArrayList<GenibusTask>> telegramTaskList = new HashMap<>();

        // Same as telegramTaskList, except this list contains the apdus with the tasks of telegramTaskList.
        Map<Integer, ApplicationProgramDataUnit> telegramApduList = new HashMap<>();

        if (this.parent.getDebug()) {
            this.parent.logInfo(this.log, "Bytes allowed: " + requestTelegramRemainingBytes + ", task queue size: "
                    + tasksQueue.size() + ".");
            this.parent.logInfo(this.log, "Tasks are listed with \"apduIdentifier, address\". The apduIdentifier "
                    + "is a three digit number where the 100 digit is the HeadClass of the apdu, the 10 digit is the "
                    + "operation (0=get, 2=set, 3=info) and the 1 digit is a counter starting at 0 to track if more than "
                    + "one apdu of this type exists.");
        }

        int answerByteCounter = 0;  // Send byte counter is not needed, as telegram class has a method to calculate that.
        int taskPickPosition = 0;

        /* This loop processes the tasksQueue, creates the APDUs, and produces the lists needed for the telegram creation
           (telegramTaskList and telegramApduList). A task is picked from the queue and it is decided if that task is
           executed as INFO, GET or SET (in that priority), or not executed at all. If the task can be executed, the
           request and response telegrams are checked if they have enough space left to fit the task. If yes, the task
           is added to an APDU. It is also decided if the task is removed from the queue, processed a second time or
           skipped. */
        while (tasksQueue.size() > taskPickPosition) {
            GenibusTask currentTask = tasksQueue.get(taskPickPosition);

            /* Check how many bytes this task would add to the telegram. This check also decides if the task is INFO, GET
               or SET and saves that decision in the apduIdentifier. It is also checked if the task needs to be executed
               at all. If not, a 0 is returned. */
            int taskSendByteCount = this.decideApduAndGetTaskByteCount(currentDevice, currentTask, telegramApduList);

            // Calculate how many bytes this task would add to the answer telegram.
            int taskAnswerByteCount = this.checkAnswerByteSize(currentTask, taskSendByteCount);

            /* When taskSendByteCount is 0, this task does nothing (for example a command task with no command to send).
               Don't add this task to the telegram and remove it from the task queue. */
            if (taskSendByteCount == 0) {
                this.removeTaskFromQueue(tasksQueue, taskPickPosition);
                if (this.parent.getDebug()) {
                    this.parent.logInfo(this.log, "Task " + currentTask.getApduIdentifier() + ", "
                            + Byte.toUnsignedInt(currentTask.getAddress()) + " has nothing to send. Removed from queue. "
                            + "Task queue size: " + tasksQueue.size());
                }
                continue;
            }

            // Check if there are enough bytes left in the telegram for this task.
            if (requestTelegramRemainingBytes - taskSendByteCount >= 0
                    && responseTelegramRemainingBytes - taskAnswerByteCount >= 0) {
                // The task can be added. Update byte counter.
                requestTelegramRemainingBytes = requestTelegramRemainingBytes - taskSendByteCount;
                responseTelegramRemainingBytes = responseTelegramRemainingBytes - taskAnswerByteCount;
                answerByteCounter += taskAnswerByteCount;

                /* Write the bytes to the APDU and update telegramTaskList and telegramApduList. This also creates the
                   APDU if necessary. */
                this.addTaskToApdu(currentTask, telegramTaskList, telegramApduList);

                // Remove the task from the queue. Special handling for tasks that can do GET and SET.
                this.removeTaskFromQueue(tasksQueue, taskPickPosition);
                if (this.parent.getDebug()) {
                    this.parent.logInfo(this.log, "Adding task: " + currentTask.getApduIdentifier() + ", "
                            + Byte.toUnsignedInt(currentTask.getAddress()) + " - bytes added: " + taskSendByteCount + " - bytes remaining: "
                            + requestTelegramRemainingBytes + " - Task queue size: " + tasksQueue.size());
                }
                if (requestTelegramRemainingBytes == 0 || responseTelegramRemainingBytes == 0) {
                    // Telegramm is full. In case tasks queue is not yet empty, need break to escape the loop.
                    break;
                }
                continue;
            }

            /* You land here if the taskSendByteCount or taskAnswerByteCount of the task is too big to fit, but there is
               still space left in the telegram and there are still tasks in the queue. Check if the task that did not
               fit is ASCII. */
            boolean currentTaskIsAscii = currentTask.getHeader() == HEAD_CLASS_CHARACTER_STRINGS;
            if (currentTaskIsAscii) {
                /* The task is ASCII. An ASCII task needs a full size APDU to itself, which is most likely the entire
                   telegram. So it is very likely that ’taskSendByteCount’ is too high for an ASCII task, but there is plenty
                   of space left in the telegram.
                   Solution: Just skip this task and continue. That leaves the task at the front of the queue for the next
                   telegram, where it has a good chance to be added successfully. */
                taskPickPosition++;
                continue;
            }

            /* The task does not fit, but it is not ASCII. Leave that task in the queue for the next telegram.
               There is still space in the telegram, but probably not much. Try to add one more task to this telegram.
               Search the remaining task queue once for a task that is small enough to fit. After that, exit the loop. */
            if (requestTelegramRemainingBytes >= 1 && responseTelegramRemainingBytes >= 1) {
                for (int i = taskPickPosition + 1; i < tasksQueue.size(); i++) {
                    currentTask = tasksQueue.get(i);
                    taskSendByteCount = this.decideApduAndGetTaskByteCount(currentDevice, currentTask, telegramApduList);
                    taskAnswerByteCount = this.checkAnswerByteSize(currentTask, taskSendByteCount);
                    if (taskSendByteCount != 0) {
                        if (requestTelegramRemainingBytes - taskSendByteCount >= 0
                                && responseTelegramRemainingBytes - taskAnswerByteCount >= 0) {
                            requestTelegramRemainingBytes = requestTelegramRemainingBytes - taskSendByteCount;
                            responseTelegramRemainingBytes = responseTelegramRemainingBytes - taskAnswerByteCount;
                            answerByteCounter += taskAnswerByteCount;
                            this.addTaskToApdu(currentTask, telegramTaskList, telegramApduList);
                            this.removeTaskFromQueue(tasksQueue, i);
                            if (this.parent.getDebug()) {
                                this.parent.logInfo(this.log, "Adding last small task: " + currentTask.getApduIdentifier()
                                        + ", " + Byte.toUnsignedInt(currentTask.getAddress()) + " - bytes added: "
                                        + taskSendByteCount + " - bytes remaining: " + requestTelegramRemainingBytes
                                        + " - Task queue size: " + tasksQueue.size() + " - remaining bytes in response telegram: "
                                        + responseTelegramRemainingBytes);
                            }
                            // Telegram is full enough. In case tasksQueue is not yet empty, need break to escape the loop.
                            break;
                        }
                    }
                }
            }
            // Telegram is full enough. In case tasksQueue is not yet empty, need break to escape the loop.
            break;
        }

        // Create the telegram and add it to the queue.
        Telegram telegram = new Telegram();
        telegram.setStartDelimiterDataRequest();
        telegram.setDestinationAddress(currentDevice.getGenibusAddress());
        telegram.setSourceAddress(0x01);
        telegram.setPumpDevice(currentDevice);

        // Upper limit estimate, used to calculate communication timeout setting.
        telegram.setAnswerTelegramPduLengthEstimate(answerByteCounter);

        telegramApduList.forEach((key, apdu) -> {
            telegram.getProtocolDataUnit().putApdu(apdu);
        });
        telegram.setTelegramTaskList(telegramTaskList);

        this.telegramQueue.add(telegram);
    }

    /**
     * Create an empty telegram for the Genibus device ’currentDevice’.
     * @param currentDevice the Genibus device.
     */
    private void sendEmptyTelegram(PumpDevice currentDevice) {
        Telegram telegram = new Telegram();
        telegram.setStartDelimiterDataRequest();
        telegram.setDestinationAddress(currentDevice.getGenibusAddress());
        telegram.setSourceAddress(0x01);
        telegram.setPumpDevice(currentDevice);
        this.telegramQueue.add(telegram);
    }

    /**
     * This method fills the taskQueue of the pump device with tasks. Once per cycle, all high tasks are added (if the
     * queue does not already contain them). Then a configurable amount of low tasks are added as well. If it is the
     * first telegram sent to this device (true after a connection loss), then all priority once tasks are added too.
     * Using a timestamp, the method recognizes if it was already called this cycle for this device. If it is not the
     * first call, no tasks are added to the queue if the queue is not empty. If the queue is empty, the remaining low
     * tasks are added. The logic of the method should prevent a task from being executed more than once per cycle.
     *
     * @param currentDevice the pump device for which to prepare tasks.
     * @param telegramRemainingBytes remaining bytes in the telegram.
     */
    private void fillTaskQueue(PumpDevice currentDevice, int telegramRemainingBytes) {
        List<GenibusTask> tasksQueue = currentDevice.getTaskQueue();

        /* Priority low tasks are added with .getOneTask(), which starts from 0 when the end of the list is reached.
           Make sure the number of low tasks added per telegram is not longer than the list to prevent adding a task twice. */
        int lowTasksToAdd = currentDevice.getLowPrioTasksPerCycle();
        int numberOfLowTasks = currentDevice.getTaskManager().getAllTasks(Priority.LOW).size();
        if (lowTasksToAdd <= 0) {
            lowTasksToAdd = 1;
        }
        if (lowTasksToAdd > numberOfLowTasks) {
            lowTasksToAdd = numberOfLowTasks;
        }

        boolean firstTelegramToThisDeviceThisCycle = this.timeSinceLastTimestamp > this.cycleTimeMs - 50;
        if (firstTelegramToThisDeviceThisCycle || currentDevice.isFirstTelegramAfterConnectionLoss()) {
            // This part should execute once per cycle only.

            currentDevice.setTimestamp(this.parent.timestampExecuteWrite);
            currentDevice.setAllLowPrioTasksAdded(false);
            currentDevice.setFirstTelegramAfterConnectionLoss(false);
            currentDevice.setEmptyTelegramSent(false);

            /* Check content of taskQueue. If length is longer than numberOfHighTasks + lowTasksToAdd (=number of tasks
               this method would add), all ’high’ tasks are already in the queue and don't need to be added again. */
            int numberOfHighTasks = currentDevice.getTaskManager().getAllTasks(Priority.HIGH).size();
            if (tasksQueue.size() <= numberOfHighTasks + lowTasksToAdd) {
                // Add all high tasks
                tasksQueue.addAll(currentDevice.getTaskManager().getAllTasks(Priority.HIGH));

                /* Add all ’once’ tasks that need a second execution because the first execution was only INFO. This list
                   should be empty after the second or third cycle and won't be refilled. */
                List<GenibusTask> onceTasksWithInfo = currentDevice.getOnceTasksWithInfo();
                for (int i = 0; i < onceTasksWithInfo.size(); i++) {
                    GenibusTask currentTask = onceTasksWithInfo.get(i);
                    if (currentTask.informationDataAvailable()) {
                        tasksQueue.add(currentTask);
                        onceTasksWithInfo.remove(i);
                        i--;
                    }
                }

                // Add all once Tasks. Only done on the first run or after a device reset.
                if (currentDevice.isAddAllOnceTasks()) {
                    currentDevice.getTaskManager().getAllTasks(Priority.ONCE).forEach(onceTask -> {
                        tasksQueue.add(onceTask);
                        // Tasks with INFO need two executions. First to get INFO, second for GET and/or SET.
                        switch (onceTask.getHeader()) {
                            case HEAD_CLASS_MEASURED_DATA:
                            case HEAD_CLASS_CONFIGURATION_PARAMETERS:
                            case HEAD_CLASS_REFERENCE_VALUES:
                                currentDevice.getOnceTasksWithInfo().add(onceTask);
                        }
                    });
                    currentDevice.setAddAllOnceTasks(false);
                }

                // Add a number of low tasks.
                List<GenibusTask> fillerTaskQueue = currentDevice.getLowTasksFillQueue();
                for (int i = 0; i < lowTasksToAdd; i++) {
                    GenibusTask currentTask;
                    if (fillerTaskQueue.isEmpty()) {
                        currentTask = currentDevice.getTaskManager().getOneTask(Priority.LOW);
                    } else {
                        /* Pick tasks from fillerTaskQueue if that queue is not empty, so the pick order is preserved.
                           Makes sure no task is skipped. */
                        currentTask = fillerTaskQueue.get(0);
                        fillerTaskQueue.remove(0);
                    }
                    if (currentTask == null) {
                        break;  // This should not happen, but checked just in case to prevent null pointer exception.
                    } else {
                        tasksQueue.add(currentTask);
                    }
                }
            }
        } else {
            /* This executes if a telegram was already sent to this pumpDevice in this cycle.
               Check how full the taskQueue is, and estimate if that fills a telegram to at least 1/3 capacity (assume
               one task = one byte). If not, fill up taskQueue with remaining low priority tasks to not waste telegram
               capacity. Sending nearly empty telegrams is not very efficient (empty telegram time ~40 ms, full telegram
               time ~140 ms).
               If all the low priority tasks have been executed this cycle, there is nothing useful left to do. */
            if (tasksQueue.size() <= (telegramRemainingBytes / 3)) {
                List<GenibusTask> fillerTaskQueue = currentDevice.getLowTasksFillQueue();

                if (currentDevice.isAllLowPrioTasksAdded() == false) {
                    // This executes just once per cycle because of the boolean.
                    currentDevice.setAllLowPrioTasksAdded(true);

                    /* The amount ’lowTasksToAdd’ was already added to tasksQueue in the first execution this cycle. The
                       number of ’low’ tasks not yet executed this cycle is then ’numberOfLowTasks - lowTasksToAdd’.
                       Fill the fillerTaskQueue up so that it contains this amount of tasks. These tasks are then all
                       the ’low’ tasks not yet executed this cycle. */
                    int addToFillerTaskQueue = numberOfLowTasks - lowTasksToAdd - fillerTaskQueue.size();
                    addToFillerTaskQueue = Math.max(addToFillerTaskQueue, 0);
                    for (int i = 0; i < addToFillerTaskQueue; i++) {
                        GenibusTask currentTask = currentDevice.getTaskManager().getOneTask(Priority.LOW);
                        if (currentTask == null) {
                            break;  // This should not happen, but checked just in case to prevent null pointer exception.
                        } else {
                            fillerTaskQueue.add(currentTask);
                        }
                    }
                }

                if (fillerTaskQueue.size() > 0) {   // If this is false, there is nothing useful left to do.
                    int lowTaskFill = fillerTaskQueue.size();
                    /* Compare that number with the bytes allowed in this telegram. We want to fill this telegram up
                       with low tasks, but we don't want to add more tasks than can be sent with this telegram. Anything
                       left in taskQueue is executed in the next telegram and reduces the space there for high tasks.
                       Assume one task = one byte.
                       The math is not exact but a good enough estimate. 1-2 low tasks remaining in taskQueue is not a big deal. */
                    if ((telegramRemainingBytes - 2) < lowTaskFill) {    // -2 to account for at least one apdu header.
                        lowTaskFill = (telegramRemainingBytes - 2);
                    }

                    for (int i = 0; i < lowTaskFill; i++) {
                        GenibusTask currentTask = fillerTaskQueue.get(0);
                        if (currentTask == null) {
                            break;  // This should not happen, but checked just in case to prevent null pointer exception.
                        } else {
                            fillerTaskQueue.remove(0);
                            tasksQueue.add(currentTask);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes the task at ’position’ from the tasksQueue.
     *
     * <p>Exception: if the task is head class 4 or 5 GET, it is not removed. These tasks can do GET and SET in the same
     * telegram, but there is just one instance of them in the task queue.</p>
     *
     * <p>The method to execute such a task two times then works like this: There is a flag for GET, and the task is
     * executed as GET if that flag is set (and INFO is already done). This method then checks if the task was added as
     * GET by examining its apduIdentifier. If a GET is detected, the ’GET flag’ is switched back to false and the task
     * left in the queue in the same position. The next loop iteration will then pick this task again, but it will
     * execute it as SET because the ’GET flag’ is now false. If executed as SET, the task is removed from the queue.</p>
     *
     * @param tasksQueue the task queue.
     * @param position the position in the task queue (first position is 0).
     */
    private void removeTaskFromQueue(List<GenibusTask> tasksQueue, int position) {
        GenibusTask currentTask = tasksQueue.get(position);
        if (currentTask instanceof HeadClass4and5 && currentTask.informationDataAvailable()) {
            boolean isGet = ((currentTask.getApduIdentifier() % 100) / 10) == 0;
            if (isGet) {
                // This task was executed as GET. Set ’executeGet’ to false and leave task in queue, to execute it again as SET.
                ((HeadClass4and5) currentTask).setExecuteGet(false);
                return;
            }
        }
        tasksQueue.remove(position);
    }

    /**
     * Returns how many bytes this task would add to the telegram, if it were added. Decides if a task is executed or
     * not, and returns 0 if no. Also, the method decides if a task is executed as INFO, GET or SET (in that priority),
     * and then decides in which APDU the task is placed. A corresponding apduIdentifier is generated and saved to the
     * task object, for later retrieval. The apduIdentifier is saved even if the task is not executed, for debug purposes.
     *
     * <p>How to decide between INFO, GET or SET:</p>
     * <ul>
     * <li>Tasks of head class 3 can do INFO but don't need it. They only do SET.
     * <li>Tasks of head class 7 and 0 only do GET.
     * <li>Tasks of head class 2, 4 and 5 need the data from INFO to process the data of GET or SET. So INFO is done
     *     first, and GET or SET only allowed after the data from INFO has been saved to the task object. Info does not
     *     change (with a few exceptions) so only needs to be executed once.
     * <li>Tasks of head class 2 do INFO and GET. After INFO is received they only do GET.
     * <li>Tasks of head class 4 and 5 can do GET and SET. However, the GET data can be considered static, as it only
     *     changes because of a SET. As a result, a GET only needs to be executed once at the beginning and then after
     *     every SET. A flag is used to control if a GET should be executed.
     * </ul>
     *
     * <p>The number of bytes a task adds to the telegram then depends on the type of the task and the byte size of the
     * task. Also, it is checked if an APDU needs to be created to place that task or not. Adding an APDU adds the 2
     * bytes of its header to the telegram.</p>
     *
     * <p>GET tasks have a size equivalent to their dataByteSize. Tasks with more than one byte are segmented into one
     * hi and one or more lo bytes. The hi byte is on the task address, the lo bytes are on the following addresses. Each
     * address needs to be requested, and each address takes 1 byte in the APDU.</p>
     *
     * <p>INFO tasks are always just 1 byte, even for tasks with more than one byte. The relevant information is in INFO
     * of the hi byte, so only the address of the hi byte needs to be put in the APDU.</p>
     *
     * <p>SET tasks depends on the head class. Commands (head class 3) have no value to set, you just put the address in
     * the APDU (=1 byte). Configuration parameters (head class 4) and reference values (head class 5) have values to set.
     * The byte pattern here is the address, followed by the value. Multi byte tasks do that for each byte. As a result,
     * they require dataByteSize*2 bytes in the APDU. As an example, a 16 bit SET task would need 4 bytes. The four bytes
     * are: address hi byte, value hi byte, address lo byte, value lo byte.</p>
     *
     * <p>Furthermore, SET task are checked if there is actually a value that needs setting. If there is no value to set,
     * this method returns 0, to indicate that this task should not be added to the telegram.</p>
     *
     * @param currentDevice the Genibus device the telegram will be sent to.
     * @param task the Genibus task for which to decide the APDU and calculate the number of bytes.
     * @param telegramApduList The list of APDUs already in this telegram.
     * @return the number of bytes this task would add to the telegram. Returns 0 if the task should not execute.
     */
    private int decideApduAndGetTaskByteCount(PumpDevice currentDevice, GenibusTask task, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int headClass = task.getHeader();
        int dataByteSize = task.getDataByteSize();
        switch (headClass) {
            case HEAD_CLASS_PROTOCOL_DATA:
                task.setApduIdentifier(0);
                /* HeadClass 0 only has three commands. No more than one HeadClass 0 APDU will ever exist, and it will
                   never be too full to fit the task. So only need to check if it already exists or not.
                   apduIdentifier is (0*100 for HeadClass 0) + (0*10 for GET) + (0 for first apdu). */
                if (telegramApduList.containsKey(0)) {
                    return dataByteSize;
                } else {
                    // APDU does not yet exist, needs to be created. Add 2 bytes to telegram because of headers of new APDU.
                    return 2 + dataByteSize;
                }
            case HEAD_CLASS_MEASURED_DATA:
                // Check if INFO has already been received. If yes, this task is GET. If not, this task is INFO.
                if (task.informationDataAvailable()) {  // Task is GET
                    /* See if an APDU exists, check if task can fit in existing APDU.
                       apduIdentifier is (2*100 for HeadClass 2) + (0*10 for GET) + (0 for first apdu). */
                    return dataByteSize + this.getApduHeaderBytes(task, 200, dataByteSize, telegramApduList);
                } else {    // Task is INFO
                    /* INFO tasks always need just 1 byte in the request telegram, because we only need the INFO of the
                       hi byte.
                       apduIdentifier is (2*100 for HeadClass 2) + (3*10 for INFO) + (0 for first apdu). */
                    return 1 + this.getApduHeaderBytes(task, 230, 1, telegramApduList);
                }
            case HEAD_CLASS_COMMANDS:
                if (task instanceof GenibusWriteTask) {
                    /* No need to check INFO. HeadClass 3 does allow INFO, but it is not needed. So HeadClass 3 is SET
                       only. Check if task is executed or not. Commands are boolean where ’true’ means ’send the command’
                       and ’false’ means ’don't send’. This is checked by ’isSetAvailable()’. */
                    if (((GenibusWriteTask)task).isSetAvailable()) {    // Execute task.
                        // apduIdentifier is (3*100 for HeadClass 3) + (2*10 for SET) + (0 for first apdu).
                        return dataByteSize + this.getApduHeaderBytes(task, 320, dataByteSize, telegramApduList);
                    } else {    // Don't execute task.
                        task.setApduIdentifier(320);    // Set apduIdentifier for debug info.
                        return 0;   // Task should not execute, so no bytes added to telegram.
                    }
                } else {
                    this.parent.logError(this.log, "GENIbus error. Wrong head class for task "
                            + task.getHeader() + ", " + task.getAddress() + ". Can't execute.");
                    return 0;
                }
            case HEAD_CLASS_CONFIGURATION_PARAMETERS:
            case HEAD_CLASS_REFERENCE_VALUES:
                if (task instanceof HeadClass4and5) {
                    // Check if INFO has already been received. If yes, this task can be GET or SET.
                    if (task.informationDataAvailable()) {
                        // Check flag if this should execute as GET. If not, execute as SET.
                        if (((HeadClass4and5)task).getExecuteGet()) {  // Task is GET.
                            // apduIdentifier is (HeadClass*100) + (0*10 for GET) + (0 for first apdu).
                            return dataByteSize + this.getApduHeaderBytes(task, headClass * 100, dataByteSize, telegramApduList);
                        } else {    // Task is SET.
                            // Check if there is a value to set. If not, don't execute the task.
                            if (((HeadClass4and5)task).isSetAvailable()) {
                                // apduIdentifier is (HeadClass*100) + (2*10 for SET) + (0 for first apdu).
                                return (2 * dataByteSize) + this.getApduHeaderBytes(task,
                                        (headClass * 100) + 20, 2 * dataByteSize, telegramApduList);
                            } else {
                                // Set apduIdentifier for debug readout.
                                task.setApduIdentifier((headClass * 100) + 20);
                                return 0;   // Task should not execute, so no bytes added to telegram.
                            }
                        }
                    } else {    // Task is INFO
                        // apduIdentifier is (HeadClass*100) + (3*10 for INFO) + (0 for first apdu).
                        return 1 + this.getApduHeaderBytes(task, (headClass * 100) + 30, 1, telegramApduList);
                    }
                } else {
                    this.parent.logError(this.log, "GENIbus error. Wrong head class for task "
                            + task.getHeader() + ", " + task.getAddress() + ". Can't execute.");
                    return 0;
                }
            case HEAD_CLASS_CHARACTER_STRINGS:
                /* HeadClass 7 is ASCII, which has only GET. Each ASCII task needs it's own APDU, to be safe from
                   buffer overflow. To achieve that, return maximum APDU byte count.
                   This is not as straight forward, as pumps with a read buffer size of 70 bytes exist.
                   The telegram header is 4 bytes, and the CRC is 2 bytes. This leaves 64 bytes for the APDU. This is
                   lower than the maximum allowed APDU byte count (63 bytes data + 2 bytes header).
                   apduIdentifier is (7*100 for HeadClass 7) + (0*10 for GET) + (0 for first apdu). */
                task.setApduIdentifier(700);
                int spaceInTelegramForApdu = currentDevice.getDeviceReadBufferLengthBytes() - 6;
                return Math.min(spaceInTelegramForApdu, 65);
        }
        // If code gets to here, something went wrong.
        this.parent.logError(this.log, "GENIbus error. Unsupported or wrong head class for task "
                + task.getHeader() + ", " + task.getAddress() + ". Can't execute.");
        return 0;
    }

    /**
     * Helper method for ’decideApduAndGetTaskByteCount()’. Returns the number of bytes that are added because of APDU
     * requirements. Returns 0 if no extra APDU is needed, otherwise 2. If an additional APDU is needed in the telegram
     * to fit this task, the byte count increases by 2 because of the APDU header of the additional APDU.
     *
     * <p>As this method checks in which APDU a task can be placed, the resulting apduIdentifier is saved to the task
     * object so it can be used later when the task is actually added to an apdu.</p>
     *
     * @param task the Genibus task for which to calculate the APDU header bytes.
     * @param apduIdentifier the apduIdentifier of the first APDU this task could be placed in.
     * @param numberOfBytesNeeded the number of bytes this task needs in the APDU.
     * @param telegramApduList The list of APDUs already in this telegram.
     * @return the number of bytes added to the telegram because of an additional APDU.
     */
    private int getApduHeaderBytes(GenibusTask task, int apduIdentifier, int numberOfBytesNeeded, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int nextFreeApdu = apduIdentifier;
        /* nextFreeApdu is the apduIdentifier of the last existing APDU +1. So iterate over the APDUs in the list until
           you find one that does not exist. */
        while (telegramApduList.containsKey(nextFreeApdu)) {
            nextFreeApdu++;
        }
        if (nextFreeApdu == apduIdentifier) { // If this is true, no APDU of the required type exists yet and it needs to be created.
            task.setApduIdentifier(nextFreeApdu);   // Save the apduIdentifier to the task for later retrieval.
            return 2;    // New APDU needed, means 2 additional bytes because of APDU header.
        }
        // If code goes to here, an APDU of the required type already exists. Check remaining space in the last APDU.
        int apduMaxBytes = 63;  // The maximum byte count of an APDU as per Genibus specs.
        boolean isInfo = ((apduIdentifier % 100) / 10) == 3;
        if (isInfo) {
            /* The response to an INFO request can be up to 4 byte. So 1 byte (=1 Task) in the request APDU can result
               in 4 byte in the response ADPU. However, the response APDU has the same 63 bytes limit as the request
               APDU. If that is exceeded, the response telegram won't send. There is no in built protection to prevent
               that, it is totally possible to create and send request telegrams where the response telegram will be
               illegal. This code here limits INFO request APDUs to max 15 tasks (1 task = 1 byte, 15*4 = 60 < 63), so
               that the response APDU will not exceed its limit. */
            apduMaxBytes = 15;
        } else if (task.getHeader() == HEAD_CLASS_CHARACTER_STRINGS) {
            /* The response to an ASCII task can potentially fill the whole APDU with just one task. To account for this,
               restrict ASCII tasks to one task per APDU. */
            apduMaxBytes = 1;
        }
        int remainingBytes = apduMaxBytes - telegramApduList.get(nextFreeApdu - 1).getLength();
        if (remainingBytes >= numberOfBytesNeeded) {
            task.setApduIdentifier(nextFreeApdu - 1);
            return 0; // Task fits in existing APDU. No new APDU needed, no bytes added because of APDU header.
        }
        task.setApduIdentifier(nextFreeApdu);
        return 2;   // Task does not fit in existing APDU. Add 2 bytes because of header of new APDU.
    }

    /**
     * Returns how many bytes this task would need in the response telegram if it were added to the request telegram. The
     * result is used to check telegram size limits and estimate the response time.
     *
     * <p>This is an upper limit/estimate and not an accurate value. For example, INFO can be a 1 or 4 byte answer -> upper
     * limit is 4 bytes. Additional bytes needed for an APDU header are calculated from ’sendByteSize’. If the request
     * is in a new APDU, so is the response.</p>
     *
     * @param task the Genibus task for which to estimate the byte count in the response telegram.
     * @param sendByteSize the byte count of the Genibus task in the request telegram.
     * @return the number of bytes this task would add to the response telegram.
     */
    private int checkAnswerByteSize(GenibusTask task, int sendByteSize) {
        int operationSpecifier = (task.getApduIdentifier() % 100) / 10;
        if (task.getHeader() == HEAD_CLASS_CHARACTER_STRINGS) {
            /* Task is ASCII. Don't know how long that answer will be, 30 is a conservative guess. The number here
               is for timing purposes only. An ASCII task should be the only task in the telegram anyway, so you don't
               need to worry about response telegram buffer overflow. */
            return 30;
        }
        switch (operationSpecifier) {
            case 0: // GET
                return sendByteSize;
            case 2: // SET
                if (sendByteSize > 0) {
                    /* The answer to a SET APDU is an empty APDU. Bytes added to the response telegram will then only be
                       the 2 APDU header bytes. */
                    if (task instanceof HeadClass4and5) {
                        // ’sendByteSize’ is calculated in ’decideApduAndGetTaskByteCount()’. This formula extracts the APDU header part
                        return sendByteSize - (task.getDataByteSize() * 2);
                    } else {    // Head class 3
                        // ’sendByteSize’ is calculated in ’decideApduAndGetTaskByteCount()’. This formula extracts the APDU header part
                        return sendByteSize - task.getDataByteSize();
                    }
                } else {
                    return 0;
                }
            case 3: // INFO
                // INFO response is up to 4 bytes. Possible APDU header bytes are calculated from ’sendByteSize’.
                return  4 + (sendByteSize - 1);
        }
        // Fallback value, this should not happen.
        return 0;
    }

    /**
     * Adds a task to an APDU, which means the task is added to the telegram. This is done by adding the task to the
     * telegramTaskList and the telegramApduList. From these lists the telegram is built.
     * The method will create an APDU if a new one is needed to place the task.
     *
     * @param currentTask the task to add.
     * @param telegramTaskList the list needed for the operation.
     * @param telegramApduList the list needed for the operation.
     */
    private void addTaskToApdu(GenibusTask currentTask, Map<Integer, ArrayList<GenibusTask>> telegramTaskList, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int apduIdentifier = currentTask.getApduIdentifier();
        int operationSpecifier = (apduIdentifier % 100) / 10;

        // telegramApduList should have the same keys as telegramTaskList, so only need to check one.
        if (telegramTaskList.containsKey(apduIdentifier) == false) {
            // Create task list and apdu if they don't exist yet.
            ArrayList<GenibusTask> apduTaskList = new ArrayList<GenibusTask>();
            telegramTaskList.put(apduIdentifier, apduTaskList);
            ApplicationProgramDataUnit newApdu = new ApplicationProgramDataUnit();
            newApdu.setHeadClass(apduIdentifier / 100);
            newApdu.setHeadOsAck(operationSpecifier);
            telegramApduList.put(apduIdentifier, newApdu);
        }

        // Add task to list.
        telegramTaskList.get(apduIdentifier).add(currentTask);

        // Write bytes in apdu. For tasks with more than 8 bit, put more than one byte in the apdu.
        int numberOfBytesInApdu = currentTask.getDataByteSize();
        if (operationSpecifier == 3) {
            /* Task is INFO, needs just one byte even for multi byte tasks. ’numberOfBytesInApdu’ would have been > 1 for
               a multibyte task. */
            numberOfBytesInApdu = 1;
        }
        for (int i = 0; i < numberOfBytesInApdu; i++) {
            telegramApduList.get(apduIdentifier).putDataField(currentTask.getAddress() + i);
            // Add write value for write task.
            switch (currentTask.getHeader()) {
                case HEAD_CLASS_COMMANDS:
                    if (currentTask instanceof GenibusWriteTask) {  // Safety, should always be true.
                        if (i == 0) {   // Commands should always be just one byte
                            // Reset channel write value to send command just once.
                            ((GenibusWriteTask)currentTask).clearNextWriteAndUpdateChannel();
                        } else {
                            // Should not trigger, but just in case.
                            this.parent.logWarn(this.log, "Code error. Command task " + currentTask.getHeader()
                                    + ", " + currentTask.getAddress() + " for APDU " + apduIdentifier + " should be one byte only, "
                                    + "but it is not!");
                        }
                    } else {
                        // Should not trigger, but just in case.
                        this.parent.logError(this.log, "GENIbus error. Wrong head class for task "
                                + currentTask.getHeader() + ", " + currentTask.getAddress() + ". Can't execute.");
                    }
                    break;
                case HEAD_CLASS_CONFIGURATION_PARAMETERS:
                case HEAD_CLASS_REFERENCE_VALUES:
                    if (currentTask instanceof HeadClass4and5) {  // Safety, should always be true.
                        boolean isSet = ((apduIdentifier % 100) / 10) == 2;
                        if (isSet) {
                            int unsignedByteForApdu = ((HeadClass4and5) currentTask).getByteIfSetAvailable(i);
                            if (unsignedByteForApdu < 0 || unsignedByteForApdu > 255) {
                                // Should not trigger, but just in case.
                                this.parent.logWarn(this.log, "Code error. Write value from task " + currentTask.getHeader()
                                        + ", " + currentTask.getAddress() + " for APDU " + apduIdentifier + " should be a byte, "
                                        + "but it is not!");
                                /* Code continues and puts the wrong byte value in the APDU. This is necessary, as
                                   otherwise the byte pattern is not correct. */
                            }
                            telegramApduList.get(apduIdentifier).putDataField((byte) unsignedByteForApdu);
                            if (i >= (currentTask.getDataByteSize() - 1)) {
                                // All bytes collected now, reset channel write value to send it just once.
                                ((HeadClass4and5)currentTask).clearNextWriteAndUpdateChannel();
                            }
                        }
                    } else {
                        // Should not trigger, but just in case.
                        this.parent.logError(this.log, "GENIbus error. Wrong head class for task "
                                + currentTask.getHeader() + ", " + currentTask.getAddress() + ". Can't execute.");
                    }
                    break;
            }
        }
    }


    /**
     * Checks if the telegram queue is empty and if yes, creates a telegram (if possible) and puts it in the queue.
     * If a telegram is in the queue, the connection is checked and the telegram is sent. If the connection is not ok,
     * the telegram will stay in the waiting queue until it can be sent. This is important to ensure priority once tasks
     * are actually executed, since they are only added to one telegram. So far there is no check to see if a priority
     * once task has actually been executed.
     */
    @Override
    protected void forever() {
        /* The forever method can start delayed, if the last execution took longer than one cycle. The delay time is
           considered for timing calculations. */
        this.delayCurrentRun = this.parent.workerStartDelay;
        this.runIdentifier = this.parent.workerRunIdentifier;
        if (this.cycleStopwatch.isRunning()) {
            this.cycleTimeMs = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayLastRun - this.delayCurrentRun;
            if (this.parent.getDebug()) {
                this.parent.logInfo(this.log, "Stopwatch 3: " + this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }
        this.cycleStopwatch.reset();
        this.cycleStopwatch.start();

        if (this.telegramQueue.isEmpty()) {
            this.createTelegram();
        }

        while (this.telegramQueue.isEmpty() == false) {
            // Check connection.
            if (this.parent.connectionHandler.checkConnectionStatus() == false) {
                // If checkStatus() returns false, the connection is lost. Try to reconnect
                this.parent.connectionOk = this.parent.connectionHandler.startConnection(this.parent.portName);
                this.deviceList.forEach(pumpDevice -> {
                    if (this.parent.connectionOk == false) {
                        // Reset device in case pump was changed or restarted.
                        pumpDevice.setConnectionOk(false);
                        pumpDevice.resetDevice();
                    }
                });
            }
            if (this.parent.connectionOk) {
                try {
                    Telegram telegram = this.telegramQueue.takeLast();
                    long startSendTimestamp = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayCurrentRun;
                    if (this.parent.getDebug()) {
                        this.parent.logInfo(this.log, "Stopwatch 1: " + startSendTimestamp);
                    }
                    this.parent.handleTelegram(telegram);    // This sends the telegram.
                    if (this.parent.getDebug()) {
                        this.parent.logInfo(this.log, "Stopwatch 2: " + (this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayCurrentRun));
                    }

                    /* If the telegram was executed successfully, measure how long it took. Calculate time per byte and
                       store it in the pump device. This value is used to estimate telegram send times and with this
                       information adjust the timeout settings based on telegram size. */
                    if (telegram.getPumpDevice().isConnectionOk()) {
                        long executionDuration = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayCurrentRun - startSendTimestamp;
                        int telegramByteLength = Byte.toUnsignedInt(telegram.getLength()) - 2
                                + telegram.getAnswerTelegramPduLength();
                        int emptyTelegramTime = telegram.getPumpDevice().getEmptyTelegramTime();
                        if (this.parent.getDebug()) {
                            this.parent.logInfo(this.log, "Estimated telegram execution time was: "
                                    + (emptyTelegramTime + telegramByteLength * telegram.getPumpDevice().getMillisecondsPerByte())
                                    + " ms. Actual time: " + executionDuration + " ms. Ms/byte = "
                                    + telegram.getPumpDevice().getMillisecondsPerByte());
                        }

                        /* Check if the telegram is suitable for timing measurement. Measurement error gets bigger the
                           smaller the telegram, so exclude tiny telegrams. */
                        if (telegramByteLength > 10 && executionDuration > emptyTelegramTime) {
                            /* Calculate "millisecondsPerByte", then store it in the pump device.
                               Calculation: An empty telegram with no tasks takes ~33 ms to send and receive. When tasks
                               are added to the telegram, the additional time is then proportional to the amount of bytes
                               the tasks added (request and answer). "millisecondsPerByte" is the average amount of time
                               each byte of a task adds to the telegram. */
                            telegram.getPumpDevice().setMillisecondsPerByte((executionDuration - emptyTelegramTime) / (telegramByteLength * 1.0));
                        }
                        if (telegramByteLength == 0) {
                            telegram.getPumpDevice().setEmptyTelegramTime((int)executionDuration);
                            if (this.parent.getDebug()) {
                                this.parent.logInfo(this.log, "Empty Telegram detected. Updating emptyTelegramTime (currently "
                                        + emptyTelegramTime + " ms).");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    this.parent.logWarn(this.log, "Couldn't get telegram. " + e);
                }

                /* Check time to decide if another telegram should be sent or not. The stop watch measures time since the
                   event TOPIC_CYCLE_EXECUTE_WRITE.
                   This time check is for the purpose of synchronization with the openEMS cycle, and to prevent telegram
                   creation while the controllers execute their code. Controller code should be executed a few
                   milliseconds before TOPIC_CYCLE_EXECUTE_WRITE, so a 100 ms buffer should suffice.
                   Telegram creation should be avoided while the controllers execute their code for consistency.
                   Controllers potentially change ’nextWriteValue’ of the channels, and creating a telegram will
                   manipulate these values as well. ’nextWriteValue’ of channels should not be changed by other code
                   during telegram creation.
                   Sending and receiving a telegram during controller execution should not be a problem. */
                if ((this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) + this.delayCurrentRun) < this.cycleTimeMs - 100) {
                    /* There is enough time, create another telegram. The createTelegram() method checks if a telegram
                       has already been sent to this device this cycle, and will then only fill it with tasks that have
                       not been executed yet this cycle. If all tasks were already executed, no telegram is created. */
                    this.createTelegram();

                    // If no telegram was created and put in the queue (the device had nothing to send), check if the other
                    // devices still have tasks. The createTelegram() method iterates over the device list with each call.
                    if (this.telegramQueue.isEmpty()) {
                        for (int i = 0; i < this.deviceList.size() - 1; i++) {   // "deviceList.size() - 1" because we already checked one device.
                            this.createTelegram();
                            if (this.telegramQueue.isEmpty() == false) {
                                // Exit for-loop if a telegram was created.
                                break;
                            }
                        }
                    }
                }
            }
        }
        this.delayLastRun = this.delayCurrentRun;
    }

    /**
     * Add a device to the Genibus bridge.
     * @param pumpDevice the device.
     */
    public void addDevice(PumpDevice pumpDevice) {
        synchronized (this.deviceList) {
            this.deviceList.add(pumpDevice);
        }
    }

    /**
     * Remove a device from the Genibus bridge.
     * @param deviceId the device.
     */
    public void removeDevice(String deviceId) {
        synchronized (this.deviceList) {
            for (int counter = 0; counter < this.deviceList.size(); counter++) {
                if (this.deviceList.get(counter).getPumpDeviceId().equals(deviceId)) {
                    this.deviceList.remove(counter);
                    /* There should be just one pumpDevice per deviceId. Could break here, since that pumpDevice has been
                       found and removed. Just to be safe, keep searching the deviceList for duplicates and remove them
                       as well. If somehow duplicate deviceIds exist, this will help expose that error.
                       Decrease counter to not skip an entry. */
                    counter--;
                }
            }
        }
    }

    /**
     * Set the OpenEMS cycle time, unit is milliseconds.
     * @param cycleTime the cycle time.
     */
    public void setCycleTime(int cycleTime) {
        this.cycleTimeMs = cycleTime;
    }

    /**
     * Gets the runIdentifier.
     * @return the runIdentifier.
     */
    public int getRunIdentifier() {
        return this.runIdentifier;
    }
}