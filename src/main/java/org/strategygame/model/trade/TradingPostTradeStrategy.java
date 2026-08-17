package org.strategygame.model.trade;

import org.strategygame.config.GameConfig;
import org.strategygame.model.building.Building;

/**
 * پاسگاه تجاری بی‌طرف. از ابتدای بازی روی نقشه است و فقط وقتی هکس آن داخل
 * قلمرو بازیکن باشد قابل استفاده است؛ این شرط در سرویس تجارت بررسی می‌شود.
 */
public class TradingPostTradeStrategy extends TradeStrategy {

    private final Building post;

    public TradingPostTradeStrategy(Building post) { this.post = post; }

    @Override public String getName()  { return "پاسگاه تجاری"; }
    @Override public double getRate()  { return GameConfig.TRADING_POST_RATE; }
    @Override public int getMaxAmount(){ return GameConfig.TRADING_POST_MAX_AMOUNT; }

    @Override protected boolean tradedThisTurn(int turn) { return post.hasTradedOn(turn); }
    @Override protected void markTraded(int turn)        { post.markTraded(turn); }
}
