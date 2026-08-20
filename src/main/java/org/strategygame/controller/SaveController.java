package org.strategygame.controller;

import org.strategygame.common.ActionResult;
import org.strategygame.model.GameState;
import org.strategygame.save.SaveException;
import org.strategygame.save.SaveInfo;
import org.strategygame.save.SaveService;
import org.strategygame.save.SaveSlot;

import java.util.List;
import java.util.function.Supplier;

/** ذخیره دستی، بارگذاری و ذخیرهٔ خودکار. UI فقط از همین کنترلر استفاده می‌کند. */
public class SaveController {

    private final SaveService saves;
    private GameState state;
    private Supplier<String> busyReason = () -> null;

    public SaveController(SaveService saves) {
        this.saves = saves;
    }

    public void bind(GameState state) { this.state = state; }

    public void setBusyReason(Supplier<String> busyReason) {
        this.busyReason = busyReason == null ? () -> null : busyReason;
    }

    public GameState state() { return state; }

    public List<SaveInfo> slots() { return saves.list(); }

    public ActionResult save(SaveSlot slot) {
        String busy = busyReason.get();
        if (busy != null) return ActionResult.fail(busy);
        if (slot == null || slot.isAutosave()) {
            return ActionResult.fail("این اسلات برای ذخیرهٔ دستی نیست");
        }
        try {
            saves.save(state, slot);
            return ActionResult.ok("بازی در " + slot.label() + " ذخیره شد");
        } catch (SaveException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    public GameState load(SaveSlot slot) throws SaveException {
        String busy = busyReason.get();
        if (busy != null) throw new SaveException(SaveException.Kind.BUSY, busy);
        GameState loaded = saves.load(slot);
        this.state = loaded;
        return loaded;
    }

    /** شکست ذخیرهٔ خودکار بازی را متوقف نمی‌کند. */
    public ActionResult autosave() {
        try {
            saves.save(state, SaveSlot.AUTOSAVE);
            return ActionResult.ok("ذخیرهٔ خودکار انجام شد");
        } catch (Exception e) {
            return ActionResult.fail("ذخیرهٔ خودکار انجام نشد");
        }
    }
}
