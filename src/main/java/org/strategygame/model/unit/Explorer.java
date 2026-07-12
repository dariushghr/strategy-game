package org.strategygame.model.unit;

public class Explorer extends Unit {
    private boolean autoExplore = false;
    public Explorer() { super(6, 3); }
    public boolean isAutoExplore()      { return autoExplore; }
    public void setAutoExplore(boolean v){ this.autoExplore = v; }
    @Override public UnitType getType() { return UnitType.EXPLORER; }
}
