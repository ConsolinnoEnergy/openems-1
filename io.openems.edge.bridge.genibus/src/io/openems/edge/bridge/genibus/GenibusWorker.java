package io.openems.edge.bridge.genibus;

import com.google.common.base.Stopwatch;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import io.openems.edge.bridge.genibus.api.task.HeadClass4and5;
import io.openems.edge.bridge.genibus.protocol.ApplicationProgramDataUnit;
import io.openems.edge.bridge.genibus.protocol.Telegram;
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
 * This worker organizes the contents of the telegrams to send to the pumps and which pump to send them to.
 */
class GenibusWorker extends AbstractCycleWorker {

    private final Logger log = LoggerFactory.getLogger(GenibusWorker.class);
    private final Stopwatch cycleStopwatch = Stopwatch.createUnstarted();
    private final LinkedBlockingDeque<Telegram> telegramQueue = new LinkedBlockingDeque<>();
    private final ArrayList<PumpDevice> deviceList = new ArrayList<>();
    private int currentDeviceCounter = 0;
    private final GenibusImpl parent;
    private long cycleTimeMs = 1000;    // Start with 1000 ms, then measure the actual cycle time.
    private long lastExecutionMillis;


    protected GenibusWorker(GenibusImpl parent) {
        this.parent = parent;
    }

    @Override
    public void activate(String name) {
        super.activate(name);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }


