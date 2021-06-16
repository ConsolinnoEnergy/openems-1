package io.openems.edge.heater.api;

public interface EnableSignalHandler {

    boolean enableSignalActive(Heater heaterChannel);

    void resetTimer();

    void increaseCycleCounter();
}
