package org.strategygame.model.unit;

/** مالک یک یونیت روی نقشه. */
public enum Owner {
    PLAYER("بازیکن"),
    TRIBE("قبیله"),
    WILD("حیات وحش");

    private final String label;
    Owner(String label) { this.label = label; }
    public String getLabel() { return label; }

    public boolean isPlayer() { return this == PLAYER; }

    /** آیا این مالک برای بازیکن دشمن است. */
    public boolean isHostileToPlayer() { return this != PLAYER; }
}
