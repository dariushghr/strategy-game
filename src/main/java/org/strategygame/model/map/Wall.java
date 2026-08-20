package org.strategygame.model.map;

import org.strategygame.config.GameConfig;
import org.strategygame.model.combat.Structure;

/**
 * دیوار روی یال بین دو هکس. مثل هر سازه‌ی دیگری HP دارد و می‌تواند
 * مستقیما هدف حمله قرار بگیرد.
 */
public class Wall implements Structure {

    private final int maxHp = GameConfig.WALL_MAX_HP;
    private int hp = GameConfig.WALL_MAX_HP;

    public Wall() { }

    public Wall(int hp) { this.hp = Math.max(0, Math.min(maxHp, hp)); }

    @Override public int getHp()      { return hp; }
    @Override public int getMaxHp()   { return maxHp; }
    @Override public int getDefense() { return GameConfig.WALL_DEFENSE_BONUS; }
    @Override public boolean isDemolishable() { return true; }
    @Override public String getDisplayName()  { return "دیوار"; }

    @Override public void takeDamage(int amount) {
        if (amount > 0) hp = Math.max(0, hp - amount);
    }

    public void repair(int amount) { hp = Math.min(maxHp, hp + Math.max(0, amount)); }
}
