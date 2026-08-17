package org.strategygame.model.combat;

import org.strategygame.model.unit.UnitType;

import java.util.List;

/**
 * زنجیره‌ی مسئولیت برای پخش آسیب نبرد. اولویت دریافت ضربه شمشیرزن، بعد کماندار
 * و بعد سوارنظام است؛ حلقه‌ی آخر هر جنگنده‌ی باقی‌مانده را می‌پذیرد.
 */
public class DamageResolver {

    /** {@code null} یعنی این حلقه هر نوع یونیتی را می‌پذیرد. */
    private final UnitType accepted;
    private DamageResolver next;

    public DamageResolver(UnitType accepted) { this.accepted = accepted; }

    /** حلقه‌ی بعدی را وصل می‌کند و خودِ حلقه‌ی بعدی را برمی‌گرداند. */
    public DamageResolver setNext(DamageResolver n) {
        this.next = n;
        return n;
    }

    /** زنجیره‌ی استاندارد بازی: شمشیرزن → کماندار → سوارنظام → بقیه. */
    public static DamageResolver standardChain() {
        DamageResolver head = new DamageResolver(UnitType.SWORDSMAN);
        head.setNext(new DamageResolver(UnitType.ARCHER))
            .setNext(new DamageResolver(UnitType.CAVALRY))
            .setNext(new DamageResolver(null));
        return head;
    }

    /**
     * تعداد ضربه‌ها را روی مدافع‌ها اعمال می‌کند.
     * @return ضربه‌هایی که هدفی برایشان نمانده است.
     */
    public int apply(List<Combatant> targets, int hits, List<Combatant> killed) {
        boolean progressed = true;
        while (hits > 0 && progressed) {
            progressed = false;
            for (Combatant c : targets) {
                if (hits <= 0) break;
                if (!c.isAlive() || !accepts(c)) continue;
                c.takeBattleHit();
                hits--;
                progressed = true;
                if (!c.isAlive()) killed.add(c);
            }
        }
        return (next != null && hits > 0) ? next.apply(targets, hits, killed) : hits;
    }

    private boolean accepts(Combatant c) {
        return accepted == null || c.getType() == accepted;
    }
}
