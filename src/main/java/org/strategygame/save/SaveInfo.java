package org.strategygame.save;

/** خلاصه یک اسلات برای نمایش در UI، بدون بارگذاری کامل بازی. */
public record SaveInfo(
        SaveSlot slot,
        boolean occupied,
        long savedAt,
        int turn,
        String gameId,
        String problem
) {
    public static SaveInfo empty(SaveSlot slot) {
        return new SaveInfo(slot, false, 0, 0, null, null);
    }

    public String statusText() {
        if (problem != null) return slot.label() + " — خراب: " + problem;
        if (!occupied) return slot.label() + " — خالی";
        return slot.label() + " — نوبت " + turn;
    }
}
