package io.openems.edge.bridge.mqtt.api;

public interface MqttTask {
    /**
     * Get the Mqtt: Quality of Service of this Task.
     *
     * @return the QoS
     */
    int getQos();

    /**
     * Get the Topic of the Task.
     *
     * @return the Topic
     */
    String getTopic();

    /**
     * Get the Payload of the Task.
     *
     * @return the payload
     */
    String getPayload();

    /**
     * Check if this task has the RetainFlag set.
     *
     * @return the boolean.
     */
    boolean getRetainFlag();

    /**
     * Check if a Time should be added to the payload (usually only for publish tasks).
     *
     * @return the boolean.
     */
    boolean getAddTime();

    /**
     * Returns the Priority of this task (Low, High, Urgent).
     *
     * @return the Priority of this task.
     */
    MqttPriority getPriority();

    /**
     * Get the MqttType of this task.
     *
     * @return the MqttType.
     */

    MqttType getMqttType();

    /**
     * Called by Abstract Cycle Worker for current Tasks to handle.
     *
     * @param currentTime the currentTime, calculated each cycle by abstractManager
     * @return aboolean.
     */
    boolean isReady(long currentTime);
}
