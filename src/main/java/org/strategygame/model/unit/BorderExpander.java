package org.strategygame.model.unit;

public class BorderExpander extends Unit {
    public BorderExpander() { super(3, 1); }
    public void consume()   { setAlive(false); }
    @Override public UnitType getType() { return UnitType.BORDER_EXPANDER; }
}
