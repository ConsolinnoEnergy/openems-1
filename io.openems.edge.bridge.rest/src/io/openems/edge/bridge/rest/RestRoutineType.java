package io.openems.edge.bridge.rest;

/**
 * Helps to determine if a {@link io.openems.edge.bridge.rest.device.task.RestRemoteReadTask} or
 * {@link io.openems.edge.bridge.rest.device.task.RestRemoteWriteTask} should be configured.
 */
public enum RestRoutineType {
    READ, WRITE
}
