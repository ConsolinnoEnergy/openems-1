package io.openems.edge.pump.grundfos.api;


public enum PumpType {

    /**
     * Pump types. containing their headclass and their id nr for the pump.
     * Below is listet the Magna3 with some ids and headclasses. Only supporting current pump channels.
     * */
    MAGNA_3(23, 2, 28, 2, 30, 2, 34, 2, 37,
            2, 39, 2, 58, 2, 32, 2, 112,
            2, 154, 2, 156, 2, 158,
            2, 159, 2, 160, 2, 161,
            2, 162, 2, 105, 4, 106,
            4, 101, 4, 103, 4, 104,
            4, 7, 3, 6, 3, 5, 3,
            25, 3, 26, 3, 22,
            3, 24, 3, 83,
            4, 84, 4, 76, 2,
            77, 2, 1, 5, 52, 3);

    //////read Measured Data////////
    //diff pressure head
    private final int hDiff;
    private final int hDiffHeadClass;
    //temperature Electronics
    private final int tE;
    private final int tEheadClass;
    //current motor
    private final int iMo;
    private final int imoHeadClass;
    //powerConsumption
    private final int plo;
    private final int ploHeadClass;
    //pressure
    private final int h;
    private final int hHeadClass;
    //pump flow
    private final int q;
    private final int qHeadClass;
    //pumped Water medium temperature
    private final int tW;
    private final int tWHeadClass;
    //motor frequency
    private final int fAct;
    private final int fActHeadClass;
    //control Mode
    private final int controlMode;
    private final int controlModeHeadClass;
    //alarm code
    private final int alarmCodePump;
    private final int alarmCodePumpHeadClass;
    //warnCode
    private final int warnCode;
    private final int warnCodeHeadClass;
    //alarmCode
    private final int alarmCode;
    private final int alarmCodeHeadClass;
    //warnBits

    private final int warnBits1;
    private final int warnBits1HeadClass;
    private final int warnBits2;
    private final int warnBits2HeadClass;
    private final int warnBits3;
    private final int warnBits3HeadClass;
    private final int warnBits4;
    private final int warnBits4HeadClass;


    //reference setting range
    private final int rMin;
    private final int rMinHeadClass;
    private final int rMax;
    private final int rMaxHeadClass;
    ///////Write/////////////
    //Pump flow Config Params
    private final int qMaxHi;
    private final int qMaxHiHeadClass;
    private final int qMaxLo;
    private final int qMaxLowClass;

    //pressure Config Params
    private final int deltaH;
    private final int deltaHheadClass;
    private final int hMaxHi;
    private final int hMaxHiHeadClass;
    private final int hMaxLo;
    private final int hMaxLoHeadClass;


    //Commands
    private final int remote;
    private final int remoteHeadClass;
    private final int start;
    private final int startHeadClass;
    private final int stop;
    private final int stopHeadClass;
    private final int minMotorCurve;
    private final int minMotorCurveHeadClass;
    private final int maxMotorCurve;
    private final int maxMotorCurveHeadClass;
    private final int constFrequency;
    private final int constFrequencyHeadClass;
    private final int constPressure;
    private final int constPressureHeadClass;
    private final int hConstRefMin;
    private final int hConstRefMinHeadClass;
    private final int hConstRefMax;
    private final int hConstRefMaxHeadClass;
    private final int autoAdapt;
    private final int autoAdaptHeadClass;


    //Reference Value
    private final int refRem;
    private final int refRemHeadClass;


