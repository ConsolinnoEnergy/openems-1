package io.openems.edge.controller.heatnetwork.communication.request.manager;

import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequestManager;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.timer.api.TimerType;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Get Requests as Input, Determined by Position and List<Request>  (Map <Integer, List<Request>)
 * getManagedRequests (After Request handling these will be the current Requests)
 * setMaximumAllowedRequests
 */
public class RestRequestManagerImpl implements RestRequestManager {


    private enum WaitWork {
        WAIT, WORK
    }

    private boolean manageAllAtOnce;
    private int maxRequestsAtOnce;
    private ManageType manageType = ManageType.FIFO;
    private Map<Integer, List<RestRequest>> allRequests;
    private final Map<Integer, List<RestRequest>> managedRequests = new HashMap<>();
    //Maps for Timer by Time
    private final Map<Integer, DateTime> waitTime = new HashMap<>();
    private final Map<Integer, DateTime> workTime = new HashMap<>();
    //Maps for Timer by Cycles
    private final Map<Integer, AtomicInteger> waitCycles = new HashMap<>();
    private final Map<Integer, AtomicInteger> workCycles = new HashMap<>();
    private int maxWaitTime = 10;
    private TimerType timerType = TimerType.TIME;
    private final List<Integer> waitList = new ArrayList<>();


    /**
     * Usually Called By CommunicationMaster.
     * Give all of the Requests and handle them by ManageType (e.g. FIFO) and Maximum Requests.
     * Includes a Waitlist
     *
     * @param allRequests All Requests mapped by Integer (Usually a Position/Number)
     */
    @Override
    public void manageRequests(Map<Integer, List<RestRequest>> allRequests) {
        this.allRequests = allRequests;
        if (this.timerType.equals(TimerType.CYCLES)) {
            this.workCycles.forEach((key, value) -> value.getAndIncrement());
        }
        manageByManageType();

    }

    /**
     * ManageRequest by ManageType.
     * ATM Only FIFO.
     */
    private void manageByManageType() {
        if (this.manageType.equals(ManageType.FIFO)) {
            manageByFiFo();
        }
    }

    /**
     * Clear Executed Requests
     * ManageWaitList (Prevent Starvation)
     * Iterate through all Requests by Key and check if there is a Request (Usually 1 if request and 0 if no request)
     * If Request is available --> Put into Managed Requests if Size ok. else put into waitList.
     */
    private void manageByFiFo() {
        clearExecutedRequests();
        manageWaitList();
        swapKeyIfWaitTimeIsUp();
        //only check Requests that are neither in managedRequests already nor in Waitlist <-- REMAINING REQUESTS!
        this.allRequests.keySet().stream()
                .filter(containingKey ->
                        this.managedRequests.containsKey(containingKey) == false && this.waitList.contains(containingKey) == false)
                .forEach(key -> {
                    boolean match = this.allRequests.get(key).stream().anyMatch(request -> request.getRequest().getValue().equals("1"));
                    if (match) {
                        if (this.managedRequests.size() < this.maxRequestsAtOnce) {
                            this.managedRequests.put(key, this.allRequests.get(key));
                            this.addNewTime(WaitWork.WORK, key);
                        } else {
                            if (this.waitList.contains(key) == false && this.managedRequests.containsKey(key) == false) {
                                this.waitList.add(key);
                                this.addNewTime(WaitWork.WAIT, key);
                            }
                        }
                    } else {
                        //Set EnableSignal of Components who have no HeatRequest to 0
                        this.allRequests.get(key).forEach(request->{
                            request.getCallbackRequest().setValue("0");
                        });
                    }
                });
        this.waitList.forEach(this::unsetEnable);
    }

    private void addNewTime(WaitWork waitWork, Integer key) {
        switch (waitWork) {
            case WAIT:
                if (this.timerType.equals(TimerType.TIME)) {
                    this.waitTime.put(key, new DateTime());
                } else {
                    this.waitCycles.put(key, new AtomicInteger(0));
                }
                break;
            case WORK:
                if (this.timerType.equals(TimerType.TIME)) {
                    this.workTime.put(key, new DateTime());
                } else {
                    this.workCycles.put(key, new AtomicInteger(0));
                }
                break;
        }
    }

    /**
     * Checks if a member of the Waitinglist is waiting for longer then the time specified in the config.
     * If applicable the member will call the swapWaitingMember method to swap this member with the longest operating member of the
     * worklist.
     */
    private void swapKeyIfWaitTimeIsUp() {
        ArrayList<Integer> keysToSwap = new ArrayList<>();
        this.waitList.forEach(key -> {
            if (this.checkWaitTime(key)) {
                keysToSwap.add(key);
            }
        });
        keysToSwap.forEach(this::swapWaitingMember);

    }

    private boolean checkWaitTime(Integer key) {
        boolean isTimeUp = false;
        switch (this.timerType) {

            case CYCLES:
                isTimeUp = this.waitCycles.get(key).getAndIncrement() >= this.maxWaitTime;

                break;
            case TIME:
                DateTime now = new DateTime();
                DateTime compare = new DateTime(this.waitTime.get(key));
                compare = compare.plusMinutes(this.maxWaitTime);
                isTimeUp = now.isAfter(compare);
            default:
                break;
        }

        return isTimeUp;
    }

