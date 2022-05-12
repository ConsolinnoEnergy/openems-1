package io.openems.edge.bridge.rest;

import io.openems.edge.bridge.rest.api.RestBridge;
import io.openems.edge.bridge.rest.api.RestReadRequest;
import io.openems.edge.bridge.rest.api.RestRequest;
import io.openems.edge.bridge.rest.api.RestWriteRequest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This RestBridge allows the communication to different OpenEMS-Edge Systems.
 * {@link RestRequest}s are created by RestRemoteDevices.
 * Reading from or Writing to a Channel of a decentralized Edge System.
 * This allows the ability to control/Monitor/react to different Edge Systems.
 * To prevent locks while reading/writing when the connection is interrupted, the Read and Write Tasks will be Executed within a Thread.
 * When the thread takes more than 10 Seconds to execute -> there must be an error, either HTTP doesn't work, the path is wrong or the Client is down.
 * the thread will be shut down and after Time X the Connections will be checked again.
 * If ANY request is not working -> ConnectionOk is set to false. This will be used by e.g. the CommunicationMaster
 * enabling a Fallback handling when the Connection of the RestDevices are not ok.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Rest",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE})
public class RestBridgeImpl extends AbstractOpenemsComponent implements RestBridge, OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(RestBridgeImpl.class);

    private final Map<String, RestRequest> tasks = new ConcurrentHashMap<>();
    private final Map<String, RestRequest> readTasks = new ConcurrentHashMap<>();
    private final Map<String, RestRequest> writeTasks = new ConcurrentHashMap<>();
    private String loginData;
    private String ipAddressAndPort;
    private int keepAlive;
    AtomicBoolean connectionOk = new AtomicBoolean(true);
    DateTime initialDateTime;
    DateTime initialReadTime;
    DateTime initialWriteTime;
    private int readInterval;
    private int writeInterval;
    private boolean initialReadWasSet;
    private boolean initialWriteWasSet;
    private boolean initialDateTimeSet = false;
    private final AtomicBoolean readIsRunning = new AtomicBoolean(false);
    private final AtomicBoolean writeIsRunning = new AtomicBoolean(false);
    private static final int AWAIT_EXECUTOR_SHUTDOWN = 20;

    public RestBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    private enum TaskType {
        READ, WRITE
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
        this.checkIfAllConnectionsAreOkay();
    }

    private void activationOrModifiedRoutine(Config config) {
        if (config.enabled()) {
            this.loginData = "Basic " + Base64.getEncoder().encodeToString((config.username() + ":" + config.password()).getBytes());
            this.ipAddressAndPort = config.ipAddress() + ":" + config.port();
            this.keepAlive = config.keepAlive();
            this.readInterval = config.readInterval();
            this.writeInterval = config.writeInterval();
        }
    }

    Runnable runnableRead = () -> {
        this.readIsRunning.set(true);
        this.readTasks.forEach((key, entry) -> {
            try {
                this.handleReadRequest((RestReadRequest) entry);
            } catch (IOException e) {
                this.connectionOk.set(false);
            }
        });
    };

    /**
     * One of the three runnable in this class.
     * This runnable executes the taskRoutine for all the WRITE tasks.
     * It will be called within the EventHandler {@link #executeWrites()}
     */

    Runnable runnableWrite = () -> {
        this.writeIsRunning.set(true);
        this.writeTasks.forEach((key, entry) -> {
            try {
                this.handlePostRequest((RestWriteRequest) entry);
            } catch (IOException e) {
                this.connectionOk.set(false);
            }
        });
    };

    /**
     * One of the three runnable in this class.
     * This runnable executes the taskRoutine for CheckConnections.
     * It will be called within the EventHandler {@link #checkIfAllConnectionsAreOkay()}
     */

    Runnable runnableConnectionCheck = this::checkConnections;

    /**
     * Check the Connection. If it's ok, read / get Data in Before Process Image,
     * otherwise write into Channel in Execute Write.
     *
     * @param event the Event, either BeforeProcessImage or Execute Write
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() == false) {
            return;
        }
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                if (this.initialDateTimeSet == false) {
                    this.initialDateTime = new DateTime();
                    this.initialDateTimeSet = true;
                } else {
                    this.checkIfAllConnectionsAreOkay();
                }
                if (this.connectionOk.get() && this.readIsRunning.get() == false && this.shouldRead()) {
                    this.initialReadWasSet = false;
                    this.executeReads();
                }
                break;

            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                if (this.connectionOk.get() && this.writeIsRunning.get() == false && this.shouldWrite()) {
                    this.initialWriteWasSet = false;
                    this.executeWrites();
                }
                break;

        }

    }

    private boolean shouldWrite() {
        return this.shouldExecuteTask(TaskType.WRITE);
    }

    private boolean shouldRead() {
        return this.shouldExecuteTask(TaskType.READ);
    }

    private boolean shouldExecuteTask(TaskType taskType) {
        boolean shouldExecute;
        DateTime now = DateTime.now();
        DateTime compare = DateTime.now();
        int plusSeconds = 10;
        switch (taskType) {

            case READ:
                if (!this.initialReadWasSet) {
                    this.initialReadTime = DateTime.now();
                    this.initialReadWasSet = true;
                }
                compare = this.initialReadTime;
                plusSeconds = this.readInterval;
                break;
            case WRITE:
                if (!this.initialWriteWasSet) {
                    this.initialWriteTime = DateTime.now();
                    this.initialWriteWasSet = true;
                }
                compare = this.initialWriteTime;
                plusSeconds = this.writeInterval;
                break;
        }
        shouldExecute = now.isAfter(compare.plusSeconds(plusSeconds));
        return shouldExecute;
    }

    /**
     * Executes the Write Tasks.
     * This Method is called by {@link #handleEvent(Event)} and executes with the help of an ExecutorService the
     * runnable {@link #runnableWrite}. This allows to:
     * a) Run the tasks within a thread. b) shutdown of the executor. c) allows controlled shutdown after X seconds,
     * when the REST bridge fails to either open a connection or if the Input/OutputStreams are abruptly interrupted and
     * therefore in a locked state. This locked state prevents OpenEMS to run at all!
     * Therefore an await of the executor-shutdown is more than necessary.
     * Additionally: when the tasks cannot be executed in TimeFrame X -> connectionOk will be set to false.
     */

    private void executeWrites() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> this.runnableWrite.run());
        try {
            executorService.shutdown();
            executorService.awaitTermination(AWAIT_EXECUTOR_SHUTDOWN, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.log.error("Tasks Interrupted!");
        } finally {
            if (executorService.isTerminated() == false) {
                this.log.error("Non Finished WRITE TASKS will be canceled");
                this.connectionOk.set(false);
            }
            executorService.shutdownNow();
            this.writeIsRunning.set(false);
        }
    }


    /**
     * Executes the Read Tasks.
     * This Method is called by {@link #handleEvent(Event)} and executes with the help of an ExecutorService the
     * runnable {@link #runnableWrite}. This allows to:
     * a) Run the tasks within a thread. b) shutdown of the executor. c) allows controlled shutdown after X seconds,
     * when the REST bridge fails to either open a connection or if the Input/OutputStreams are abruptly interrupted and
     * therefore in a locked state. This locked state prevents OpenEMS to run at all!
     * Therefore an await of the executor-shutdown is more than necessary.
     */

    private void executeReads() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> this.runnableRead.run());
        try {
            executorService.shutdown();
            executorService.awaitTermination(AWAIT_EXECUTOR_SHUTDOWN, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.log.error("Tasks Interrupted!");
        } finally {
            if (executorService.isTerminated() == false) {
                this.log.error("Non Finished READ TASKS will be canceled");
                this.connectionOk.set(false);
            }
            executorService.shutdownNow();
            this.readIsRunning.set(false);
        }
    }

    /**
     * Checks All Connections if the connections are alive.
     * This Method is called by {@link #handleEvent(Event)} and executes with the help of an ExecutorService the
     * runnable {@link #runnableWrite}. This allows to:
     * a) Run the tasks within a thread. b) shutdown of the executor. c) allows controlled shutdown after X seconds,
     * when the REST bridge fails to either open a connection or if the Input/OutputStreams are abruptly interrupted and
     * therefore in a locked state. This locked state prevents OpenEMS to run at all!
     * Therefore an await of the executor-shutdown is more than necessary.
     */

    private void checkIfAllConnectionsAreOkay() {
        DateTime now = new DateTime();
        if (now.isAfter(this.initialDateTime.plusSeconds(this.keepAlive))) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                this.runnableConnectionCheck.run();
            });
            try {
                executorService.shutdown();
                executorService.awaitTermination(AWAIT_EXECUTOR_SHUTDOWN, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                this.log.error("Tasks Interrupted!");
            } finally {
                if (executorService.isTerminated() == false) {
                    this.log.error("Non Finished CONNECTION CHECKS will be canceled");
                    this.connectionOk.set(false);
                }
                executorService.shutdownNow();
            }
            this.initialDateTimeSet = false;
        }
    }

    /**
     * Check if the Connection is ok with given Id and Channel.
     *
     * @param value any Task.
     * @return responseCode == HTTP_OK
     */
    private boolean checkConnection(RestRequest value) {
        try {
            URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + value.getRequest());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", this.loginData);

            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Handles PostRequests called by the CycleWorker.
     *
     * @param entry the RestWriteRequest given by the CycleWorker. from this.tasks
     *              <p>
     *              Creates URL and if ReadyToWrite (can be changed via Interface) && isAutoAdaptation --> AutoAdaptRequest.
     *              AutoAdaptRequests is only necessary if Device is a Relays. --> IsCloser will be asked.
     *              Bc Opener and Closer have Inverse Logic. A Closer is Normally Open and an Opener is NormallyClosed,
     *              Therefore Changes in Relays needs to be Adapted. "ON" means true with closer but false with opener and
     *              vice versa.
     *              </p>
     * @throws IOException Bc of URL and connection.
     */

    private void handlePostRequest(RestWriteRequest entry) throws IOException {
        URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + entry.getRequest());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", this.loginData);

        if (entry.allowedToWrite()) {
            String msg = entry.getPostMessage();
            if (msg.equals("NoValueDefined") || msg.equals("NotReadyToWrite")) {
                return;
            }
            if (msg.equals("ChannelNotAvailable")) {
                this.log.warn("Channel for: " + entry.getDeviceId() + " is not available");
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(msg.getBytes());
            os.flush();
            os.close();
            //Task can check if everything's ok --> good for Controller etc; ---> Check Channel
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                entry.wasSuccess(true, entry.getDeviceId() + entry.getPostMessage());
            } else {
                entry.wasSuccess(false, "REST POST DID NOT WORK FOR ENTRY: " + entry.getDeviceId());
            }
        }
    }

    /**
     * Gets a RestRequest and creates the GET Rest Method.
     *
     * @param entry entry the RestWriteRequest given by the CycleWorker. from this.tasks
     * @throws IOException bc of URL requests etc.
     *                     <p>
     *                     Gets a Request via CycleWorker. Creates the URL and reacts if HTTP_OK is true
     *                     If that's the case, the response will be set to entry.
     *                     </p>
     */
    private void handleReadRequest(RestReadRequest entry) throws IOException {
        URL url = new URL("http://" + this.ipAddressAndPort + "/rest/channel/" + entry.getRequest());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", this.loginData);

        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String readLine;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuilder response = new StringBuilder();
            while ((readLine = in.readLine()) != null) {
                response.append(readLine);
            }
            in.close();
            //---------------------//
            entry.setResponse(true, response.toString());
            //---------------------//
        } else {
            entry.setResponse(false, "ERROR WITH CONNECTION");
        }
        connection.disconnect();
    }

    /**
     * This method checks all tasks, if either is not working -> set {@link #connectionOk} to false.
     */
    private void checkConnections() {
        AtomicBoolean connectionOkThisRun = new AtomicBoolean(true);
        this.tasks.forEach((key, value) -> {
            if (connectionOkThisRun.get()) {
                connectionOkThisRun.set(this.checkConnection(value));
            }
        });
        this.connectionOk.set(connectionOkThisRun.get());
    }

    /**
     * Is the Connection OK (Test Get request) Not ideal but it works.
     *
     * @return a boolean if connection is Ok.
     */

    @Override
    public boolean connectionOk() {
        return this.connectionOk.get();
    }

    /**
     * Adds the RestRequest to the tasks map.
     *
     * @param id      identifier == remote device Id usually from Remote Device config
     * @param request the RestRequest created by the Remote Device.
     * @throws ConfigurationException if the id is already in the Map.
     */
    @Override
    public void addRestRequest(String id, RestRequest request) throws ConfigurationException {

        if (this.tasks.containsKey(id)) {
            this.log.warn("ID : " + id + "Already in List, please Check your config. The previous Task will be removed now!");
        }
        if (request instanceof RestWriteRequest) {
            this.writeTasks.put(id, request);
        } else if (request instanceof RestReadRequest) {
            this.readTasks.put(id, request);
        }
        this.tasks.put(id, request);
        AtomicBoolean connOk = new AtomicBoolean(true);
        this.log.info("Trying to check for Connection of RestRequest: " + id);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Runnable runnableSingleConnectionCheck = new Runnable() {
                RestRequest checkRequest;
                private AtomicBoolean connectionOk;

                @SuppressWarnings("checkstyle:RequireThis")
                @Override
                public void run() {
                    this.connectionOk.set(checkConnection(this.checkRequest));
                }

                public Runnable init(RestRequest req, AtomicBoolean atomicBoolean) {
                    this.checkRequest = req;
                    this.connectionOk = atomicBoolean;
                    return (this);
                }
            }.init(request, connOk);
            runnableSingleConnectionCheck.run();
            try {
                executorService.shutdown();
                executorService.awaitTermination(AWAIT_EXECUTOR_SHUTDOWN, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                this.log.error("Tasks Interrupted!");
            } finally {
                if (executorService.isTerminated() == false) {
                    this.log.error("Non Finished CONNECTION CHECKS will be canceled");
                    connOk.set(false);
                }
                executorService.shutdownNow();
            }

        });
        if (connOk.get() == false) {
            this.tasks.remove(id);
            throw new ConfigurationException("AddRestRequest", "Internet Or Path Wrong for RemoteDevice, Host can be down too! " + id);
        }
    }

    /**
     * Removes a Remote device from the Bridge.
     * Usually called by RestRemote Component on deactivation or when the Bridge itself deactivates.
     *
     * @param deviceId the deviceId to Remove.
     */
    @Override
    public void removeRestRemoteDevice(String deviceId) {
        this.tasks.remove(deviceId);
        this.writeTasks.remove(deviceId);
        this.readTasks.remove(deviceId);
    }

    /**
     * Get all requests from the RestBridge.
     *
     * @return all of the stored RestRequests
     */
    @Override
    public Map<String, RestRequest> getAllRequests() {
        return this.tasks;
    }


    /**
     * Deactivates the Component.
     */
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

}