    PumpType(int hDiff, int hDiffHeadClass, int tE, int tEheadClass, int iMo, int imoHeadClass, int plo,
             int ploHeadClass, int h, int hHeadClass, int q, int qHeadClass, int tW, int tWHeadClass, int fAct,
             int fActHeadClass, int controlMode, int controlModeHeadClass, int alarmCodePump,
             int alarmCodePumpHeadClass, int warnCode, int warnCodeHeadClass, int alarmCode, int alarmCodeHeadClass,
             int warnBits1, int warnBits1HeadClass, int warnBits2, int warnBits2HeadClass, int warnBits3,
             int warnBits3HeadClass, int warnBits4, int warnBits4HeadClass, int qMaxHi, int qMaxHiHeadClass,
             int qMaxLo, int qMaxLowClass, int deltaH, int deltaHheadClass, int hMaxHi, int hMaxHiHeadClass,
             int hMaxLo, int hMaxLoHeadClass, int remote, int remoteHeadClass, int start, int startHeadClass,
             int stop, int stopHeadClass, int minMotorCurve, int minMotorCurveHeadClass,
             int maxMotorCurve, int maxMotorCurveHeadClass, int constFrequency, int constFrequencyHeadClass,
             int constPressure, int constPressureHeadClass, int hConstRefMin, int hConstRefMinHeadClass,
             int hConstRefMax, int hConstRefMaxHeadClass, int rMin, int rMinHeadClass, int rMax, int rMaxHeadClass,
             int refRem, int refRemHeadClass, int autoAdapt, int autoAdaptHeadClass) {
        this.hDiff = hDiff;
        this.hDiffHeadClass = hDiffHeadClass;
        this.tE = tE;
        this.tEheadClass = tEheadClass;
        this.iMo = iMo;
        this.imoHeadClass = imoHeadClass;
        this.plo = plo;
        this.ploHeadClass = ploHeadClass;
        this.h = h;
        this.hHeadClass = hHeadClass;
        this.q = q;
        this.qHeadClass = qHeadClass;
        this.tW = tW;
        this.tWHeadClass = tWHeadClass;
        this.fAct = fAct;
        this.fActHeadClass = fActHeadClass;
        this.controlMode = controlMode;
        this.controlModeHeadClass = controlModeHeadClass;
        this.alarmCodePump = alarmCodePump;
        this.alarmCodePumpHeadClass = alarmCodePumpHeadClass;
        this.warnCode = warnCode;
        this.warnCodeHeadClass = warnCodeHeadClass;
        this.alarmCode = alarmCode;
        this.alarmCodeHeadClass = alarmCodeHeadClass;
        this.warnBits1 = warnBits1;
        this.warnBits1HeadClass = warnBits1HeadClass;
        this.warnBits2 = warnBits2;
        this.warnBits2HeadClass = warnBits2HeadClass;
        this.warnBits3 = warnBits3;
        this.warnBits3HeadClass = warnBits3HeadClass;
        this.warnBits4 = warnBits4;
        this.warnBits4HeadClass = warnBits4HeadClass;
        this.qMaxHi = qMaxHi;
        this.qMaxHiHeadClass = qMaxHiHeadClass;
        this.qMaxLo = qMaxLo;
        this.qMaxLowClass = qMaxLowClass;
        this.deltaH = deltaH;
        this.deltaHheadClass = deltaHheadClass;
        this.hMaxHi = hMaxHi;
        this.hMaxHiHeadClass = hMaxHiHeadClass;
        this.hMaxLo = hMaxLo;
        this.hMaxLoHeadClass = hMaxLoHeadClass;
        this.remote = remote;
        this.remoteHeadClass = remoteHeadClass;
        this.start = start;
        this.startHeadClass = startHeadClass;
        this.stop = stop;
        this.stopHeadClass = stopHeadClass;
        this.minMotorCurve = minMotorCurve;
        this.minMotorCurveHeadClass = minMotorCurveHeadClass;
        this.maxMotorCurve = maxMotorCurve;
        this.maxMotorCurveHeadClass = maxMotorCurveHeadClass;
        this.constFrequency = constFrequency;
        this.constFrequencyHeadClass = constFrequencyHeadClass;
        this.constPressure = constPressure;
        this.constPressureHeadClass = constPressureHeadClass;
        this.hConstRefMin = hConstRefMin;
        this.hConstRefMinHeadClass = hConstRefMinHeadClass;
        this.hConstRefMax = hConstRefMax;
        this.hConstRefMaxHeadClass = hConstRefMaxHeadClass;
        this.rMin = rMin;
        this.rMinHeadClass = rMinHeadClass;
        this.rMax = rMax;
        this.rMaxHeadClass = rMaxHeadClass;
        this.refRem = refRem;
        this.refRemHeadClass = refRemHeadClass;
        this.autoAdapt = autoAdapt;
        this.autoAdaptHeadClass = autoAdaptHeadClass;
    }

    public int gethDiff() {
        return this.hDiff;
    }

    public int gethDiffHeadClass() {
        return this.hDiffHeadClass;
    }

    public int gettE() {
        return this.tE;
    }

    public int gettEheadClass() {
        return this.tEheadClass;
    }

    public int getiMo() {
        return this.iMo;
    }

    public int getImoHeadClass() {
        return this.imoHeadClass;
    }

    public int getPlo() {
        return this.plo;
    }

    public int getPloHeadClass() {
        return this.ploHeadClass;
    }

    public int getH() {
        return this.h;
    }

    public int gethHeadClass() {
        return this.hHeadClass;
    }

