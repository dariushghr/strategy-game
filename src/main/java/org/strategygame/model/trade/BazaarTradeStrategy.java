package org.strategygame.model.trade;

import org.strategygame.model.building.Bazaar;
import org.strategygame.model.resource.ResourceStorage;

/** معامله در بازار امپراتوری؛ نرخ و سقف از سطح بازار می‌آید. */
public class BazaarTradeStrategy extends TradeStrategy {

    private final Bazaar bazaar;

    public BazaarTradeStrategy(Bazaar bazaar) { this.bazaar = bazaar; }

    @Override public String getName()  { return "بازار"; }
    @Override public double getRate()  { return bazaar.getRate(); }
    @Override public int getMaxAmount(){ return bazaar.getMaxAmount(); }

    @Override protected boolean tradedThisTurn(int turn) { return bazaar.hasTradedOn(turn); }
    @Override protected void markTraded(int turn)        { bazaar.markTraded(turn); }

    @Override
    protected String validateSpecific(ResourceStorage storage, int turn, TradeRequest request) {
        if (!bazaar.isFunctional()) return "بازار فعلا کار نمی‌کند";
        return null;
    }
}
