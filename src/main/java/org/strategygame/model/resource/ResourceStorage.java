package org.strategygame.model.resource;

import java.util.EnumMap;
import java.util.Map;

public class ResourceStorage {

    private static final int BASE_CAPACITY = 200;
    private static final int UPGRADE_BONUS  = 150;

    private final Map<ResourceType, Integer> current  = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> capacity = new EnumMap<>(ResourceType.class);

    public ResourceStorage() {
        for (ResourceType t : ResourceType.values()) {
            current.put(t, 0);
            capacity.put(t, BASE_CAPACITY);
        }

        current.put(ResourceType.FOOD,  30);
        current.put(ResourceType.WOOD,  20);
        current.put(ResourceType.STONE, 10);
        current.put(ResourceType.IRON,   0);
    }

    public int get(ResourceType t) {
        Integer v = current.get(t);
        return v == null ? 0 : v;
    }

    public int getCapacity(ResourceType t) {
        Integer v = capacity.get(t);
        return v == null ? 0 : v;
    }

    public void add(ResourceType t, int amount) {
        int next = Math.min(get(t) + amount, getCapacity(t));
        current.put(t, next);
    }

    public boolean canAfford(ResourceType t, int amount) {
        return get(t) >= amount;
    }

    public boolean canAffordAll(int[] foodWoodStoneIron) {
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length; i++) {
            if (get(types[i]) < foodWoodStoneIron[i]) return false;
        }
        return true;
    }

    public void deduct(ResourceType t, int amount) {
        current.put(t, Math.max(0, get(t) - amount));
    }

    public void deductAll(int[] foodWoodStoneIron) {
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length; i++) {
            deduct(types[i], foodWoodStoneIron[i]);
        }
    }

    public void upgradeCapacity() {
        for (ResourceType t : ResourceType.values()) {
            capacity.put(t, capacity.get(t) + UPGRADE_BONUS);
        }
    }
}