    // Creates a telegram for the current device (if possible) and increments the currentDeviceCounter (with a few exceptions).
    // The telegram is built from a task list of priority high, low and once tasks. This list is created by the module
    // that implements the pump device.
    // Picking tasks for the telegram works as follows: from the list of tasks, all high tasks are added to taskQueue.
    // Then a number of low tasks is added to tasksQueue. How many low tasks are added is defined by the pump device
    // module. Then as many tasks as possible are taken from tasksQueue and added to the telegram. Tasks added to the
    // telegram are removed from tasksQueue. If there are still tasks in taskQueue once the telegram is full, they
    // remain there and will be processed next time a telegram is created for this device. The queue is refilled as
    // needed and checks are in place to prevent the queue from getting too big.
    // All priority once tasks are added to the queue on the first run after the high tasks.
    //
    // If this method is called multiple times in a cycle for the same device, all possible tasks are only executed once.
    // Once the regularly scheduled tasks have been executed (high tasks + defined number of low tasks), a telegram with
    // the remaining (unscheduled) low tasks is created. Further calls of this method in the same cycle for the same
    // device will then not create a telegram but only switch to the next device (increase currentDeviceCounter).
    protected void createTelegram() {
        if (this.deviceList.isEmpty()) {
            if (this.parent.getDebug()) {
                this.parent.logInfo(this.log, "No devices registered for the GENIbus bridge.");
            }
            return;
        }
        PumpDevice currentDevice = this.deviceList.get(0);   // Only done so Intellij does not complain about "variable might not be initialized".

        // This handles the switching to the next pump. Selects the next pump in line, unless that pump is offline.
        for (int i = 0; i < this.deviceList.size(); i++) {
            if (this.deviceList.size() <= this.currentDeviceCounter) {
                this.currentDeviceCounter = 0;
            }
            currentDevice = this.deviceList.get(this.currentDeviceCounter);

            // The timeoutCounter is 0 when the device is responding. If the device has not responded 3 times in a row,
            // pump is most likely offline or wrong address. Wait 5 seconds before trying again, otherwise skip the pump.
            if (currentDevice.getTimeoutCounter() > 2) {
                long timeSinceLastTry = System.currentTimeMillis() - currentDevice.getTimestamp();
                if (timeSinceLastTry > 5000 || timeSinceLastTry < 0) {  // < 0 for overflow protection.
                    break;
                }
                this.currentDeviceCounter++;
            } else {
                break;
            }
        }

        // Remaining bytes that the answer telegram can have. You can cause an output buffer overflow in the device too.
        // This is achieved by sending lots of tasks that can have more bytes in the answer than in the request such as
        // INFO and ASCII. An INFO answer can be 1 or 4 bytes (for 8 bit tasks), an ASCII answer any amount of bytes.
        // Because of that, ASCII tasks are put in a telegram all by themselves. INFO tasks are assumed to have the
        // maximum byte count.
        // Calculated by getting the buffer length of the device and subtracting the telegram header (4 bytes) and the
        // crc (2 bytes).
        int responseTelegramRemainingBytes = currentDevice.getDeviceSendBufferLengthBytes() - 6;

        // A timestamp is done on the first execution in a cycle (per device). So this is the time since the last first
        // execution. Used to track if more than one telegram is sent to the same device in a cycle.
        this.lastExecutionMillis = System.currentTimeMillis() - currentDevice.getTimestamp();
        // Rollover backup. Just in case this software actually runs that long...
        if (this.lastExecutionMillis < 0) {
            this.lastExecutionMillis = this.cycleTimeMs;
        }

        // If connection to pump is lost (pump turned off for example), send an empty telegram to test if the pump is
        // back online. If an answer is received, isConnectionOk() returns true next time and normal execution will resume.
        if (currentDevice.isConnectionOk() == false) {
            if (this.lastExecutionMillis > this.cycleTimeMs - 50) {
                currentDevice.setTimestamp();
                currentDevice.setFirstTelegram(true);
                if (this.parent.getDebug()) {
                    this.parent.logInfo(this.log, "Sending empty telegram to test if device " + currentDevice.getPumpDeviceId() + " is online.");
                }
                this.sendEmptyTelegram(currentDevice);
            }
            this.currentDeviceCounter++;
            return;
        }

        // Estimate how big the telegram can be to still fit in this cycle.
        long remainingCycleTime = this.cycleTimeMs - 60 - this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);  // Include 60 ms buffer.
        if (remainingCycleTime < 0) {
            remainingCycleTime = 0;
        }
        // Each byte adds a certain time to the telegram execution length. The remaining time in the cycle can then be
        // expressed as "cycleRemainingBytes".
        // A telegram with no tasks takes ~33 ms to send and receive. Each additional byte in the telegrams (request and
        // answer) adds ~1 ms to that. We only calculate for the request bytes here, so divide by 2 as a good estimate.
        // A buffer of 60 ms was used in the time calculation, the 33 ms base time for a telegram is included in that buffer.
        int cycleRemainingBytes = (int) (remainingCycleTime / (currentDevice.getMillisecondsPerByte() * 2));
        if (cycleRemainingBytes < 5) {
            // Not enough time left. The telegram would be so small that it is not worth sending.
            // Exit the method without changing the device.
            if (this.parent.getDebug()) {
                this.parent.logInfo(this.log, "Not enough time left this cycle to send a telegram.");
            }
            return;
        }

        // Remaining bytes that can be put in this telegram. More bytes will result in a buffer overflow in the device
        // and there will be no answer.
        // Calculated by getting the buffer length of the device and subtracting the telegram header (4 bytes) and the
        // crc (2 bytes).
        int requestTelegramRemainingBytes = currentDevice.getDeviceReadBufferLengthBytes() - 6;

        // Reduce telegram byte count if time is too short
        if (cycleRemainingBytes < requestTelegramRemainingBytes) {
            // If this is the first telegram to that device this cycle and the telegram has a greatly reduced byte count
            // because of time constraints, don't switch to the next device. So the device is guaranteed to have a full
            // size telegram in the next cycle. This avoids one device being stuck at the end of the cycle and thus only
            // getting small telegram sizes.
            if (this.lastExecutionMillis > this.cycleTimeMs - 50 && cycleRemainingBytes / (requestTelegramRemainingBytes * 1.0) < 0.3) {
                this.currentDeviceCounter--;
            }
            double divisor = cycleRemainingBytes / requestTelegramRemainingBytes;
            requestTelegramRemainingBytes = cycleRemainingBytes;
            responseTelegramRemainingBytes = (int) Math.round(responseTelegramRemainingBytes * divisor);
        }
        this.currentDeviceCounter++;

