package org.strategygame.model.combat;

/** هر موجودیتی که HP دارد و می‌تواند هدف حمله قرار بگیرد. */
public interface Attackable {

    int getHp();

    int getMaxHp();

    /** نامی که در گزارش نبرد نشان داده می‌شود. */
    String getDisplayName();

    void takeDamage(int amount);

    default boolean isDestroyed() { return getHp() <= 0; }
}
