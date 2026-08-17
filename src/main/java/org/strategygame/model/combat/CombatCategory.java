package org.strategygame.model.combat;

import org.strategygame.config.GameConfig;

/** دسته‌ی رزمی یک جنگنده که تعداد تاس دفاع پایه‌اش را مشخص می‌کند. */
public enum CombatCategory {

    REGULAR("نظامی", GameConfig.DEFENDER_MIN_DICE),
    BARBARIAN("بربر", GameConfig.BARBARIAN_DEFENSE_DICE),
    WILD_ANIMAL("حیوان وحشی", GameConfig.WILD_ANIMAL_DEFENSE_DICE);

    private final String label;
    private final int    defenseDice;

    CombatCategory(String label, int defenseDice) {
        this.label       = label;
        this.defenseDice = defenseDice;
    }

    public String getLabel()   { return label; }
    public int getDefenseDice(){ return defenseDice; }
}
