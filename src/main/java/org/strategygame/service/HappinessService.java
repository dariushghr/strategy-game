package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.happiness.HappinessLevel;
import org.strategygame.model.happiness.HappinessState;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.tribe.RelationState;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.unit.Unit;

/**
 * رویدادهای شادی. مقدار شادی انباشته است؛ رویدادهای شرطی با کلید ثبت می‌شوند
 * تا هر نوبت دوباره اعمال نشوند و با برطرف شدن شرط دقیقا برگردند.
 */
public class HappinessService {

    private static final String KEY_UNIT_CAP        = "unit_cap_reached";
    private static final String KEY_MILITARY_AT_HOME = "military_at_town_hall";

    /** ارزیابی رویدادهای شرطی در پایان هر نوبت. */
    public void evaluateTurn(GameState state) {
        HappinessState happiness = state.getHappiness();

        applyConditional(state, KEY_UNIT_CAP,
                state.militaryUnitCount() >= state.getMilitaryCap() && state.getMilitaryCap() > 0,
                GameConfig.HAPPINESS_UNIT_CAP_REACHED,
                "رسیدن یونیت‌های نظامی به سقف");

        applyConditional(state, KEY_MILITARY_AT_HOME,
                hasMilitaryAtTownHall(state),
                GameConfig.HAPPINESS_MILITARY_AT_HOME,
                "حضور یونیت نظامی در تان هال");

        applyMonuments(state);
        applyApPenalty(state);
    }

    /** ساخت شهرک یا شهر جدید یک بار شادی را کم می‌کند. */
    public void onSettlementBuilt(GameState state, Building settlement) {
        HappinessLevel changed = state.getHappiness().changeOnce(
                "settlement:" + settlement.getId(),
                GameConfig.HAPPINESS_NEW_SETTLEMENT,
                "ساخت شهرک جدید");
        announce(state, changed);
    }

    /** بنای یادبود فعال شادی می‌دهد. */
    private void applyMonuments(GameState state) {
        for (Building b : state.getBuildings()) {
            if (b.getType() != BuildingType.MONUMENT) continue;
            if (!b.isFunctional()) continue;
            HappinessLevel changed = state.getHappiness().changeOnce(
                    "monument:" + b.getId(),
                    GameConfig.MONUMENT_HAPPINESS,
                    "بنای یادبود فعال");
            announce(state, changed);
        }
    }

    public void onTribeAttacked(GameState state, Tribe tribe) {
        RelationState relation = tribe.getRelationState();
        int delta = 0;
        String reason = null;

        if (tribe.isAllied() || relation == RelationState.ALLIED) {
            delta  = GameConfig.HAPPINESS_ATTACK_ALLIED;
            reason = "حمله به قبیله‌ی متحد " + tribe.getName();
        } else if (relation == RelationState.FRIENDLY) {
            delta  = GameConfig.HAPPINESS_ATTACK_FRIENDLY;
            reason = "حمله به قبیله‌ی دوست " + tribe.getName();
        }
        if (delta == 0) return;

        announce(state, state.getHappiness().change(delta, reason));
    }

    public void onWarDeclared(GameState state, Tribe tribe) {
        announce(state, state.getHappiness().change(
                GameConfig.HAPPINESS_DECLARE_WAR, "اعلام جنگ به " + tribe.getName()));
    }

    /**
     * رویداد شرطی: با برقرار شدن شرط یک بار اعمال و با برطرف شدن آن دقیقا
     * همان مقدار برگردانده می‌شود.
     */
    private void applyConditional(GameState state, String key, boolean active,
                                  int delta, String reason) {
        HappinessState happiness = state.getHappiness();
        boolean fired = happiness.hasFired(key);

        if (active && !fired) {
            announce(state, happiness.changeOnce(key, delta, reason));
        } else if (!active && fired) {
            happiness.resetEvent(key);
            announce(state, happiness.change(-delta, "پایان: " + reason));
        }
    }

    private boolean hasMilitaryAtTownHall(GameState state) {
        if (state.getTownHall() == null) return false;
        HexCell home = state.getTownHall().getLocation();
        if (home == null) return false;
        for (Unit u : home.getUnits()) {
            if (u.isAlive() && u.isMilitary()) return true;
        }
        return false;
    }

    /** جریمه‌ی AP شورش در محاسبه‌ی حداکثر AP اعمال می‌شود، نه با عدد پراکنده. */
    public void applyApPenalty(GameState state) {
        int penalty = state.getHappiness().apPenalty();
        for (Unit u : state.getUnits()) u.setApPenalty(penalty);
    }

    private void announce(GameState state, HappinessLevel newLevel) {
        if (newLevel == null) return;
        state.notify(NotificationKind.HAPPINESS,
                "سطح شادی به «" + newLevel.getLabel() + "» تغییر کرد — "
                        + newLevel.getEffectText());
    }
}
