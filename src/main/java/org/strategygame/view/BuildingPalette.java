package org.strategygame.view;

import org.strategygame.model.building.BuildingType;

import java.awt.Color;

/**
 * رنگ اختصاصی هر نوع سازه. هم روی نقشه (رنگ‌آمیزی هکس‌های قابل ساخت)
 * و هم روی دکمه‌های پنل ساخت از همین رنگ‌ها استفاده می‌شود تا بازیکن
 * سریع بفهمد روی هر هکس چه چیزی می‌تواند بسازد.
 */
public final class BuildingPalette {

    private BuildingPalette() { }

    public static Color of(BuildingType t) {
        return switch (t) {
            case LUMBER_MILL  -> new Color(255, 152, 40);
            case STONE_MINE   -> new Color(150, 225, 255);
            case IRON_MINE    -> new Color(150, 125, 255);
            case FARM         -> new Color(250, 235, 70);
            case STABLE       -> new Color(255, 110, 175);
            case DOCK         -> new Color(80, 190, 255);
            case BAZAAR       -> new Color(255, 205, 120);
            case MONUMENT     -> new Color(215, 215, 235);
            case SETTLEMENT   -> new Color(110, 250, 210);
            case TRADING_POST -> new Color(200, 170, 110);
            case OUTPOST      -> new Color(160, 190, 160);
            case TOWN_HALL    -> new Color(255, 80, 80);
        };
    }

    /** رنگ متن روی پس‌زمینه‌ی این سازه. */
    public static Color textOn(BuildingType t) {
        Color c = of(t);
        double luma = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
        return luma > 150 ? new Color(20, 20, 25) : Color.WHITE;
    }

    /** رنگ همان سازه در حالت تیره، برای پس‌زمینه‌ی دکمه‌ها. */
    public static Color dark(BuildingType t) {
        Color c = of(t);
        return new Color((int) (c.getRed() * 0.32) + 18,
                         (int) (c.getGreen() * 0.32) + 22,
                         (int) (c.getBlue() * 0.32) + 32);
    }

    /** حرف نشانه‌ی سازه روی نقشه. */
    public static String marker(BuildingType t) {
        return switch (t) {
            case TOWN_HALL    -> "⌂";
            case LUMBER_MILL  -> "L";
            case STONE_MINE   -> "S";
            case IRON_MINE    -> "I";
            case FARM         -> "F";
            case STABLE       -> "T";
            case DOCK         -> "D";
            case BAZAAR       -> "B";
            case MONUMENT     -> "M";
            case SETTLEMENT   -> "V";
            case TRADING_POST -> "P";
            case OUTPOST      -> "O";
        };
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
