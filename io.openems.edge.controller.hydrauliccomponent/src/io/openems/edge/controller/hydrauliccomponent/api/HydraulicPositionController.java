package io.openems.edge.controller.hydrauliccomponent.api;

public interface HydraulicPositionController extends HydraulicController {
    double getPositionByTemperature(int temperature);

    int getTemperatureByPosition(double position);

    void addPositionByTemperatureAndPosition(int temperature, int position);

}