        this.fillTaskQueue(currentDevice, requestTelegramRemainingBytes);

        // Get taskQueue from the device. Tasks that couldn't fit in the last telegram stayed in the queue.
        List<GenibusTask> tasksQueue = currentDevice.getTaskQueue();

        if (tasksQueue.isEmpty()) {
            // Don't send when it's broadcast.
            if (currentDevice.isEmptyTelegramSent() == false && currentDevice.getGenibusAddress() != 254) {
                currentDevice.setEmptyTelegramSent(true);
                this.sendEmptyTelegram(currentDevice);
                return;
            }

            // Nothing useful left to do. No tasks could be found that were not already executed this cycle.
            if (this.parent.getDebug()) {
                this.parent.logInfo(this.log, "No tasks left this cycle for pump number "
                        + this.currentDeviceCounter + ". Time since last timestamp: " + this.lastExecutionMillis + " ms.");
                // currentDeviceCounter has already been incremented at this point, but that is fine. First pump in the
                // list is now displayed as "pump number 1", while the deviceCounter number would be 0.
            }
            return;
        }

        if (this.parent.getDebug()) {
            this.parent.logInfo(this.log, "--Telegram Builder--");
            this.parent.logInfo(this.log, "Number of pumps in list: " + this.deviceList.size()
                    + ", current pump number: " + (this.currentDeviceCounter) + ", current pump id: " + currentDevice.getPumpDeviceId()
                    + ", GENIbus address: " + currentDevice.getGenibusAddress()
                    + ", Time since last timestamp: " + this.lastExecutionMillis + " ms.");
        }

        // This list contains all tasks for the current telegram. The key is a 3 digit decimal number called the
        // apduIdentifier. The 100 digit is the HeadClass of the apdu, the 10 digit is the operation
        // (0=get, 2=set, 3=info), the 1 digit is a counter starting at 0. The counter allows to have more than one apdu
        // of a given type. Since an apu (request and answer) is limited to 63 bytes, several apdu of the same type
        // might be needed to fit all tasks.
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

        // Need separate counter as start count of responseTelegramRemainingBytes is dynamic.
        int answerByteCounter = 0;

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

            // When taskSendByteCount is 0, this task does nothing (for example a command task with no command to send).
            // Don't add this task to the telegram and remove it from the task queue.
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
            boolean currentTaskIsAscii = currentTask.getHeader() == 7;
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

        // Upper limit estimate. Not the actual length. That is set once the answer telegram has been received. +2 for crc.
        telegram.setAnswerTelegramLength(answerByteCounter + 2);

        telegramApduList.forEach((key, apdu) -> {
            telegram.getProtocolDataUnit().putApdu(apdu);
        });
        telegram.setTelegramTaskList(telegramTaskList);

