package org.strategygame.model.resource;

import java.util.EnumMap;
import java.util.Map;

public class ResourceRate {
    private final Map<ResourceType, Integer> production = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> upkeep     = new EnumMap<>(ResourceType.class);

    public void addProduction(ResourceType t, int v) {
        Integer cur = production.get(t);
        production.put(t, (cur == null ? 0 : cur) + v);
    }

    public void addUpkeep(ResourceType t, int v) {
        Integer cur = upkeep.get(t);
        upkeep.put(t, (cur == null ? 0 : cur) + v);
    }

    public int getNet(ResourceType t) { return getProduction(t) - getUpkeep(t); }

    public int getProduction(ResourceType t) {
        Integer v = production.get(t); return v == null ? 0 : v;
    }

    public int getUpkeep(ResourceType t) {
        Integer v = upkeep.get(t); return v == null ? 0 : v;
    }
}
