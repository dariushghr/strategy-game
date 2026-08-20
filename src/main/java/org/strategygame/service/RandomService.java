package org.strategygame.service;

import org.strategygame.config.GameConfig;

import java.util.List;
import java.util.Random;

/**
 * تک منبع تصادفی بازی. همه‌ی تاس‌ها، بلایای طبیعی و تولید نقشه از همین سرویس
 * استفاده می‌کنند تا با ذخیره‌ی {@code seed} و تعداد فراخوانی، نتیجه‌ی بازی
 * قابل بازتولید باشد.
 */
public class RandomService {

    private final long seed;
    private Random     random;
    private long       callCount = 0;

    public RandomService(long seed) {
        this.seed   = seed;
        this.random = new Random(seed);
    }

    public long getSeed()      { return seed; }
    public long getCallCount() { return callCount; }

    public int nextInt(int bound) {
        callCount++;
        return random.nextInt(Math.max(1, bound));
    }

    public double nextDouble() {
        callCount++;
        return random.nextDouble();
    }

    /** یک تاس شش‌وجهی. */
    public int rollDice() { return 1 + nextInt(GameConfig.DICE_SIDES); }

    public boolean chance(double probability) { return nextDouble() < probability; }

    public <T> T pick(List<T> items) {
        if (items == null || items.isEmpty()) return null;
        return items.get(nextInt(items.size()));
    }

    /**
     * وضعیت تصادفی را به یک نقطه‌ی مشخص برمی‌گرداند؛ برای بازگرداندن بازی
     * ذخیره‌شده به همان مسیر تصادفی قبلی.
     * توجه: {@code java.util.Random.nextInt} و {@code nextDouble} مقدار متفاوتی
     * از جریان تصادفی مصرف می‌کنند، بنابراین بازپخش فقط با {@code nextDouble}
     * دقیق نیست. برای Load از {@link #snapshot}/{@link #restoreSnapshot} استفاده شود.
     */
    public void restoreTo(long targetCallCount) {
        random    = new Random(seed);
        callCount = 0;
        while (callCount < targetCallCount) nextDouble();
    }

    /** تصویر باینری {@link Random} برای ذخیرهٔ دقیق جریان تصادفی. */
    public byte[] snapshot() {
        try {
            var out = new java.io.ByteArrayOutputStream();
            try (var oos = new java.io.ObjectOutputStream(out)) {
                oos.writeObject(random);
            }
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("نمی‌توان وضعیت تصادفی را ذخیره کرد", e);
        }
    }

    public void restoreSnapshot(byte[] data) {
        if (data == null || data.length == 0) return;
        try (var ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data))) {
            this.random = (Random) ois.readObject();
        } catch (java.io.IOException | ClassNotFoundException e) {
            throw new IllegalStateException("نمی‌توان وضعیت تصادفی را بارگذاری کرد", e);
        }
    }

    public void restoreSnapshot(byte[] data, long savedCallCount) {
        restoreSnapshot(data);
        this.callCount = Math.max(0, savedCallCount);
    }
}
