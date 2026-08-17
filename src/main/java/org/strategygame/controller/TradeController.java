package org.strategygame.controller;

import org.strategygame.common.ActionResult;
import org.strategygame.model.GameState;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.trade.TradeRequest;
import org.strategygame.model.trade.TradeResult;
import org.strategygame.model.trade.TradeStrategy;
import org.strategygame.model.tribe.Tribe;

import java.util.ArrayList;
import java.util.List;

/** سه کانال تجاری بازی: بازار، پاسگاه تجاری بی‌طرف و قبیله‌ها. */
public class TradeController {

    private final GameState    state;
    private final GameServices services;

    public TradeController(GameState state, GameServices services) {
        this.state    = state;
        this.services = services;
    }

    public TradeStrategy bazaar()      { return services.trade().bazaarStrategy(state); }
    public TradeStrategy tradingPost() { return services.trade().tradingPostStrategy(state); }
    public TradeStrategy tribe(Tribe tribe) { return services.trade().tribeStrategy(tribe); }

    public String bazaarProblem()      { return services.trade().bazaarProblem(state); }
    public String tradingPostProblem() { return services.trade().tradingPostProblem(state); }

    public String tribeProblem(Tribe tribe) {
        return services.trade().tribeTradeProblem(state, tribe);
    }

    /** قبیله‌های کشف‌شده‌ای که به‌عنوان کانال تجاری در پنل نمایش داده می‌شوند. */
    public List<Tribe> knownTribesForTrade() {
        List<Tribe> list = new ArrayList<>();
        for (Tribe tribe : state.getTribes()) {
            if (tribe.isDiscovered() && !tribe.isDefeated() && tribe.getType().isTrader()) {
                list.add(tribe);
            }
        }
        return list;
    }

    /** اعتبارسنجی کامل یک معامله روی یک کانال؛ {@code null} یعنی مجاز. */
    public String tradeProblem(TradeStrategy strategy, ResourceType source,
                               ResourceType target, int amount) {
        if (strategy == null) return "کانال تجاری در دسترس نیست";
        return strategy.validate(state.getStorage(), state.getTurn(),
                new TradeRequest(source, target, amount));
    }

    public ActionResult trade(TradeStrategy strategy, ResourceType source,
                              ResourceType target, int amount) {
        TradeResult result = services.trade().trade(state, strategy,
                new TradeRequest(source, target, amount));
        return result.success()
                ? ActionResult.ok(result.message())
                : ActionResult.fail(result.message());
    }
}
