package io.openems.edge.exceptionalstate.api;

public interface ExceptionalStateHandler {

    boolean exceptionalStateActive(ExceptionalState exceptionalStateChannel);
}
