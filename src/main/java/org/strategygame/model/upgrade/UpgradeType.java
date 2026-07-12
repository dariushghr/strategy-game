package org.strategygame.model.upgrade;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public enum UpgradeType {
    STORAGE_UPGRADE_1("ارتقا انبار ۱",  2, new int[]{0,10,5,0},  Collections.<UpgradeType>emptyList()),
    STORAGE_UPGRADE_2("ارتقا انبار ۲",  3, new int[]{0,20,15,0}, Arrays.asList(STORAGE_UPGRADE_1)),
    STONE_MINE_TECH  ("تکنولوژی معدن سنگ",2,new int[]{0,15,0,0}, Collections.<UpgradeType>emptyList()),
    IRON_MINE_TECH   ("تکنولوژی معدن آهن",3,new int[]{0,15,10,0},Arrays.asList(STONE_MINE_TECH)),
    PROFESSIONAL_TOOLS("ابزار حرفه‌ای",  4, new int[]{0,0,10,10}, Arrays.asList(STONE_MINE_TECH, IRON_MINE_TECH)),
    SETTLEMENT_TECH  ("تکنولوژی شهرک",  4, new int[]{0,20,15,5}, Arrays.asList(STONE_MINE_TECH));

    private final String label;
    private final int turns;
    private final int[] cost;
    private final List<UpgradeType> prereqs;

    UpgradeType(String label, int turns, int[] cost, List<UpgradeType> prereqs) {
        this.label   = label;
        this.turns   = turns;
        this.cost    = cost;
        this.prereqs = prereqs;
    }

    public String getLabel()              { return label; }
    public int    getTurns()              { return turns; }
    public int[]  getCost()               { return cost; }
    public List<UpgradeType> getPrereqs() { return prereqs; }

    public boolean isUnlocked(Set<UpgradeType> done) {
        return done.containsAll(prereqs);
    }
}
