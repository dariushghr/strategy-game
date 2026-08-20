package org.strategygame.view;

/** دلیل قفل بودن ورودی؛ برای tooltip دکمهٔ ذخیره و جلوگیری از دابل‌کلیک پایان نوبت. */
public enum BusyReason {
    NONE(null),
    END_TURN("تا پایان نوبت ذخیره ممکن نیست"),
    ANIMATION("تا پایان حرکت ذخیره ممکن نیست"),
    DISASTER("تا پایان بلای طبیعی ذخیره ممکن نیست"),
    COMBAT("تا پایان نمایش نبرد ذخیره ممکن نیست"),
    SAVE("در حال ذخیره...");

    private final String message;
    BusyReason(String message) { this.message = message; }
    public String message() { return message; }
    public boolean busy() { return this != NONE; }
}
