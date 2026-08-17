package org.strategygame.model.combat;

/**
 * سازه‌ی قابل حمله. سازه‌ها بدون تاس آسیب می‌بینند و مقدار آسیب از
 * {@code structureAttack} مهاجم می‌آید.
 */
public interface Structure extends Attackable {

    /** امتیاز دفاعی سازه؛ فعلا فقط برای نمایش و قواعد آینده استفاده می‌شود. */
    int getDefense();

    /** آیا این سازه با اکشن تخریب اختیاری بازیکن قابل حذف است. */
    boolean isDemolishable();
}
