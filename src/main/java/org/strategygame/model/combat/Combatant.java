package org.strategygame.model.combat;

import org.strategygame.model.map.HexCell;
import org.strategygame.model.unit.Owner;
import org.strategygame.model.unit.UnitType;

/** هر یونیتی که می‌تواند در نبرد تاسی شرکت کند. */
public interface Combatant extends Attackable {

    HexCell getPosition();

    UnitType getType();

    Owner getOwner();

    /** برد حمله بر حسب تعداد هکس. */
    int getAttackRange();

    /** آسیبی که بدون تاس به سازه می‌زند؛ صفر یعنی به سازه آسیب نمی‌زند. */
    int getStructureDamage();

    CombatCategory getCombatCategory();

    /** تعداد ضربه‌ی تاسی که تا مرگ تحمل می‌کند. */
    int getBattleHp();

    /** اعمال یک ضربه‌ی تاسی. */
    void takeBattleHit();

    boolean isAlive();

    boolean hasAP(int n);

    void spendAP(int n);
}
