package io.openems.edge.heater.chp.viessmann;

public enum ViessmannChpType {
    /**
     * Datasheet information. If -1 is given there was no data available.
     * */
    Vito_EM_6_15(6, 14.9f, 22.2f, -1, -1, 7, 14.9f, 4.5f, 3.5f, "Vitobloc_200_EM_6_15"),
    Vito_EM_9_20(8.5f, 20.1f, 30.1f, -1, -1, 7, 20.1f, 16.1f, 12.3f, "Vitobloc_200_EM_9_20"),
    Vito_EM_20_39(20, 39, 62, -1, -1, 7, 39, 27.5f, 22.3f, "Vitobloc_200_EM_20_39"),
    Vito_EM_20_39_RL_70(20, 39, 62, -1, -1, 7, 35.7f, 30.6f, 23.1f, "Vitobloc_200_EM_20_39_70"),
    Vito_EM_50_81(50, 83, 145, 93, 75, 7, 83, 64, 46, "Vitobloc_200_EM_50_81"),
    Vito_EM_70_115(70, 117, 204, 92, 75, 7, 117, 85, 66, "Vitobloc_200_EM_70_115"),
    Vito_EM_100_167(99, 173, 280, -1, -1, 7, 167, 135, 105, "Vitobloc_200_EM_100_167"),
    Vito_EM_140_207(140, 207, 384, 94, 75, 7, 209, 171, 130, "Vitobloc_200_EM_140_207"),
    Vito_EM_199_263(190, 278, 516, 90, 70, 7, 278, 235, 180, "Vitobloc_200_EM_199_263"),
    Vito_EM_199_293(199, 293, 553, -1, -1, 7, 278, 235, 180, "Vitobloc_200_EM_199_293"),
    Vito_EM_238_363(238, 363, 667, 90, 75, -1, -1, -1, -1, "Vitobloc_200_EM_238_363"),
    Vito_EM_363_498(363, 498, 960, -1, -1, 7, 499, 404, 302, "Vitobloc_200_EM_363_498"),
    Vito_EM_401_549(401, 552, 1053, 90, 70, 7, 552, 423, 316, "Vitobloc_200_EM_401_549"),
    Vito_EM_530_660(530, 660, 1342, -1, -1, 7, 660, 590, 463, "Vitobloc_200_EM_530_660"),
    Vito_BM_36_66(36, 66, 122, -1, -1, -1, -1, -1, -1, "Vitobloc_200_BM_36_66"),
    Vito_BM_55_88(55, 88, 165, -1, -1, -1, -1, -1, -1, "Vitobloc_200_BM_55_88"),
    Vito_BM_190_238(190, 238, 493, -1, -1, -1, -1, -1, -1, "Vitobloc_200_BM_190_238"),
    Vito_BM_366_437(366, 437, 950, -1, -1, -1, -1, -1, -1, "Vitobloc_200_BM_366_437");


    //tolerance in Percent
    private final int heatOutputTolerance;
    //values in kW
    private final float electricOutput;
    private final float thermalOutput;
    private final float fuelUse;
    //in kW and only high temperature natural gas
    private final float heatOutputAt100Percent;
    private final float heatOutputAt75Percent;
    private final float heatOutputAt50Percent;

    //values in °C
    private final int maxFlowTemperature;
    private final int maxReturnTemperature;

    private final String name;

    /*For Later Usage at some point in dev.*/
    ViessmannChpType(float electricOutput, float thermalOutput, float fuelUse, int maxFlowTemperature, int maxReturnTemperature, int heatOutputTolerance, float heatOutputAt100Percent, float heatOutputAt75Percent, float heatOutputAt50Percent, String name) {
        this.electricOutput = electricOutput;
        this.thermalOutput = thermalOutput;
        this.fuelUse = fuelUse;
        this.maxFlowTemperature = maxFlowTemperature;
        this.maxReturnTemperature = maxReturnTemperature;
        this.heatOutputTolerance = heatOutputTolerance;
        this.heatOutputAt50Percent = heatOutputAt50Percent;
        this.heatOutputAt75Percent = heatOutputAt75Percent;
        this.heatOutputAt100Percent = heatOutputAt100Percent;
        this.name = name;

    }

    public float getElectricOutput() {
        return this.electricOutput;
    }

    public float getThermalOutput() {
        return this.thermalOutput;
    }

    public float getFuelUse() {
        return this.fuelUse;
    }

    public int getHeatOutputTolerance() {
        return this.heatOutputTolerance;
    }

    public float getHeatOutputAt100Percent() {
        return this.heatOutputAt100Percent;
    }

    public float getHeatOutputAt75Percent() {
        return this.heatOutputAt75Percent;
    }

    public float getHeatOutputAt50Percent() {
        return this.heatOutputAt50Percent;
    }

    public int getMaxFlowTemperature() {
        return this.maxFlowTemperature;
    }

    public int getMaxReturnTemperature() {
        return this.maxReturnTemperature;
    }

    public String getName() {
        return this.name;
    }
}
