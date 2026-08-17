package org.strategygame.model.combat;

/**
 * یک جفت تاس مقایسه‌شده. تاس‌ها نزولی مرتب و جفت‌به‌جفت مقایسه می‌شوند و
 * تساوی به نفع مدافع است.
 */
public record DicePair(int attackDie, int defenseDie, boolean attackerWins) {

    public static DicePair compare(int attackDie, int defenseDie) {
        return new DicePair(attackDie, defenseDie, attackDie > defenseDie);
    }

    public String text() {
        return attackDie + " در برابر " + defenseDie + " ← "
                + (attackerWins ? "برد مهاجم" : "برد مدافع");
    }
}
