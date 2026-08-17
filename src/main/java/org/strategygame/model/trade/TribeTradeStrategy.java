package org.strategygame.model.trade;

import org.strategygame.config.GameConfig;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.tribe.Tribe;

import java.util.Set;

/**
 * تجارت با قبیله. نرخ و منابع خروجی از نوع قبیله می‌آید و در هر نوبت با هر
 * قبیله فقط یک معامله ممکن است.
 */
public class TribeTradeStrategy extends TradeStrategy {

    private final Tribe tribe;

    public TribeTradeStrategy(Tribe tribe) { this.tribe = tribe; }

    @Override public String getName()  { return "تجارت با " + tribe.getName(); }
    @Override public double getRate()  { return tribe.getTradeRate(); }
    @Override public int getMaxAmount(){ return GameConfig.TRIBE_TRADE_MAX_AMOUNT; }

    @Override public Set<ResourceType> allowedTargets() {
        return tribe.getType().getTradeOutputs();
    }

    @Override protected boolean tradedThisTurn(int turn) { return tribe.hasTradedOn(turn); }
    @Override protected void markTraded(int turn)        { tribe.markTraded(turn); }

    @Override
    protected String validateSpecific(ResourceStorage storage, int turn, TradeRequest request) {
        if (tribe.isDefeated())    return tribe.getName() + " دیگر وجود ندارد";
        if (!tribe.getType().isTrader()) return tribe.getType().getLabel() + " تجارت نمی‌کند";
        if (tribe.isAtWar())       return "با " + tribe.getName() + " در جنگ هستید";
        if (!tribe.getRelationState().allowsTrade())
            return "تجارت به رابطه‌ی حداقل " + GameConfig.RELATION_TRADE_MIN
                    + " نیاز دارد (فعلی: " + tribe.getRelation() + ")";
        if (tribe.getRelation() < GameConfig.RELATION_TRADE_MIN)
            return "رابطه کافی نیست";
        return null;
    }
}