    public int getQ() {
        return this.q;
    }

    public int getqHeadClass() {
        return this.qHeadClass;
    }

    public int gettW() {
        return this.tW;
    }

    public int gettWHeadClass() {
        return this.tWHeadClass;
    }

    public int getfAct() { return this.fAct; }

    public int getfActHeadClass() { return this.fActHeadClass; }

    public int getControlMode() {
        return this.controlMode;
    }

    public int getControlModeHeadClass() {
        return this.controlModeHeadClass;
    }

    public int getAlarmCodePump() {
        return this.alarmCodePump;
    }

    public int getAlarmCodePumpHeadClass() {
        return this.alarmCodePumpHeadClass;
    }

    public int getWarnCode() {
        return this.warnCode;
    }

    public int getWarnCodeHeadClass() {
        return this.warnCodeHeadClass;
    }

    public int getAlarmCode() {
        return this.alarmCode;
    }

    public int getAlarmCodeHeadClass() {
        return this.alarmCodeHeadClass;
    }

    public int getWarnBits1() {
        return this.warnBits1;
    }

    public int getWarnBits1HeadClass() {
        return this.warnBits1HeadClass;
    }

    public int getWarnBits2() {
        return this.warnBits2;
    }

    public int getWarnBits2HeadClass() {
        return this.warnBits2HeadClass;
    }

    public int getWarnBits3() {
        return this.warnBits3;
    }

    public int getWarnBits3HeadClass() {
        return this.warnBits3HeadClass;
    }

    public int getWarnBits4() {
        return this.warnBits4;
    }

    public int getWarnBits4HeadClass() {
        return this.warnBits4HeadClass;
    }


    public int getrMin() {
        return this.rMin;
    }

    public int getrMinHeadClass() {
        return this.rMinHeadClass;
    }

    public int getrMax() {
        return this.rMax;
    }

    public int getrMaxHeadClass() {
        return this.rMaxHeadClass;
    }

    public int getqMaxHi() {
        return this.qMaxHi;
    }

    public int getqMaxHiHeadClass() {
        return this.qMaxHiHeadClass;
    }

    public int getqMaxLo() {
        return this.qMaxLo;
    }

    public int getqMaxLowClass() {
        return this.qMaxLowClass;
    }


    public int getDeltaH() {
        return this.deltaH;
    }

    public int getDeltaHheadClass() {
        return this.deltaHheadClass;
    }

    public int gethMaxHi() {
        return this.hMaxHi;
    }

    public int gethMaxHiHeadClass() {
        return this.hMaxHiHeadClass;
    }

    public int gethMaxLo() {
        return this.hMaxLo;
    }

    public int gethMaxLoHeadClass() {
        return this.hMaxLoHeadClass;
    }


    //commands

    public int getRemote() {
        return this.remote;
    }

    public int getRemoteHeadClass() {
        return this.remoteHeadClass;
    }

    public int getStart() {
        return this.start;
    }

    public int getStartHeadClass() {
        return this.startHeadClass;
    }

    public int getStop() {
        return this.stop;
    }

    public int getStopHeadClass() {
        return this.stopHeadClass;
    }

    public int getMinMotorCurve() {
        return this.minMotorCurve;
    }

    public int getMinMotorCurveHeadClass() {
        return this.minMotorCurveHeadClass;
    }

    public int getMaxMotorCurve() {
        return this.maxMotorCurve;
    }

    public int getMaxMotorCurveHeadClass() {
        return this.maxMotorCurveHeadClass;
    }

    public int getConstFrequency() {
        return this.constFrequency;
    }

    public int getConstFrequencyHeadClass() {
        return this.constFrequencyHeadClass;
    }

    public int getConstPressure() {
        return this.constPressure;
    }

    public int getConstPressureHeadClass() {
        return this.constPressureHeadClass;
    }

    public int getAutoAdapt() {
        return this.autoAdapt;
    }

    public int getAutoAdaptHeadClass() {
        return this.autoAdaptHeadClass;
    }

    public int gethConstRefMin() {
        return this.hConstRefMin;
    }

    public int gethConstRefMinHeadClass() {
        return this.hConstRefMinHeadClass;
    }

    public int gethConstRefMax() {
        return this.hConstRefMax;
    }

    public int gethConstRefMaxHeadClass() {
        return this.hConstRefMaxHeadClass;
    }


    public int getRefRem() {
        return this.refRem;
    }

    public int getRefRemHeadClass() {
        return this.refRemHeadClass;
    }
}
