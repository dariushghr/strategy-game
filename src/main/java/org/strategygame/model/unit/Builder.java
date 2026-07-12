package org.strategygame.model.unit;

public class Builder extends Unit {
    private int charges = 3;
    public Builder() { super(3, 1); }
    public boolean hasCharges() { return charges > 0; }
    public int getCharges()     { return charges; }
    public void useCharge()     { if (--charges <= 0) setAlive(false); }
    @Override public UnitType getType() { return UnitType.BUILDER; }
}
