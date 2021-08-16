package io.openems.edge.controller.hydrauliccomponent.api;

public class HydraulicPosition {
    //key of Position, usually a Temperature
    int temperature;
    //value of Position, usually a ValvePosition
    double hydraulicPosition;

    public HydraulicPosition(int temperature, double hydraulicPosition) {
        this.temperature = temperature;
        this.hydraulicPosition = hydraulicPosition;
    }

    public int getTemperature() {
        return temperature;
    }

    public double getHydraulicPosition() {
        return hydraulicPosition;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public void setHydraulicPosition(int hydraulicPosition) {
        this.hydraulicPosition = hydraulicPosition;
    }
}
