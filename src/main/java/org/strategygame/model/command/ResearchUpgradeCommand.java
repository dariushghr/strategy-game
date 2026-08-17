package org.strategygame.model.command;

import org.strategygame.model.GameState;
import org.strategygame.model.upgrade.UpgradeType;

/** تحقیق ارتقاهای پایه‌ی بازی (انبار، تکنولوژی معادن، شهرک و ...). */
public class ResearchUpgradeCommand extends TownHallCommand {

    private final UpgradeType upgrade;

    public ResearchUpgradeCommand(UpgradeType upgrade) {
        super(upgrade.getTurns());
        this.upgrade = upgrade;
    }

    public UpgradeType getUpgrade() { return upgrade; }

    @Override public String getLabel() { return "تحقیق " + upgrade.getLabel(); }
    @Override public int[]  getCost()  { return upgrade.getCost(); }

    @Override public String getResultText() { return upgrade.getLabel() + " فعال می‌شود"; }

    @Override
    public String validate(GameState state) {
        if (state.getDoneUpgrades().contains(upgrade))
            return upgrade.getLabel() + " قبلا انجام شده است";
        if (!upgrade.isUnlocked(state.getDoneUpgrades()))
            return "پیش‌نیاز لازم را ندارید";
        return null;
    }

    @Override
    protected void applyEffect(GameState state) {
        state.applyUpgrade(upgrade);
    }
}
