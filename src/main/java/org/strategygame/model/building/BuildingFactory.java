package org.strategygame.model.building;

/**
 * سازه‌ی تولیدی عمومی؛ ظرفیت کارگر و میزان تولید را از روی
 * {@link BuildingType} برمی‌دارد تا عددی که به بازیکن نشان داده می‌شود
 * دقیقا همان عددی باشد که در بازی تولید می‌شود.
 */
class ProductionBuilding extends Building {

    ProductionBuilding(BuildingType type) { super(type); }

    @Override public int produce(double multiplier) {
        return (int) Math.floor(workerCount() * getType().getYieldPerWorker() * multiplier);
    }
}

/** سازه‌ای که خودش منبعی تولید نمی‌کند (بازار، بنای یادبود، پاسگاه و ...). */
class StaticBuilding extends Building {

    StaticBuilding(BuildingType type) { super(type); }

    @Override public int produce(double multiplier) { return 0; }
}

public class BuildingFactory {

    public static Building create(BuildingType t) {
        return switch (t) {
            case SETTLEMENT -> new Settlement();
            case TOWN_HALL  -> new TownHall();
            case BAZAAR     -> new Bazaar();
            case MONUMENT, TRADING_POST, OUTPOST -> new StaticBuilding(t);
            default         -> new ProductionBuilding(t);
        };
    }
}