    /**
     * Swaps a specified Member of the Waitinglist with the longest running member in the active working list.
     *
     * @param waitKey Map-key of the waiting Member
     */
    private void swapWaitingMember(int waitKey) {

        this.waitList.remove(this.waitList.indexOf(waitKey));
        int workKey = getMaxWorkTimeMember();
        this.managedRequests.remove(workKey);
        managedRequests.put(waitKey, allRequests.get(waitKey));
        switch (this.timerType) {
            case CYCLES:
                this.workCycles.remove(workKey);
                this.waitCycles.remove(waitKey);

                this.waitList.add(workKey);
                this.waitCycles.put(workKey, new AtomicInteger(0));
                this.workCycles.put(waitKey, new AtomicInteger(0));
                break;

            case TIME:
            default:

                //---------------------------Remove old timestamps---------------------------\\
                workTime.remove(workKey);
                waitTime.remove(waitKey);
                //-------------------Put old workingMember in the waitList-------------------\\
                waitList.add(workKey);
                waitTime.put(workKey, new DateTime());
                //-------------------Put old waitingMember in the workList-------------------\\
                workTime.put(waitKey, new DateTime());
                break;
        }
    }

    /**
     * Get the Member from the ManagedRequest list that is working for the longest time.
     *
     * @return Key of the Longest running member
     */
    private int getMaxWorkTimeMember() {

        AtomicInteger maxWorkTimeKey = new AtomicInteger(0);
        switch (this.timerType) {

            case CYCLES:
                AtomicInteger maxCounter = new AtomicInteger(Integer.MIN_VALUE);
                workCycles.forEach((key,value)->{
                    if(value.get() > maxCounter.get()){
                        maxCounter.set(value.get());
                        maxWorkTimeKey.set(key);
                    }
                });
                return maxWorkTimeKey.get();
            case TIME:
            default:
                AtomicReference<DateTime> maxTime = new AtomicReference<>();

                workTime.forEach((entry, value) -> {
                    if (maxTime.get() != null && value.isBefore(maxTime.get())) {
                        maxTime.set(value);
                        maxWorkTimeKey.set(entry);
                    } else if (maxTime.get() == null) {
                        maxTime.set(value);
                        maxWorkTimeKey.set(entry);
                    }
                });
                return maxWorkTimeKey.intValue();
        }

    }

    /**
     * If more than maxRequestSize Components had Heat Requests then they will be added to wait-list.
     * Therefor the Waitlist is handled first and then the rest of the tasks.
     * After adding to requests, they will be removed from wait-list.
     */
    private void manageWaitList() {
        List<Integer> removeEntriesFromWaitList = new ArrayList<>();
        this.waitList.forEach(entry -> {
            if (this.managedRequests.size() < this.maxRequestsAtOnce) {
                this.managedRequests.put(entry, this.allRequests.get(entry));
                removeEntriesFromWaitList.add(entry);
            }

        });
        this.waitList.removeIf(removeEntriesFromWaitList::contains);
    }

    /**
     * Clears executed Requests by iterating through Managed Requests and check if the all Requests are "0" (false).
     * if not contain them in managedRequests. In Future: TimeManagement -> Prevent Starvation
     */
    private void clearExecutedRequests() {
        List<Integer> keyToRemove = new ArrayList<>();
        if (this.managedRequests.size() > 0) {
            this.managedRequests.keySet().forEach(key -> {
                boolean noRequests = this.managedRequests.get(key).stream().noneMatch(request -> request.getRequest().getValue().equals("1"));
                if (noRequests) {
                    keyToRemove.add(key);
                    //sets callback to false
                    this.unsetEnable(key);
                }
            });
            this.managedRequests.keySet().removeIf(keyToRemove::contains);
        }
    }

    /**
     * returns the managed Request either all of them (e.g. if isForcing in Communicationmaster is set) or
     * returns managed requests (Maximum Requests size determined by Keys of the Map).
     *
     * @return the ManagedRequests.
     */
    @Override
    public Map<Integer, List<RestRequest>> getManagedRequests() {
        if (manageAllAtOnce) {
            return allRequests;
        } else {
            return managedRequests;
        }
    }

    /**
     * Stop the Request by Key and Value.
     *
     * @param key          key of request map
     * @param restRequests requests to stop/Callback to 0.
     */
    @Override
    public void stopRestRequests(Integer key, List<RestRequest> restRequests) {
        restRequests.forEach(request -> {
            if (this.allRequests.get(key).contains(request)) {
                int index = this.allRequests.get(key).indexOf(request);
                this.allRequests.get(key).get(index).getCallbackRequest().setValue("0");
            }
        });
    }

    @Override
    public void setMaxManagedRequests(int requestNo) {
        if (requestNo >= 0) {
            this.maxRequestsAtOnce = requestNo;
        }
    }

    @Override
    public int getMaxRequestsAtOnce() {
        return this.maxRequestsAtOnce;
    }

    @Override
    public void setManageAllAtOnce(boolean manageAllAtOnce) {
        this.manageAllAtOnce = manageAllAtOnce;
    }

    @Override
    public void setManageType(ManageType type) {
        this.manageType = type;
    }

    @Override
    public ManageType getManageType() {
        return this.manageType;
    }

    /**
     * Clears the managed Requests, and set all Callbacks to False.
     */
    @Override
    public void stop() {
        this.managedRequests.clear();
        this.allRequests.forEach((key, value) -> {
            value.forEach(restRequest -> {
                restRequest.getCallbackRequest().setValue("0");
            });
        });
    }

    @Override
    public void setMaxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public void setTimerType(TimerType type){
        this.timerType = type;
    }

    private void unsetEnable(int key) {
        this.allRequests.get(key).forEach(restRequest -> restRequest.getCallbackRequest().setValue("0"));
    }

}
