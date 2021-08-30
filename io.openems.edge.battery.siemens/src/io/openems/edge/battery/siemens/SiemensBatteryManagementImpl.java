package io.openems.edge.battery.siemens;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

/**
 * This represents the Controller for the Siemens JuneLight Battery.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.battery.siemens", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)


public class SiemensBatteryManagementImpl extends AbstractOpenemsComponent implements OpenemsComponent, ManagedSymmetricEss, SymmetricEss, EventHandler {
    public SiemensBatteryManagementImpl() {
        super(OpenemsComponent.ChannelId.values(), ManagedSymmetricEss.ChannelId.values(), SymmetricEss.ChannelId.values());
    }

    private CactusRestBridge cactusRestBridge;
    private float capacity;
    private final Logger log = LoggerFactory.getLogger(SiemensBatteryManagementImpl.class);
    private int cycles = 0;
    private static final int MAXCYLCES = 150;

    @Activate
    void activate(ComponentContext context, Config config) throws MalformedURLException {
        this.cactusRestBridge = new CactusRestBridge(config.tenant_ID(), config.token(), config.project_ID(), config.product_ID(), config.serial());
        this.capacity = config.capacity();
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.cactusRestBridge.checkConnection()) {
            try {
                String lgBattery = this.cactusRestBridge.makeReadRequest("LG_BATTERY_APP");
                String powerInverter = this.cactusRestBridge.makeReadRequest("POWER_INVERTER_READ_APP");
                String modules = (this.responseSolver(lgBattery, "Number_of_Modules"));
                float cosPhi = (Float.parseFloat(this.responseSolver(powerInverter, "Inverter_Read_CosPhi")) / 1000);
                float activePower = Float.parseFloat(this.responseSolver(powerInverter, "Inverter_Bat_Power"));
                float apparentPower = 0;
                if (cosPhi != 0) {
                    apparentPower = activePower / cosPhi;
                }
                double reactivePower = Math.sqrt((apparentPower * apparentPower) - (activePower * activePower));

                getSocChannel().setNextValue(Integer.parseInt(this.responseSolver(lgBattery, "LG_Battery_SOC")));
                getCapacityChannel().setNextValue((Integer.parseInt(modules) * this.capacity));
                getGridModeChannel().setNextValue(GridMode.ON_GRID);
                getActivePowerChannel().setNextValue(Math.round(activePower));
                getReactivePowerChannel().setNextValue((float) reactivePower);
                getActiveDischargeEnergyChannel().setNextValue(Integer.parseInt(this.responseSolver(powerInverter, "Inverter_Pac_max_Read")));
                getMaxApparentPowerChannel().setNextValue(Math.round(apparentPower));
                getActiveChargeEnergyChannel().setNextValue(Integer.parseInt(this.responseSolver(powerInverter, "Inverter_Pac_min_Read")));
                getMinCellVoltageChannel().setNextValue(Integer.parseInt(this.responseSolver(powerInverter, "Battery_Voltage")));


            } catch (IOException ignored) {
                this.log.error("A Read request seemed to Failed. Connection Closed or invalid input parameters.");
            }

            if (this.cycles >= MAXCYLCES) {
              this.fallback();
            }
            this.cycles++;
        }
    }

    /**
     * Filters the result for the Channels out of the response String from the Cloud.
     *
     * @param response the full response from the cloud
     * @param filter   the Value you want to get
     * @return the Value as a String or -1 if something went wrong
     */
    private String responseSolver(String response, String filter) {
        if (response.contains("success") == false && response.contains(filter)) {
            String[] parts = response.split("\"" + filter + "\":");

            String[] message = parts[1].split(",");
            if (message[0].contains("}")) {
                String[] end = message[0].split("}");
                return end[0];
            }
            return message[0].replace("\"", "");
        } else if (response.equals("success")) {
            return response;
        }

        return "-1";
    }

    /**
     * Tells the Battery to charge
     *
     * @param power The Power the battery has to charge with
     * @return "Success" if it worked, "Failure" Otherwise
     */
    private String charge(int power) {
        EventParameter opMode = new EventParameter("App_Select_Operation_Mode", 2);
        EventParameter powerValue = new EventParameter("App_Setpoint_Mode_Power_Value", power);
        String response = "Failure";
        try {
            response = this.cactusRestBridge.makeWriteRequest("POWER3_PARAMETER_MANAGER_APP",
                    this.cactusRestBridge.createParameterString(Arrays.asList(opMode, powerValue)));
        } catch (Exception e) {
            this.log.error("A Write request seemed to Failed. Connection Closed or invalid input parameters.");
        }
        this.cycles = 0;
        return response;
    }

    /**
     * Tells the Battery to discharge.
     *
     * @return "Success" if it worked, "Failure" Otherwise
     */
    private String fallback() {
        EventParameter opMode = new EventParameter("App_Select_Operation_Mode", 1);
        EventParameter powerValue = new EventParameter("App_Setpoint_Mode_Power_Value", 0);
        String response = "Failure";
        try {
            response = this.cactusRestBridge.makeWriteRequest("POWER3_PARAMETER_MANAGER_APP",
                    this.cactusRestBridge.createParameterString(Arrays.asList(opMode, powerValue)));
        } catch (Exception e) {
            this.log.error("A Write request seemed to Failed. Connection Closed or invalid input parameters.");
        }

        return response;
    }

    @Override
    public Power getPower() {
        return null;
    }

    @Override
    public void applyPower(int activePower, int reactivePower) {
        //this.charge(activePower);
    }

    @Override
    public int getPowerPrecision() {
        return 0;
    }
}
