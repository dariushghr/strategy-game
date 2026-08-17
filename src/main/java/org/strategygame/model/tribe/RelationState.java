package org.strategygame.model.tribe;

/**
 * وضعیت رابطه با یک قبیله. هر وضعیت بازه‌ی عددی خودش را می‌شناسد و
 * می‌گوید چه تعامل‌هایی در آن مجاز است (State Pattern روی مقدار رابطه).
 */
public enum RelationState {

    ENEMY     ("دشمن",      -100, -50),
    DISPLEASED("ناراضی",     -49, -20),
    NEUTRAL   ("بی‌طرف",     -19,  19),
    FRIENDLY  ("دوست",        20,  69),
    ALLIED    ("متحد",        70, 100);

    private final String label;
    private final int    min;
    private final int    max;

    RelationState(String label, int min, int max) {
        this.label = label;
        this.min   = min;
        this.max   = max;
    }

    public String getLabel() { return label; }
    public int getMin()      { return min; }
    public int getMax()      { return max; }

    public static RelationState of(int relation) {
        for (RelationState s : values()) {
            if (relation >= s.min && relation <= s.max) return s;
        }
        return relation < ENEMY.min ? ENEMY : ALLIED;
    }

    public boolean allowsGift()    { return this != ENEMY; }
    public boolean allowsTrade()   { return this == FRIENDLY || this == ALLIED; }
    public boolean allowsMission() { return this == NEUTRAL || this == FRIENDLY || this == ALLIED; }
    public boolean allowsWar()     { return this != ENEMY; }
    public boolean allowsPeace()   { return this == ENEMY; }

    public String rangeText() { return min + " تا " + max; }
}
