package org.strategygame.model.unit;

import org.strategygame.model.combat.Combatant;

/**
 * پایه‌ی همه‌ی یونیت‌های جنگنده. HP نبرد از HP واقعی مشتق می‌شود تا فقط یک
 * سیستم سلامت در بازی وجود داشته باشد: هر ضربه‌ی تاسی به اندازه‌ی
 * {@code maxHp / maxBattleHp} آسیب می‌زند.
 */
public abstract class CombatUnit extends Unit implements Combatant {

    private final int maxBattleHp;

    protected CombatUnit(int maxAP, int visionRadius, int maxHp, int maxBattleHp) {
        super(maxAP, visionRadius, maxHp);
        this.maxBattleHp = Math.max(1, maxBattleHp);
    }

    public int getMaxBattleHp() { return maxBattleHp; }

    /** آسیب معادل یک ضربه‌ی تاسی. */
    public int hpPerBattleHit() { return Math.max(1, getMaxHp() / maxBattleHp); }

    @Override public int getBattleHp() {
        return (int) Math.ceil((double) getHp() / hpPerBattleHit());
    }

    @Override public void takeBattleHit() { takeDamage(hpPerBattleHit()); }
}
