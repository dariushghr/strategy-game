package org.strategygame.model.combat;

import java.util.ArrayList;
import java.util.List;

/**
 * نتیجه‌ی کامل یک نبرد؛ شامل تاس‌ها، جفت‌ها، برنده‌ی هر جفت و تلفات.
 * این کلاس کامل مستقل از UI است تا هم در پنل نمایش و هم در لاگ استفاده شود.
 */
public class CombatResult {

    private final String attackerLabel;
    private final String defenderLabel;

    private final List<Integer>  attackDice  = new ArrayList<>();
    private final List<Integer>  defenseDice = new ArrayList<>();
    private final List<DicePair> pairs       = new ArrayList<>();

    private final List<String> attackerCasualties = new ArrayList<>();
    private final List<String> defenderCasualties = new ArrayList<>();

    private int     wallBonus       = 0;
    private int     structureDamage = 0;
    private String  structureTarget = null;
    private boolean structureDestroyed = false;
    private String  note            = null;

    public CombatResult(String attackerLabel, String defenderLabel) {
        this.attackerLabel = attackerLabel;
        this.defenderLabel = defenderLabel;
    }

    public String getAttackerLabel() { return attackerLabel; }
    public String getDefenderLabel() { return defenderLabel; }
    public List<Integer> getAttackDice()  { return attackDice; }
    public List<Integer> getDefenseDice() { return defenseDice; }
    public List<DicePair> getPairs()      { return pairs; }
    public List<String> getAttackerCasualties() { return attackerCasualties; }
    public List<String> getDefenderCasualties() { return defenderCasualties; }
    public int getWallBonus()          { return wallBonus; }
    public int getStructureDamage()    { return structureDamage; }
    public String getStructureTarget() { return structureTarget; }
    public boolean isStructureDestroyed() { return structureDestroyed; }
    public String getNote()            { return note; }

    public boolean isStructureAttack() { return structureTarget != null; }

    public void addAttackDie(int v)  { attackDice.add(v); }
    public void addDefenseDie(int v) { defenseDice.add(v); }
    public void addPair(DicePair p)  { pairs.add(p); }
    public void addAttackerCasualty(String name) { attackerCasualties.add(name); }
    public void addDefenderCasualty(String name) { defenderCasualties.add(name); }
    public void setWallBonus(int v)  { this.wallBonus = v; }
    public void setNote(String v)    { this.note = v; }

    public void setStructureOutcome(String target, int damage, boolean destroyed) {
        this.structureTarget    = target;
        this.structureDamage    = damage;
        this.structureDestroyed = destroyed;
    }

    public int attackerWonPairs() {
        return (int) pairs.stream().filter(DicePair::attackerWins).count();
    }

    public int defenderWonPairs() { return pairs.size() - attackerWonPairs(); }

    /** گزارش خوانا برای نمایش به بازیکن. */
    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("مهاجم: ").append(attackerLabel).append('\n');
        sb.append("مدافع: ").append(defenderLabel).append('\n');

        if (isStructureAttack()) {
            sb.append("\nحمله به سازه بدون تاس انجام می‌شود.\n");
            sb.append("هدف: ").append(structureTarget)
              .append("  |  آسیب: ").append(structureDamage).append('\n');
            if (structureDestroyed) sb.append("سازه نابود شد.\n");
            if (note != null) sb.append('\n').append(note).append('\n');
            return sb.toString();
        }

        sb.append("\nتاس حمله: ").append(diceText(attackDice)).append('\n');
        sb.append("تاس دفاع: ").append(diceText(defenseDice));
        if (wallBonus > 0) sb.append("  (شامل +").append(wallBonus).append(" دیوار، سقف ۶)");
        sb.append('\n');

        sb.append("\nمقایسه‌ی جفت‌ها:\n");
        if (pairs.isEmpty()) {
            sb.append("  جفتی برای مقایسه وجود نداشت.\n");
        } else {
            for (int i = 0; i < pairs.size(); i++) {
                sb.append("  جفت ").append(i + 1).append(": ").append(pairs.get(i).text()).append('\n');
            }
        }

        sb.append("\nتلفات مدافع: ").append(casualtyText(defenderCasualties)).append('\n');
        sb.append("تلفات مهاجم: ").append(casualtyText(attackerCasualties)).append('\n');
        if (note != null) sb.append('\n').append(note).append('\n');
        return sb.toString();
    }

    private String diceText(List<Integer> dice) {
        if (dice.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dice.size(); i++) {
            if (i > 0) sb.append(" , ");
            sb.append(dice.get(i));
        }
        return sb.toString();
    }

    private String casualtyText(List<String> list) {
        return list.isEmpty() ? "—" : String.join(" , ", list);
    }
}
