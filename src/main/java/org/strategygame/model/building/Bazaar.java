package org.strategygame.model.building;

import org.strategygame.config.GameConfig;

/**
 * بازار. سطح معامله‌ی بازار از سطح تان هال می‌آید؛ داک برای بازار مسیر ارتقای
 * جداگانه‌ای تعریف نکرده است، پس همان سطح امپراتوری ملاک گرفته شده تا نرخ و
 * سقف فروش در یک جا و قابل توضیح باشد.
 */
public class Bazaar extends Building {

    private int tradeLevel = 1;

    public Bazaar() { super(BuildingType.BAZAAR); }

    @Override public int produce(double multiplier) { return 0; }

    public int getTradeLevel() { return tradeLevel; }

    public void setTradeLevel(int level) {
        int max = GameConfig.BAZAAR_RATE.length;
        this.tradeLevel = Math.max(1, Math.min(level, max));
    }

    /** حداکثر مقداری که در یک معامله می‌توان فروخت. */
    public int getMaxAmount() { return GameConfig.BAZAAR_MAX_AMOUNT[tradeLevel - 1]; }

    /** نرخ تبدیل در این سطح. */
    public double getRate() { return GameConfig.BAZAAR_RATE[tradeLevel - 1]; }

    public String rateText() {
        return "سطح " + tradeLevel + ": حداکثر " + getMaxAmount()
                + " واحد با نرخ " + (int) (getRate() * 100) + "٪";
    }
}
