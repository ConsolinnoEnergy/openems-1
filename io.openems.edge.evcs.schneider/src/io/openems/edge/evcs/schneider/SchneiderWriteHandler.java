package io.openems.edge.evcs.schneider;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Reference;

/**
 * This WriteHandler writes the Values from the Internal Channels that where retrieved over Modbus into the correct OpenEms Channels.
 * External READ_ONLY Register -> Internal OpenEms
 */
public class SchneiderWriteHandler {

    private final SchneiderImpl parent;
    private CPWState state;
    private int energySession;

    @Reference
    protected ConfigurationAdmin cm;

    public SchneiderWriteHandler(SchneiderImpl parent) {
        this.parent = parent;
    }

    void run() {

        //--------------EVCS--------------\\
        this.setPhaseCount();
        this.setEnergySession();
        this.detectStatus();
        //The Schneider EVCS reports in kW but OpenEms needs W so StationPower * 1000
        this.parent._setChargePower((int) (this.parent.getStationPowerTotal() * 1000));
        this.parent._setChargingType(ChargingType.AC);
        this.parent._setMaximumHardwarePower(16 * 230);
        this.parent._setMinimumPower(this.parent.getMinPower() * 230);
        this.parent._setMaximumPower(this.parent.getMaxPower() * 230);

        //----------Managed Evcs-----------\\
        //The Schneider EVCS takes A and the Power Precision Channel needs W so 1 A * 230 V
        this.parent._setPowerPrecision(1 * 230);

    }

    /**
     * Sets all the Channels regarding Energy.
     */
    private void setEnergySession() {
        int currentEnergy = this.parent.getStationEnergyLSB() + this.parent.getStationEnergyMSB();
        this.parent._setActiveConsumptionEnergy(currentEnergy);
        this.energySession += currentEnergy;
        this.parent._setEnergySession(this.energySession);

    }

    /**
     * Reads the Status from the ModbusRegister and interprets it into the CPWState and the OpenEms EVCS Status.
     */
    private void detectStatus() {
        int status = this.parent.getCPWState();
        switch (status) {
            case 0:
                this.state = CPWState.EVSE_NOT_AVAILABLE;
                this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
                break;
            case 1:
                this.state = CPWState.EVSE_AVAILABLE;
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 2:
                this.state = CPWState.PLUG_DETECTED;
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 4:
                this.state = CPWState.EV_CONNECTED;
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 5:
                this.state = CPWState.EV_CONNECTED2;
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 6:
                this.state = CPWState.EV_CONNECTED_VENTILATION_REQUIRED;
                this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
                break;
            case 7:
                this.state = CPWState.EVSE_READY;
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 8:
                this.state = CPWState.EV_READY;
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 9:
                this.state = CPWState.CHARGING;
                this.parent._setStatus(Status.CHARGING);
                break;
            case 10:
                this.state = CPWState.EV_READY_VENTILATION_REQUIRED;
                this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
                break;
            case 11:
                this.state = CPWState.CHARGING_VENTILATION_REQUIRED;
                this.parent._setStatus(Status.CHARGING);
                break;
            case 12:
                this.state = CPWState.STOP_CHARGING;
                this.parent._setStatus(Status.CHARGING_FINISHED);
                break;
            case 13:
                this.state = CPWState.ALARM;
                this.parent._setStatus(Status.ERROR);
                break;
            case 14:
                this.state = CPWState.SHORTCUT;
                this.parent._setStatus(Status.ERROR);
                break;
            case 15:
                this.state = CPWState.DIGITAL_COM_BY_EVSE_STATE;
                this.parent._setStatus(Status.UNDEFINED);
                break;
            default:
                this.parent._setStatus(Status.UNDEFINED);
                break;
        }
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getStationIntensityPhaseX() >= 1) {
            phases += 1;
        }
        if (this.parent.getStationIntensityPhase2() >= 1) {
            phases += 1;
        }
        if (this.parent.getStationIntensityPhase3() >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);
        if (phases == 3) {
            this.parent._setMinimumHardwarePower(14 * 230);
        }
    }

    /**
     * Return the CPWState of the EVCS.
     * See CPWState Enum for more information.
     *
     * @return CPWState
     */
    public CPWState getState() {
        return this.state;
    }
}
