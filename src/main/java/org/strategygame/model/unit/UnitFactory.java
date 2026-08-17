package org.strategygame.model.unit;

/** ساخت یونیت از روی نوع آن (Factory Method) تا منطق تولید به کلاس‌های خاص وابسته نشود. */
public final class UnitFactory {

    private UnitFactory() { }

    public static Unit create(UnitType type) {
        return switch (type) {
            case EXPLORER        -> new Explorer();
            case BUILDER         -> new Builder();
            case WORKER          -> new Worker();
            case BORDER_EXPANDER -> new BorderExpander();
            case SWORDSMAN       -> new Swordsman();
            case ARCHER          -> new Archer();
            case CAVALRY         -> new Cavalry();
            case BEAR            -> null;
        };
    }

    /** یونیت نظامی از روی نوع؛ برای نگهبان‌های قبیله هم استفاده می‌شود. */
    public static MilitaryUnit createMilitary(UnitType type, Owner owner) {
        Unit unit = create(type);
        if (!(unit instanceof MilitaryUnit military)) return null;
        military.setOwner(owner);
        return military;
    }
}
