package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Bazaar;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.trade.BazaarTradeStrategy;
import org.strategygame.model.trade.TradeRequest;
import org.strategygame.model.trade.TradeResult;
import org.strategygame.model.trade.TradeStrategy;
import org.strategygame.model.trade.TradingPostTradeStrategy;
import org.strategygame.model.trade.TribeTradeStrategy;
import org.strategygame.model.tribe.Tribe;

/** انتخاب کانال تجاری مناسب و اجرای معامله. */
public class TradeService {

    /** استراتژی بازار امپراتوری؛ {@code null} یعنی بازاری در دسترس نیست. */
    public TradeStrategy bazaarStrategy(GameState state) {
        Bazaar bazaar = state.findBazaar();
        return bazaar == null ? null : new BazaarTradeStrategy(bazaar);
    }

    /** دلیل در دسترس نبودن بازار. */
    public String bazaarProblem(GameState state) {
        int level = state.getTownHall() == null ? 1 : state.getTownHall().getLevel();
        if (level < BuildingType.BAZAAR.getRequiredTownHallLevel())
            return "بازار به تان هال سطح " + BuildingType.BAZAAR.getRequiredTownHallLevel()
                    + " نیاز دارد";
        Bazaar bazaar = state.findBazaar();
        if (bazaar == null)          return "هنوز بازاری نساخته‌اید";
        if (!bazaar.isFunctional())  return "بازار فعلا کار نمی‌کند";
        if (bazaar.hasTradedOn(state.getTurn()))
            return "در این نوبت قبلا با بازار معامله کرده‌اید";
        return null;
    }

    /** استراتژی پاسگاه تجاری بی‌طرف؛ فقط اگر داخل قلمرو بازیکن باشد. */
    public TradeStrategy tradingPostStrategy(GameState state) {
        Building post = state.findUsableTradingPost();
        return post == null ? null : new TradingPostTradeStrategy(post);
    }

    public String tradingPostProblem(GameState state) {
        Building post = state.findTradingPost();
        if (post == null) return "پاسگاه تجاری روی نقشه پیدا نشد";
        if (post.getLocation() == null || !post.getLocation().isInBorder())
            return "پاسگاه تجاری فقط وقتی قابل استفاده است که هکس آن داخل قلمرو شما باشد";
        if (post.hasTradedOn(state.getTurn()))
            return "در این نوبت قبلا با پاسگاه تجاری معامله کرده‌اید";
        return null;
    }

    public TradeStrategy tribeStrategy(Tribe tribe) {
        return tribe == null ? null : new TribeTradeStrategy(tribe);
    }

    public String tribeTradeProblem(GameState state, Tribe tribe) {
        if (tribe == null)          return "قبیله‌ای انتخاب نشده است";
        if (tribe.isDefeated())     return tribe.getName() + " دیگر وجود ندارد";
        if (!tribe.isDiscovered())  return "این قبیله را هنوز کشف نکرده‌اید";
        if (!tribe.getType().isTrader())
            return tribe.getType().getLabel() + " تجارت نمی‌کند";
        if (tribe.isAtWar())        return "با این قبیله در جنگ هستید";
        if (tribe.getRelation() < GameConfig.RELATION_TRADE_MIN)
            return "تجارت به رابطه‌ی حداقل " + GameConfig.RELATION_TRADE_MIN
                    + " نیاز دارد (فعلی: " + tribe.getRelation() + ")";
        if (tribe.hasTradedOn(state.getTurn()))
            return "در این نوبت قبلا با این قبیله معامله کرده‌اید";
        return null;
    }

    /** اجرای معامله؛ اعتبارسنجی و اتمیک بودن داخل خود استراتژی است. */
    public TradeResult trade(GameState state, TradeStrategy strategy, TradeRequest request) {
        if (strategy == null) return TradeResult.fail("کانال تجاری در دسترس نیست");

        TradeResult result = strategy.execute(state.getStorage(), state.getTurn(), request);
        if (result.success()) state.notify(NotificationKind.TRADE, result.message());
        return result;
    }
}
