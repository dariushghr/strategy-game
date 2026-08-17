package org.strategygame.model.trade;

import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.resource.ResourceType;

import java.util.Set;

/**
 * قواعد یک کانال تجاری. به‌جای یک {@code switch} بزرگ، هر کانال (بازار،
 * پاسگاه تجاری، قبیله) استراتژی خودش را دارد ولی قواعد مشترک اینجا یک بار
 * نوشته شده‌اند: منبع مبدا و مقصد یکسان نباشند، منبع کافی باشد، ظرفیت مقصد
 * بررسی شود، محاسبه با گرد کردن به پایین و تراکنش اتمیک باشد.
 */
public abstract class TradeStrategy {

    public abstract String getName();

    /** نرخ تبدیل؛ مثلا 0.75 یعنی ۷۵٪. */
    public abstract double getRate();

    /** بیشترین مقداری که در یک معامله قابل فروش است. */
    public abstract int getMaxAmount();

    /** منابعی که این کانال می‌تواند تحویل بدهد؛ مجموعه‌ی خالی یعنی هر منبعی. */
    public Set<ResourceType> allowedTargets() { return Set.of(); }

    /** آیا در این نوبت با این کانال معامله انجام شده است. */
    protected abstract boolean tradedThisTurn(int turn);

    /** ثبت این‌که در این نوبت معامله انجام شد. */
    protected abstract void markTraded(int turn);

    /** محاسبه‌ی مقدار دریافتی با گرد کردن به پایین. */
    public int calculateReceived(TradeRequest request) {
        return (int) Math.floor(request.amount() * getRate());
    }

    /** دلیل غیرمجاز بودن معامله؛ {@code null} یعنی مجاز است. */
    public String validate(ResourceStorage storage, int turn, TradeRequest request) {
        if (request == null || request.source() == null || request.target() == null)
            return "انتخاب منبع کامل نیست";
        if (request.source() == request.target())
            return "منبع مبدا و مقصد نمی‌توانند یکی باشند";
        if (request.amount() <= 0)
            return "مقدار معامله باید بزرگ‌تر از صفر باشد";
        if (request.amount() > getMaxAmount())
            return getName() + " در هر معامله حداکثر " + getMaxAmount() + " واحد قبول می‌کند";
        if (tradedThisTurn(turn))
            return "در این نوبت قبلا با " + getName() + " معامله کرده‌اید";
        if (!storage.canAfford(request.source(), request.amount()))
            return "منبع کافی نیست (" + request.source().getLabel() + " موجود: "
                    + storage.get(request.source()) + ")";

        Set<ResourceType> allowed = allowedTargets();
        if (!allowed.isEmpty() && !allowed.contains(request.target()))
            return getName() + " فقط " + labels(allowed) + " تحویل می‌دهد";

        int received = calculateReceived(request);
        if (received <= 0)
            return "با این مقدار و نرخ " + (int) (getRate() * 100) + "٪ چیزی دریافت نمی‌کنید";
        if (storage.freeSpace(request.target()) < received)
            return "ظرفیت انبار برای " + received + " واحد "
                    + request.target().getLabel() + " کافی نیست";

        String specific = validateSpecific(storage, turn, request);
        return specific;
    }

    /** شرط‌های اختصاصی هر کانال. */
    protected String validateSpecific(ResourceStorage storage, int turn, TradeRequest request) {
        return null;
    }

    /**
     * اجرای اتمیک معامله؛ اگر اعتبارسنجی رد شود هیچ منبعی جابه‌جا نمی‌شود.
     */
    public final TradeResult execute(ResourceStorage storage, int turn, TradeRequest request) {
        String problem = validate(storage, turn, request);
        if (problem != null) return TradeResult.fail(problem);

        int received = calculateReceived(request);
        storage.deduct(request.source(), request.amount());
        storage.add(request.target(), received);
        markTraded(turn);
        onExecuted(request, received);

        return TradeResult.ok(getName() + ": " + request.amount() + " "
                + request.source().getLabel() + " → " + received + " "
                + request.target().getLabel()
                + " (نرخ " + (int) (getRate() * 100) + "٪)", received);
    }

    /** قلاب برای اثرهای جانبی هر کانال بعد از معامله‌ی موفق. */
    protected void onExecuted(TradeRequest request, int received) { }

    public String rateText() { return "نرخ " + (int) (getRate() * 100) + "٪"; }

    private static String labels(Set<ResourceType> types) {
        StringBuilder sb = new StringBuilder();
        for (ResourceType t : types) {
            if (sb.length() > 0) sb.append(" یا ");
            sb.append(t.getLabel());
        }
        return sb.toString();
    }
}
