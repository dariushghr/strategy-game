package org.strategygame.model.happiness;

import org.strategygame.config.GameConfig;

/** سطح شادی امپراتوری که از مقدار انباشته‌ی شادی مشتق می‌شود. */
public enum HappinessLevel {

    GOLDEN_AGE("عصر طلایی", "تولید سازه‌ها +"
            + (int) (GameConfig.GOLDEN_AGE_PRODUCTION_BONUS * 100) + "٪"),
    NORMAL("عادی", "بدون اثر"),
    UNREST("نارضایتی", "هر کارگر " + GameConfig.UNREST_WORKER_PENALTY + " واحد کمتر تولید می‌کند"),
    REBELLION("شورش", "جریمه‌ی نارضایتی + حرکت یونیت‌ها "
            + GameConfig.REBELLION_AP_PENALTY + " AP کمتر");

    private final String label;
    private final String effectText;

    HappinessLevel(String label, String effectText) {
        this.label      = label;
        this.effectText = effectText;
    }

    public String getLabel()      { return label; }
    public String getEffectText() { return effectText; }

    public static HappinessLevel of(int value) {
        if (value >= GameConfig.HAPPINESS_GOLDEN_AGE_MIN) return GOLDEN_AGE;
        if (value <= GameConfig.HAPPINESS_REBELLION_MAX)  return REBELLION;
        if (value <= GameConfig.HAPPINESS_UNREST_MAX)     return UNREST;
        return NORMAL;
    }

    /** ضریبی که روی تولید نهایی سازه‌ها اعمال می‌شود. */
    public double productionMultiplier() {
        return this == GOLDEN_AGE ? 1.0 + GameConfig.GOLDEN_AGE_PRODUCTION_BONUS : 1.0;
    }

    /** کاهش تولید هر کارگر. */
    public int workerPenalty() {
        return (this == UNREST || this == REBELLION) ? GameConfig.UNREST_WORKER_PENALTY : 0;
    }

    /** کاهش حداکثر AP یونیت‌ها. */
    public int apPenalty() {
        return this == REBELLION ? GameConfig.REBELLION_AP_PENALTY : 0;
    }
}