        this.telegramQueue.add(telegram);
    }

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
     * @param currentDevice the pump device for which to prepare tasks.
     * @param telegramRemainingBytes remaining bytes in the telegram.
     */
    private void fillTaskQueue(PumpDevice currentDevice, int telegramRemainingBytes) {
        List<GenibusTask> tasksQueue = currentDevice.getTaskQueue();

        // Priority low tasks are added with .getOneTask(), which starts from 0 when the end of the list is reached.
        // Make sure the number of low tasks added per telegram is not longer than the list to prevent adding a task twice.
        int lowTasksToAdd = currentDevice.getLowPrioTasksPerCycle();
        int numberOfLowTasks = currentDevice.getTaskManager().getAllTasks(Priority.LOW).size();
        if (lowTasksToAdd <= 0) {
            lowTasksToAdd = 1;
        }
        if (lowTasksToAdd > numberOfLowTasks) {
            lowTasksToAdd = numberOfLowTasks;
        }

        if (this.lastExecutionMillis > this.cycleTimeMs - 50 || currentDevice.isFirstTelegram()) {
            // Check if this was already executed this cycle. This part should execute once per cycle only.
            // isFirstTelegram is true when connection was lost and has just been reestablished.

            currentDevice.setTimestamp();
            currentDevice.setAllLowPrioTasksAdded(false);
            currentDevice.setFirstTelegram(false);
            currentDevice.setEmptyTelegramSent(false);

            // Check content of taskQueue. If length is longer than numberOfHighTasks + lowTasksToAdd (=number of tasks
            // this method would add), all high tasks are already in the queue and don't need to be added again.
            int numberOfHighTasks = currentDevice.getTaskManager().getAllTasks(Priority.HIGH).size();
            if (tasksQueue.size() <= numberOfHighTasks + lowTasksToAdd) {
                // Add all high tasks
                tasksQueue.addAll(currentDevice.getTaskManager().getAllTasks(Priority.HIGH));

                // Add all once tasks that need a second execution because the first execution was only INFO. This list
                // should be empty after the second or third cycle and won't be refilled.
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
                            case 2:
                            case 4:
                            case 5:
                                currentDevice.getOnceTasksWithInfo().add(onceTask);
                        }
                    });
                    currentDevice.setAddAllOnceTasks(false);
                }

                // Add a number of low tasks.
                for (int i = 0; i < lowTasksToAdd; i++) {
                    GenibusTask currentTask = currentDevice.getTaskManager().getOneTask(Priority.LOW);
                    if (currentTask == null) {
                        break;
                    } else {
                        tasksQueue.add(currentTask);
                    }
                }
            }
        } else {
            // This executes if a telegram was already sent to this pumpDevice in this cycle.
            // If the taskQueue is empty, fill the telegram with any remaining low priority tasks. If that was already
            // done this cycle, exit the method.
            if (tasksQueue.isEmpty()) {
                if (currentDevice.isAllLowPrioTasksAdded() == false) {
                    currentDevice.setAllLowPrioTasksAdded(true);
                    // The amount lowTasksToAdd was already added in the first telegram this cycle. The number of tasks
                    // before they repeat is then numberOfLowTasks - lowTasksToAdd.
                    int lowTaskFill = numberOfLowTasks - lowTasksToAdd;
                    // Compare that number with the bytes allowed in this telegram. We want to fill this telegram up
                    // with low tasks, but we don't want to add more tasks than can be sent with this telegram. Anything
                    // left in taskQueue is executed in the next telegram and reduces the space there for high tasks.
                    // Assume one task = one byte.
                    // The math is not exact but a good enough estimate. 1-2 low tasks remaining in taskQueue is not a big deal.
                    if ((telegramRemainingBytes - 2) < lowTaskFill) {    // -2 to account for at least one apdu header.
                        lowTaskFill = (telegramRemainingBytes - 2);
                    }

                    for (int i = 0; i < lowTaskFill; i++) {
                        GenibusTask currentTask = currentDevice.getTaskManager().getOneTask(Priority.LOW);
                        if (currentTask == null) {
                            // This should not happen, but checked just in case to prevent null pointer exception.
                            break;
                        } else {
                            tasksQueue.add(currentTask);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes the task at ’position’ from the tasksQueue.
     * Exception: if the task is head class 4 or 5 GET, because these can do SET as well. To do that, set the ’executeGet’
     * flag to false and leave it in the queue. This way it is immediately executed as SET.
     *
     * @param tasksQueue the task queue.
     * @param position the position in the task queue (first position is 0).
     */
    private void removeTaskFromQueue(List<GenibusTask> tasksQueue, int position) {
        GenibusTask currentTask = tasksQueue.get(position);
        if (currentTask instanceof HeadClass4and5 && currentTask.informationDataAvailable()) {
            boolean isGet = ((currentTask.getApduIdentifier() % 100) / 10) == 0;
            if (isGet) {
                // This task was executed as GET. Set ’executeGet’ to false and leave task in queue, to execute it immediately as SET.
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
     * <p>How to decide between INFO, GET or SET:
     * - Tasks of head class 3 can do INFO but don't need it. They only do SET.
     * - Tasks of head class 7 and 0 only do GET.
     * - Tasks of head class 2, 4 and 5 need the data from INFO to be able to do GET or SET. So INFO is done first, and
     *   GET or SET only allowed after the data from INFO has been saved to the task object. Info does not change (with
     *   a few exceptions) so only needs to be executed once.
     * - Tasks of head class 2 do INFO and GET. After INFO is received they only do GET.
     * - Tasks of head class 4 and 5 can do GET and SET. However, the GET data of this task only changes when a SET is
     *   executed. Thus, a GET only needs to be executed once at the beginning and then after a SET. A flag is used to
     *   control if a GET should be executed.</p>
     *
     * <p>The two basic factors for the byte count is the byte size of the task (i.e. a 16 bit task will need two bytes),
     * and if an APDU needs to be added to place this task. Adding an APDU adds the 2 bytes of its header to the
     * telegram.
     * Then the byte count is modified by the type of the task. SET tasks need to check if there is a write value to
     * send. If not, the task is not executed thus adds no bytes to the telegram (return 0). INFO tasks are always just
     * one byte, even if the task is more than 8 bit. All the necessary information is received by requesting INFO on
     * just the hi byte of multi byte tasks.</p>
     *
     * @param task the Genibus task for which to decide the APDU and calculate the number of bytes.
     * @param telegramApduList The list of APDUs already in this telegram.
     * @return the number of bytes this task would add to the telegram. Returns 0 if the task is not executed.
     */
    private int decideApduAndGetTaskByteCount(PumpDevice currentDevice, GenibusTask task, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
        int headClass = task.getHeader();
        int dataByteSize = task.getDataByteSize();
        switch (headClass) {
            case 0:
                task.setApduIdentifier(0);
                /* HeadClass 0 only has three commands. No more than one HeadClass 0 apdu will exist, so don't need to
                   check if there is more than one or if apdu is full. Key is 0 since GET is 0. */
                if (telegramApduList.containsKey(0)) {
                    return dataByteSize;
                } else {
                    return 2 + dataByteSize;
                }
            case 2: // Measured Data
                // Check if INFO has already been received. If yes, this task is GET. If not, this task is INFO.
                if (task.informationDataAvailable()) {  // Task is GET
                    /* See if an apdu exists. If yes, check remaining space.
                       apduIdentifier is (2*100 for HeadClass 2) + (0*10 for GET) + (0 for first apdu). */
                    return dataByteSize + this.getApduHeaderBytes(task, 200, telegramApduList);
                } else {    // Task is INFO
                    // apduIdentifier is (2*100 for HeadClass 2) + (3*10 for INFO) + (0 for first apdu).
                    return 1 + this.getApduHeaderBytes(task, 230, telegramApduList);
                }
            case 3: // Commands
                /* No need to check INFO. HeadClass 3 does allow INFO, but it is not needed. So HeadClass 3 is SET only.
                   Check if task is executed or not. Commands are boolean where ’true’ means ’send the command’ and
                   ’false’ means ’do nothing’. This is checked by ’isSetAvailable()’. */
                if (task.isSetAvailable()) {    // Execute task.
                    // apduIdentifier is (3*100 for HeadClass 3) + (2*10 for SET) + (0 for first apdu).
                    return dataByteSize + this.getApduHeaderBytes(task, 320, telegramApduList);
                } else {    // Don't execute task.
                    task.setApduIdentifier(320);    // Set apduIdentifier for debug info.
                    return 0;   // Task is not executed, so no bytes added to telegram.
                }
            case 4: // Configuration Parameters
            case 5: // Reference Values
                // Check if INFO has already been received. If yes, this task can be GET or SET.
                if (task.informationDataAvailable()) {
                    if (task instanceof HeadClass4and5) {
                        // Check flag if this should execute as GET. If not execute as SET.
                        if (((HeadClass4and5) task).getExecuteGet()) {  // Task is GET.
                            // apduIdentifier is (HeadClass*100) + (0*10 for GET) + (0 for first apdu).
                            return dataByteSize + this.getApduHeaderBytes(task, headClass * 100, telegramApduList);
                        } else {    // Task is SET.
                            // Check if there is a value to set. If not, don't execute the task.
                            if (task.isSetAvailable()) {
                                // Byte pattern for SET with a value is: address (1 byte) + value (x byte).
                                // apduIdentifier is (HeadClass*100) + (2*10 for SET) + (0 for first apdu).
                                return 1 + dataByteSize + this.getApduHeaderBytes(task, (headClass * 100) + 20, telegramApduList);
                            } else {
                                // Set apduIdentifier for debug readout.
                                task.setApduIdentifier((headClass * 100) + 20);
                                return 0;   // Task is not executed, so no bytes added to telegram.
                            }
                        }
                    } else {
                        // This should never happen, but just in case.
                        this.parent.logError(this.log, "GENIbus error. Wrong headclass for task "
                                + task.getHeader() + ", " + task.getAddress() + ". Can't execute.");
                        return 0;
                    }
                } else {    // Task is INFO
                    // apduIdentifier is (HeadClass*100) + (3*10 for INFO) + (0 for first apdu).
                    return 1 + this.getApduHeaderBytes(task, (headClass * 100) + 30, telegramApduList);
                }
            case 7:
                /* HeadClass 7 is ASCII, which has only GET. Each ASCII task needs it's own APDU, to be save from
                   buffer overflow. Return maximum APDU byte count.
                   This is not as straight forward, as pumps with a read buffer size of 70 bytes exist.
                   The telegram header is 4 bytes, and the CRC is 2 bytes. This leaves 64 bytes for the APDU. This is
                   lower than the maximum allowed APDU byte count (63 bytes data + 2 bytes header).
                   apduIdentifier is (7*100 for HeadClass 7) + (0*10 for GET) + (0 for first apdu). */
                task.setApduIdentifier(700);
                int spaceInTelegramForApdu = currentDevice.getDeviceReadBufferLengthBytes() - 6;
                return Math.min(spaceInTelegramForApdu, 65);
        }
        this.parent.logError(this.log, "GENIbus error. Unsupported headclass for task "
                + task.getHeader() + ", " + task.getAddress() + ". Can't execute.");
        return 0;
    }



    /**
     * Helper method for decideApduAndGetTaskByteCount(). Returns the number of bytes that are added because of APDU
     * requirements. Returns 0 if no extra APDU is needed, otherwise 2.
     * If an additional APDU is needed in the telegram to fit this task, the byte count increases by 2 because of the
     * APDU header. The parameters ’apduIdentifier’ and ’apduMaxBytes’ are used to decide if a task can fit in an already
     * existing APDU or not.
     * As this method checks in which apdu a task can be placed, the resulting apduIdentifier is saved to the task
     * object so it can be used later when the task is actually added to an apdu.
     *
     * <p>Parameters:
     * apduIdentifier - put the apduIdentifier of the first APDU of this type that would be in the telegram. That means
     * the last digit is a 0.
     * apduMaxBytes - an APDU can have up to 63 bytes. However, care must be taken to assure that the response APDU also
     * stays below that limit, as otherwise the whole telegram will be canceled because of buffer overflow. There is no
     * inbuilt protection to prevent that, it is totally possible to kill the communication by sending valid request
     * telegrams to which there cannot be a response because of buffer overflow.
     * The problem is, one byte in the request APDU can result in more than one byte in the answer APDU. The problematic
     * task types are INFO and ASCII. INFO has one byte per task for the request, but up to 4 byte per task for the
     * response. INFO request APDUs should then be limited to 15 bytes to be safe. The response to an ASCII request can
     * be any number of bytes. To be able to process the response, an ASCII request ADPU can only have one task (= 1 byte)
     * in it.</p>
     *
     * @param task the Genibus task for which to calculate the APDU header bytes.
     * @param apduIdentifier the apduIdentifier of the first APDU this task could be placed in.
     * @param telegramApduList The list of APDUs already in this telegram.
     * @return the number of bytes added to the telegram because of an additional APDU.
     */
    private int getApduHeaderBytes(GenibusTask task, int apduIdentifier, Map<Integer, ApplicationProgramDataUnit> telegramApduList) {
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
        int apduMaxBytes = 63;
        boolean isInfo = ((apduIdentifier % 100) / 10) == 3;
        if (isInfo) {
            apduMaxBytes = 15;
        } else if (task.getHeader() == 7) {
            apduMaxBytes = 1;
        }
        int dataByteSize = task.getDataByteSize();
        int remainingBytes = apduMaxBytes - telegramApduList.get(nextFreeApdu - 1).getLength();
        if (remainingBytes >= dataByteSize) {
            task.setApduIdentifier(nextFreeApdu - 1);
            return 0; // Task fits in existing APDU. No new APDU needed, no bytes added because of APDU header.
        }
        task.setApduIdentifier(nextFreeApdu);
        return 2;   // Task does not fit in existing APDU. Add 2 bytes because of header of new APDU.
    }

    // Returns how many bytes this task would need in the answer telegram if it were added to the send telegram. This is
    // an upper estimate and not an accurate value. For example, INFO can be a 1 or 4 byte answer -> upper limit is 4 bytes.
    // Additional bytes needed for an apdu header are taken from "sendByteSize". If the request is in a new apdu, so is
    // the answer.
    private int checkAnswerByteSize(GenibusTask task, int sendByteSize) {
        int operationSpecifier = (task.getApduIdentifier() % 100) / 10;
        if (task.getHeader() == 7) {
            // Task is ASCII. Don't know how long that answer will be, 30 is a conservative guess. The number here
            // doesn't really matter because if the task is ASCII it should be the only task in the telegram anyway.
            return 30;
        }
        switch (operationSpecifier) {
            case 0: // GET
                return sendByteSize;
            case 2: // SET
                // The answer to a SET APDU is an empty APDU.
                return sendByteSize - task.getDataByteSize();   // 2 if it is a new apdu. Otherwise 0.
            case 3: // INFO
                return 4;
        }
        return 0;
    }


    // Adds a task to the telegramTaskList and the telegramApduList.
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
            // INFO needs just one byte, even for multi byte tasks
            numberOfBytesInApdu = 1;
        }
        for (int i = 0; i < numberOfBytesInApdu; i++) {
            telegramApduList.get(apduIdentifier).putDataField(currentTask.getAddress() + i);
            // Add write value for write task.
            switch (currentTask.getHeader()) {
                case 3:
                    if (i == 0) {   // Commands should always be just one byte
                        // Reset channel write value to send command just once.
                        currentTask.clearNextWriteAndUpdateChannel();
                    } else {
                        // Should not trigger, but just in case.
                        this.parent.logWarn(this.log, "Code error. Command task " + currentTask.getHeader()
                                + ", " + currentTask.getAddress() + " for APDU " + apduIdentifier + " should be one byte only, "
                                + "but it is not!");
                    }
                    break;
                case 4:
                case 5:
                    boolean isSet = ((apduIdentifier % 100) / 10) == 2;
                    if (isSet) {
                        int unsignedByteForApdu = currentTask.getByteIfSetAvailable(i);
                        if (unsignedByteForApdu < 0 || unsignedByteForApdu > 255) {
                            // Should not trigger, but just in case.
                            this.parent.logWarn(this.log, "Code error. Write value from task " + currentTask.getHeader()
                                    + ", " + currentTask.getAddress() + " for APDU " + apduIdentifier + " should be a byte, "
                                    + "but it is not!");
                        }
                        telegramApduList.get(apduIdentifier).putDataField((byte) unsignedByteForApdu);
                        if (i >= (currentTask.getDataByteSize() - 1)) {
                            // All bytes collected now, reset channel write value to send it just once.
                            currentTask.clearNextWriteAndUpdateChannel();
                        }
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
     *
     */
    @Override
    protected void forever() {
        if (this.cycleStopwatch.isRunning()) {
            this.cycleTimeMs = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
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
                    long timeCounterTimestamp = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
                    if (this.parent.getDebug()) {
                        this.parent.logInfo(this.log, "Stopwatch 1: " + this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }
                    long cycletimeLeft = this.cycleTimeMs - timeCounterTimestamp;
                    this.parent.handleTelegram(telegram, cycletimeLeft);
                    if (this.parent.getDebug()) {
                        this.parent.logInfo(this.log, "Stopwatch 2: " + this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }

                    // If the telegram was executed successfully, measure how long it took. Calculate time per byte and
                    // store it in the pump device. This value is later retrieved to check if a telegram for this pump
                    // could still fit into the remaining time of the cycle.
                    if (telegram.getPumpDevice().isConnectionOk()) {
                        long executionDuration = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) - timeCounterTimestamp;
                        int telegramByteLength = Byte.toUnsignedInt(telegram.getLength()) - 2 // Subtract crc
                                + telegram.getAnswerTelegramLength() - 2;
                        int emptyTelegramTime = telegram.getPumpDevice().getEmptyTelegramTime();
                        if (this.parent.getDebug()) {
                            this.parent.logInfo(this.log, "Estimated telegram execution time was: "
                                    + (emptyTelegramTime + telegramByteLength * telegram.getPumpDevice().getMillisecondsPerByte())
                                    + " ms. Actual time: " + executionDuration + " ms. Ms/byte = "
                                    + telegram.getPumpDevice().getMillisecondsPerByte());
                        }

                        // Check if the telegram is suitable for timing calculation. Calculation error gets bigger the
                        // smaller the telegram, so exclude tiny telegrams.
                        if (telegramByteLength > 10 && executionDuration > emptyTelegramTime) {
                            // Calculate "millisecondsPerByte", then store it in the pump device.
                            // Calculation: An empty telegram with no tasks takes ~33 ms to send and receive. When tasks
                            // are added to the telegram, the additional time is then proportional to the amount of bytes
                            // the tasks added (request and answer). "millisecondsPerByte" is the average amount of time
                            // each byte of a task adds to the telegram.
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

                // Check if there is enough time for another telegram. The telegram length is dynamic and adjusts depending
                // on time left in the cycle. A short telegram can be sent and received in ~50 ms.
                if (this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS) < this.cycleTimeMs - 100) {
                    // There should be enough time. Create the telegram. The createTelegram() method checks if a telegram
                    // has already been sent this cycle and will then only fill it with tasks that have not been executed
                    // this cycle. If all tasks were already executed, no telegram is created.
                    this.createTelegram();

                    // If no telegram was created and put in the queue (the device had nothing to send), check if the other
                    // devices still have tasks.
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
    }

    public void addDevice(PumpDevice pumpDevice) {
        this.deviceList.add(pumpDevice);
    }

    public void removeDevice(String deviceId) {
        for (int counter = 0; counter < this.deviceList.size(); counter++) {
            if (this.deviceList.get(counter).getPumpDeviceId().equals(deviceId)) {
                this.deviceList.remove(counter);
                // decrease counter to not skip an entry.
                counter--;
            }
        }
    }
}
