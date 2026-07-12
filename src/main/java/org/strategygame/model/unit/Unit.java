package org.strategygame.model.unit;

import org.strategygame.model.map.HexCell;
import java.util.UUID;

public abstract class Unit {
    private final String id;
    private HexCell position;
    private int currentAP;
    private final int maxAP;
    private final int visionRadius;
    private boolean alive = true;

    protected Unit(int maxAP, int visionRadius) {
        this.id           = UUID.randomUUID().toString();
        this.maxAP        = maxAP;
        this.visionRadius = visionRadius;
        this.currentAP    = maxAP;
    }

    public String getId()          { return id; }
    public HexCell getPosition()   { return position; }
    public int getCurrentAP()      { return currentAP; }
    public int getMaxAP()          { return maxAP; }
    public int getVisionRadius()   { return visionRadius; }
    public boolean isAlive()       { return alive; }

    public void setPosition(HexCell c) { this.position = c; }
    public void setAlive(boolean v)    { this.alive = v; }

    public boolean hasAP(int n) { return currentAP >= n; }
    public void spendAP(int n)  { currentAP = Math.max(0, currentAP - n); }
    public void refreshAP()     { currentAP = maxAP; }

    public abstract UnitType getType();
}
