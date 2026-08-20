package org.strategygame.model.tribe;

import org.strategygame.model.combat.Structure;

/** اردوگاه قبیله؛ سازه‌ای با HP که بعد از نابودی به پاسگاه بازیکن تبدیل می‌شود. */
public class TribeCamp implements Structure {

    private final TribeType type;
    private final int maxHp;
    private int hp;

    public TribeCamp(TribeType type) {
        this.type  = type;
        this.maxHp = type.getCampHp();
        this.hp    = this.maxHp;
    }

    @Override public int getHp()      { return hp; }
    @Override public int getMaxHp()   { return maxHp; }
    @Override public int getDefense() { return 0; }

    /** اردوگاه فعال قبیله با اکشن تخریب بازیکن حذف نمی‌شود. */
    @Override public boolean isDemolishable() { return false; }

    @Override public String getDisplayName() { return "اردوگاه " + type.getLabel(); }

    @Override public void takeDamage(int amount) {
        if (amount > 0) hp = Math.max(0, hp - amount);
    }

    public void restoreHp(int saved) {
        this.hp = Math.max(0, Math.min(maxHp, saved));
    }
}
