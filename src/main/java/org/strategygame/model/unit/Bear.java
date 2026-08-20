package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;
import org.strategygame.model.combat.CombatCategory;
import org.strategygame.model.map.HexCell;

/**
 * خرس؛ یک حیوان وحشی که در نبرد فقط یک تاس دارد، به سازه‌ها آسیب نمی‌زند
 * و در هر نوبت حداکثر یک حمله انجام می‌دهد.
 */
public class Bear extends CombatUnit {

    private static final int BATTLE_HP = 3;

    /** لانه‌ی جنگلی که خرس از آن بیرون آمده و به آن برمی‌گردد. */
    private final HexCell den;
    private boolean attackedThisTurn = false;

    public Bear(HexCell den) {
        super(GameConfig.BEAR_AP, 1, GameConfig.BEAR_HP, BATTLE_HP);
        this.den = den;
        setOwner(Owner.WILD);
    }

    public HexCell getDen() { return den; }

    public boolean hasAttackedThisTurn()      { return attackedThisTurn; }
    public void markAttacked()                { this.attackedThisTurn = true; }
    public void resetTurnFlags()              { this.attackedThisTurn = false; }
    public void restoreAttackedThisTurn(boolean v) { this.attackedThisTurn = v; }

    /** آسیب مستقیم حمله‌ی خرس به یونیت. */
    public int getAttackDamage() { return GameConfig.BEAR_ATTACK; }

    @Override public UnitType getType()  { return UnitType.BEAR; }
    @Override public int getAttackRange(){ return GameConfig.BEAR_RANGE; }

    /** خرس به ساختمان آسیب نمی‌زند. */
    @Override public int getStructureDamage() { return 0; }

    @Override public CombatCategory getCombatCategory() { return CombatCategory.WILD_ANIMAL; }
}
