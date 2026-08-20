package org.strategygame.model.resource;

import org.strategygame.config.GameConfig;

import java.util.EnumMap;
import java.util.Map;

/**
 * انبار امپراتوری. ظرفیت پایه از سطح تان هال می‌آید و ارتقاهای انبار
 * روی آن اضافه می‌شوند. هیچ منبعی هرگز از ظرفیت بیشتر نمی‌شود.
 */
public class ResourceStorage {

    private static final int UPGRADE_BONUS = 150;

    private final Map<ResourceType, Integer> current = new EnumMap<>(ResourceType.class);

    private int levelCapacity = GameConfig.STORAGE_CAPACITY_BY_LEVEL[0];
    private int bonusCapacity = 0;

    public ResourceStorage() {
        for (ResourceType t : ResourceType.values()) current.put(t, 0);

        current.put(ResourceType.FOOD,  30);
        current.put(ResourceType.WOOD,  20);
        current.put(ResourceType.STONE, 10);
        current.put(ResourceType.IRON,   0);
    }

    public int getLevelCapacity() { return levelCapacity; }
    public int getBonusCapacity() { return bonusCapacity; }

    public void restore(int levelCapacity, int bonusCapacity, int[] amounts) {
        this.levelCapacity = Math.max(0, levelCapacity);
        this.bonusCapacity = Math.max(0, bonusCapacity);
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length; i++) {
            int v = amounts != null && i < amounts.length ? Math.max(0, amounts[i]) : 0;
            current.put(types[i], v);
        }
        trimOverflow();
    }

    public int get(ResourceType t) {
        Integer v = current.get(t);
        return v == null ? 0 : v;
    }

    public int getCapacity(ResourceType t) { return levelCapacity + bonusCapacity; }

    /** جای خالی انبار برای این منبع. */
    public int freeSpace(ResourceType t) { return Math.max(0, getCapacity(t) - get(t)); }

    /**
     * افزودن منبع با رعایت ظرفیت.
     * @return مقداری که به‌خاطر پر بودن انبار هدر رفت.
     */
    public int add(ResourceType t, int amount) {
        if (amount <= 0) return 0;
        int space  = freeSpace(t);
        int stored = Math.min(amount, space);
        current.put(t, get(t) + stored);
        return amount - stored;
    }

    public boolean canAfford(ResourceType t, int amount) { return get(t) >= amount; }

    public boolean canAffordAll(int[] foodWoodStoneIron) {
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length && i < foodWoodStoneIron.length; i++) {
            if (get(types[i]) < foodWoodStoneIron[i]) return false;
        }
        return true;
    }

    public void deduct(ResourceType t, int amount) {
        current.put(t, Math.max(0, get(t) - amount));
    }

    public void deductAll(int[] foodWoodStoneIron) {
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length && i < foodWoodStoneIron.length; i++) {
            deduct(types[i], foodWoodStoneIron[i]);
        }
    }

    /** ظرفیت پایه را با سطح تان هال هم‌تراز می‌کند. */
    public void setLevelCapacity(int capacity) {
        this.levelCapacity = Math.max(0, capacity);
        trimOverflow();
    }

    public void upgradeCapacity() {
        bonusCapacity += UPGRADE_BONUS;
    }

    /** حذف سرریز در پایان نوبت. */
    public void trimOverflow() {
        for (ResourceType t : ResourceType.values()) {
            int cap = getCapacity(t);
            if (get(t) > cap) current.put(t, cap);
        }
    }
}
