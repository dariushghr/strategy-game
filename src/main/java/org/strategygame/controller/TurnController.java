package org.strategygame.controller;

import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.command.TownHallCommand;
import org.strategygame.model.disaster.DisasterEvent;
import org.strategygame.model.disaster.DisasterType;
import org.strategygame.model.event.DisasterStartedEvent;
import org.strategygame.model.event.EventBus;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.event.SaveCompletedEvent;
import org.strategygame.model.event.TurnEndedEvent;
import org.strategygame.save.SaveSlot;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.season.Season;
import org.strategygame.model.unit.Unit;
import org.strategygame.view.GameWindow;

/**
 * ترتیب یک نوبت کامل. همه‌ی سیستم‌های بازی از همین یک مسیر جلو می‌روند تا
 * ترتیب اثرها قابل توضیح و تکرارپذیر بماند:
 *
 * <pre>
 * پایان نوبت بازیکن: تولید → نگهداری → مصرف غذا → صف تان هال → شادی
 *                    → نوبت قبیله‌ها → نوبت خرس‌ها → اثرهای موقت
 * شروع نوبت بعد:     شماره‌ی نوبت و فصل → تازه‌سازی AP → قرعه‌ی بلای طبیعی → فوگ
 * </pre>
 */
public class TurnController {

    private final GameState    state;
    private final GameServices services;
    private GameWindow window;
    private SaveController saveCtrl;
    private EventBus events;

    public TurnController(GameState state, GameServices services) {
        this.state    = state;
        this.services = services;
    }

    public void setWindow(GameWindow window) { this.window = window; }
    public void setSaveController(SaveController saveCtrl) { this.saveCtrl = saveCtrl; }
    public void setEventBus(EventBus events) { this.events = events; }

    public void execute() {
        // ---------------------------------------------------- پایان نوبت بازیکن
        services.production().applyProduction(state);
        applyUpkeep();
        applyFoodConsumption();
        advanceProductionQueue();
        services.happiness().evaluateTurn(state);

        services.tribeTurn().executeTurn(state);
        services.disaster().runBearTurn(state);

        tickTemporaryEffects();
        state.getStorage().trimOverflow();

        // -------------------------------------------------------- شروع نوبت بعد
        Season before = state.getSeason();
        state.nextTurn();
        announceSeasonChange(before, state.getSeason());

        refreshAP();
        rollDisaster();

        services.tribe().updateDiscovery(state);
        state.getFog().update(state.getUnits(), state.getBuildings());

        if (events != null) events.publish(new TurnEndedEvent(state.getTurn(), state.getSeason()));
        autosaveQuietly();

        if (window != null) window.onTurnAdvanced();
    }

    // ------------------------------------------------------------------ تولید
    private void applyUpkeep() {
        ResourceStorage storage = state.getStorage();
        for (Building b : state.getBuildings()) {
            if (!b.isFunctional()) continue;

            int[] upkeep = b.getType().getUpkeepCost();
            if (storage.canAffordAll(upkeep)) {
                storage.deductAll(upkeep);
                b.payUpkeep();
            } else {
                b.missUpkeep();
                if (b.isBroken()) {
                    state.notify(NotificationKind.PRODUCTION,
                            b.getType().getLabel() + " به‌خاطر نپرداختن نگهداری خراب شد");
                }
            }
        }
    }

    private void applyFoodConsumption() {
        ResourceStorage storage = state.getStorage();
        int consumption = state.getUnits().size() * GameState.FOOD_PER_UNIT;
        boolean starving = storage.get(ResourceType.FOOD) < consumption;

        storage.deduct(ResourceType.FOOD, consumption);
        state.setStarvation(starving);

        if (!starving) return;
        state.notify(NotificationKind.PRODUCTION, "قحطی! یونیت‌ها یک AP کمتر دارند");
    }

    /** یک نوبت از دستور فعال تان هال کم می‌کند و اثر آن را در زمان اتمام اعمال می‌کند. */
    private void advanceProductionQueue() {
        if (state.getTownHall() == null) return;

        TownHallCommand done = state.getTownHall().getQueue().advanceTurn();
        if (done == null) return;

        String problem = done.validate(state);
        if (problem != null) {
            state.notify(NotificationKind.PRODUCTION,
                    "«" + done.getLabel() + "» اجرا نشد: " + problem);
            return;
        }
        done.complete(state);
    }

    private void tickTemporaryEffects() {
        for (Building b : state.getBuildings()) b.tickTemporaryEffects();
    }

    private void refreshAP() {
        boolean starving = state.isStarvation();
        for (Unit u : state.getUnits()) {
            if (!u.isAlive()) continue;
            u.refreshAP();
            if (starving) u.spendAP(1);
        }
    }

    // ------------------------------------------------------------ فصل و بلایا
    private void announceSeasonChange(Season before, Season now) {
        if (before == now) return;
        state.notify(NotificationKind.SEASON,
                "فصل به " + now.getLabel() + " تغییر کرد — " + now.getEffectText());
    }

    private void rollDisaster() {
        DisasterEvent event = services.disaster().rollForTurn(state);
        if (event == null) return;
        if (events != null) events.publish(new DisasterStartedEvent(event));
        if (window != null) window.onDisaster(event);
    }

    /** برای دیباگ: اجرای دستی یک بلای طبیعی بدون انتظار برای قرعه‌ی ۵٪. */
    public DisasterEvent forceDisaster(DisasterType type) {
        DisasterEvent event = services.disaster().trigger(state, type);
        if (event != null && events != null) events.publish(new DisasterStartedEvent(event));
        if (event != null && window != null) window.onDisaster(event);
        return event;
    }

    /** برای دیباگ: پریدن به ابتدای یک فصل مشخص. */
    public void forceSeason(Season season) {
        state.forceSeason(season);
        state.notify(NotificationKind.SEASON,
                "فصل به‌صورت دستی روی " + season.getLabel() + " تنظیم شد");
    }

    public boolean canForce(DisasterType type) {
        return switch (type) {
            case EARTHQUAKE  -> !services.disaster().landCells(state).isEmpty();
            case FLOOD       -> !services.disaster().floodCenters(state).isEmpty();
            case BEAR_ATTACK -> !services.disaster().bearDens(state).isEmpty();
        };
    }

    private void autosaveQuietly() {
        if (saveCtrl == null) return;
        var result = saveCtrl.autosave();
        if (events != null) {
            events.publish(new SaveCompletedEvent(SaveSlot.AUTOSAVE, true, result.success(), result.message()));
        }
        if (!result.success()) {
            state.notify(NotificationKind.GENERAL, "ذخیرهٔ خودکار انجام نشد");
        }
    }
}
