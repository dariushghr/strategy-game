package org.strategygame.model.trade;

/** نتیجه‌ی یک معامله. */
public record TradeResult(boolean success, String message, int received) {

    public static TradeResult ok(String message, int received) {
        return new TradeResult(true, message, received);
    }

    public static TradeResult fail(String message) {
        return new TradeResult(false, message, 0);
    }
}
