package org.strategygame.model.unit;

import org.strategygame.model.building.Building;

public class Worker extends Unit {
    private Building stationedAt = null;

    public Worker() { super(3, 1); }

    public boolean isStationed()     { return stationedAt != null; }
    public Building getStationedAt() { return stationedAt; }

    public void station(Building b, int apCost) {
        this.stationedAt = b;
        spendAP(apCost);
        b.addWorker(this);
    }

    public void unstation() {
        if (stationedAt != null) {
            stationedAt.removeWorker(this);
            stationedAt = null;

        }
    }

    @Override public UnitType getType() { return UnitType.WORKER; }
}
