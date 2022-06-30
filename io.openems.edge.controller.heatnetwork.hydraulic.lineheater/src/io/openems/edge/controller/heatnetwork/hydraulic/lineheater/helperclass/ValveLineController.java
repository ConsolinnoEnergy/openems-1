package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.heatsystem.components.HydraulicComponent;

public class ValveLineController extends AbstractLineController {

    private final HydraulicComponent valve;
    private Double max;
    private Double min;


    public ValveLineController(boolean booleanControlled, HydraulicComponent valve, boolean useMinMax, ComponentManager cpm) {
        super(booleanControlled, useMinMax, cpm);
        this.valve = valve;
    }

    @Override
    public boolean startProcess() throws OpenemsError.OpenemsNamedException {
        this.updateComponentIfNecessary();
        if (super.useMinMax) {
            this.valve.maxValueChannel().setNextWriteValue(this.max);
            this.valve.minValueChannel().setNextWriteValue(this.min);
        }
        if (this.isRunning == false || this.valve.powerLevelReached() == false) {

            this.isRunning = true;
            //Either fullpower set .--> ValveManager handles OR you could change Valve directly
            //this.valveBypass.setPowerLevelPercent().setNextValue((FULL_POWER));
            if (super.isBooleanControlled()) {
                this.valve.setPointPowerLevelChannel().setNextWriteValueFromObject(true);
            } else {
                this.valve.setPointPowerLevelChannel().setNextWriteValueFromObject(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
            }

            return true;
        }

        return false;
    }

    private void updateComponentIfNecessary() {

    }

    @Override
    public boolean stopProcess() throws OpenemsError.OpenemsNamedException {
        this.updateComponentIfNecessary();
        if (this.isRunning) {
            this.isRunning = false;
            this.valve.reset();
            return true;
        }
        return false;
    }

    @Override
    public void setMaxAndMin(Double max, Double min) {
        this.max = max;
        this.min = min;
    }

    @Override
    public void onlySetMaxMin() {
        try {
            this.valve.maxValueChannel().setNextWriteValue(this.max);
            this.valve.minValueChannel().setNextWriteValue(this.min);
        } catch (OpenemsError.OpenemsNamedException ignored) {

        }
    }
}
