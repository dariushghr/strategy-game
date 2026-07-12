package org.strategygame.model.building;

import org.strategygame.model.resource.ResourceType;

public class Settlement extends Building {
    public static final int CAP_BONUS = 5;
    public Settlement() { super(0); }
    @Override public int produce(double m)          { return 0; }
    @Override public ResourceType getResourceType()  { return null; }
    @Override public BuildingType getType()          { return BuildingType.SETTLEMENT; }
}
