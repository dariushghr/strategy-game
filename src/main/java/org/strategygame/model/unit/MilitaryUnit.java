package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;
import org.strategygame.model.combat.CombatCategory;

/** یونیت نظامی امپراتوری یا قبیله. */
public abstract class MilitaryUnit extends CombatUnit {

    private final int attackRange;
    private final int structureDamage;

    protected MilitaryUnit(int maxAP, int maxHp, int battleHp,
                           int attackRange, int structureDamage) {
        super(maxAP, GameConfig.MILITARY_VISION, maxHp, battleHp);
        this.attackRange     = attackRange;
        this.structureDamage = structureDamage;
    }

    @Override public boolean isMilitary() { return true; }
    @Override public int getAttackRange() { return attackRange; }
    @Override public int getStructureDamage() { return structureDamage; }
    @Override public CombatCategory getCombatCategory() { return CombatCategory.REGULAR; }

    /** حداکثر تعداد این نوع یونیت در یک هکس. */
    public static int hexCapacity(UnitType type) {
        return switch (type) {
            case SWORDSMAN -> GameConfig.HEX_CAP_SWORDSMAN;
            case ARCHER    -> GameConfig.HEX_CAP_ARCHER;
            case CAVALRY   -> GameConfig.HEX_CAP_CAVALRY;
            default        -> Integer.MAX_VALUE;
        };
    }
}
