package org.strategygame.model.building;

public class Settlement extends Building {
    public static final int CAP_BONUS = 5;

    public Settlement() { super(BuildingType.SETTLEMENT); }

    @Override public int produce(double m) { return 0; }
}
