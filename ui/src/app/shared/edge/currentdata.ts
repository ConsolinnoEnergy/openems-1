import { DefaultTypes } from "../service/defaulttypes";
import { Utils } from "../service/utils";

export class CurrentData {

    public readonly summary: DefaultTypes.Summary;

    constructor(
        public readonly channel: { [channelAddress: string]: any } = {}
    ) {
        this.summary = this.getSummary(channel);
    }

    private getSummary(c: { [channelAddress: string]: any }): DefaultTypes.Summary {
        let result: DefaultTypes.Summary = {
            system: {
                totalPower: null,
            }, storage: {
                soc: null,
                chargeActivePower: null, // sum of chargeActivePowerAC and chargeActivePowerDC
                chargeActivePowerAC: null,
                chargeActivePowerDC: null,
                maxChargeActivePower: null,
                dischargeActivePower: null, // equals dischargeActivePowerAC
                dischargeActivePowerAC: null,
                dischargeActivePowerDC: null,
                maxDischargeActivePower: null,
                powerRatio: null,
                maxApparentPower: null,
                effectiveChargePower: null,
                effectiveDischargePower: null,
            }, production: {
                hasDC: false,
                powerRatio: null,
                activePower: null, // sum of activePowerAC and activePowerDC
                activePowerAC: null,
                activePowerDC: null,
                maxActivePower: null
            }, grid: {
                gridMode: null,
                powerRatio: null,
                buyActivePower: null,
                maxBuyActivePower: null,
                sellActivePower: null,
                maxSellActivePower: null
            }, consumption: {
                powerRatio: null,
                activePower: null
            }
        };

        {
            /*
             * Grid
             * > 0 => Buy from grid
             * < 0 => Sell to grid
             */
            const gridActivePower: number = c['_sum/GridActivePower'];
            result.grid.maxBuyActivePower = c['_sum/GridMaxActivePower'];
            if (!result.grid.maxBuyActivePower) {
                result.grid.maxBuyActivePower = 5000;
            }
            result.grid.maxSellActivePower = c['_sum/GridMinActivePower'] * -1;
            if (!result.grid.maxSellActivePower) {
                result.grid.maxSellActivePower = -5000;
            }
            result.grid.gridMode = c['_sum/GridMode'];
            if (gridActivePower > 0) {
                result.grid.sellActivePower = 0;
                result.grid.buyActivePower = gridActivePower;
                result.grid.powerRatio = Utils.orElse(Utils.divideSafely(gridActivePower, result.grid.maxBuyActivePower), 0);
            } else {
                result.grid.sellActivePower = gridActivePower * -1;
                result.grid.buyActivePower = 0;
                result.grid.powerRatio = Utils.orElse(Utils.divideSafely(gridActivePower, result.grid.maxSellActivePower), 0);
            }
        }

        {
            /*
             * Production
             */
            result.production.activePowerAC = c['_sum/ProductionAcActivePower'];
            result.production.activePower = c['_sum/ProductionActivePower'];
            result.production.maxActivePower = c['_sum/ProductionMaxActivePower'];
            if (!result.production.maxActivePower) {
                result.production.maxActivePower = 10000;
            }
            result.production.powerRatio = Utils.orElse(Utils.divideSafely(result.production.activePower, result.production.maxActivePower), 0);
            result.production.activePowerDC = c['_sum/ProductionDcActualPower'];
        }

        {
            /*
             * Storage
             * > 0 => Discharge
             * < 0 => Charge
             */
            result.storage.soc = c['_sum/EssSoc'];
            const essActivePower: number = c['_sum/EssActivePower'];
            result.storage.maxApparentPower = c['_sum/EssMaxApparentPower'];

            if (!result.storage.maxApparentPower) {
                result.storage.maxApparentPower = 5000;
            }
            result.storage.chargeActivePowerDC = c['_sum/ProductionDcActualPower'];
            if (essActivePower == null) {
                // keep 'null'
            } else if (essActivePower > 0) {
                result.storage.chargeActivePowerAC = null;
                result.storage.dischargeActivePowerAC = essActivePower;
                // TODO: should consider DC-Power of ratio
                result.storage.powerRatio = Utils.orElse(Utils.divideSafely(essActivePower, result.storage.maxApparentPower), 0);
            } else {
                result.storage.chargeActivePowerAC = Utils.multiplySafely(essActivePower, -1);
                result.storage.dischargeActivePowerAC = null;
                result.storage.powerRatio = Utils.orElse(Utils.divideSafely(essActivePower, result.storage.maxApparentPower), 0);
            }
            result.storage.chargeActivePower = Utils.addSafely(result.storage.chargeActivePowerAC, result.storage.chargeActivePowerDC);
            result.storage.dischargeActivePower = result.storage.dischargeActivePowerAC;

            let effectivePower;
            if (result.storage.chargeActivePowerAC == null && result.storage.dischargeActivePowerAC == null && result.production.activePowerDC == null) {
                effectivePower = null;
            } else {
                effectivePower = Utils.subtractSafely(
                    Utils.subtractSafely(
                        Utils.orElse(result.storage.dischargeActivePowerAC, 0), result.storage.chargeActivePowerAC
                    ), result.production.activePowerDC);
            }
            if (effectivePower != null) {
                if (effectivePower > 0) {
                    result.storage.effectiveDischargePower = effectivePower;
                } else {
                    result.storage.effectiveChargePower = effectivePower * -1;
                }
            }
        }

        {
            /*
             * Consumption
             */
            result.consumption.activePower = c['_sum/ConsumptionActivePower'];
            let consumptionMaxActivePower = c['_sum/ConsumptionMaxActivePower'];
            if (!consumptionMaxActivePower) {
                consumptionMaxActivePower = 10000;
            }
            result.consumption.powerRatio = Utils.orElse(Utils.divideSafely(result.consumption.activePower, consumptionMaxActivePower), 0);
            if (result.consumption.powerRatio < 0) {
                result.consumption.powerRatio = 0;
            }
        }

        {
            /*
             * Total
             */
            result.system.totalPower = Math.max(
                // Productions
                result.grid.buyActivePower
                + (result.production.activePower > 0 ? result.production.activePower : 0)
                + result.storage.dischargeActivePowerAC,
                + (result.consumption.activePower < 0 ? result.consumption.activePower * -1 : 0),
                // Consumptions
                result.grid.sellActivePower
                + (result.production.activePower < 0 ? result.production.activePower * -1 : 0)
                + result.storage.chargeActivePowerAC,
                + (result.consumption.activePower > 0 ? result.consumption.activePower : 0)
            );
        }
        return result;
    }

}