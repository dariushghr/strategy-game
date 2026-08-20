package org.strategygame.save;

import org.strategygame.model.GameState;
import org.strategygame.service.TribeService;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * تست بدون UI: ذخیره و بارگذاری باید seed، نوبت، تعداد یونیت و ساختمان را حفظ کند
 * و RNG را دوباره جلو نبرد.
 */
public final class SaveRoundTrip {

    private SaveRoundTrip() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("strategy-game-save");
        try {
            String result = run(dir);
            System.out.println(result);
            if (!result.startsWith("OK")) System.exit(1);
        } finally {
            try (var stream = Files.list(dir)) {
                stream.forEach(p -> p.toFile().delete());
            }
            Files.deleteIfExists(dir);
        }
    }

    public static String run(Path dir) throws Exception {
        GameState original = new GameState(42L);
        original.initialize();
        new TribeService().spawnInitialTribes(original);
        long calls = original.getRandom().getCallCount();
        int units = original.getUnits().size();
        int buildings = original.getBuildings().size();
        int tribes = original.getTribes().size();

        SaveService service = new SaveService(dir);
        service.save(original, SaveSlot.SLOT_1);
        GameState loaded = service.load(SaveSlot.SLOT_1);

        if (loaded.getTurn() != original.getTurn()) return "FAIL turn";
        if (loaded.getRandom().getSeed() != 42L) return "FAIL seed";
        if (loaded.getRandom().getCallCount() != calls) return "FAIL rng";
        if (loaded.getUnits().size() != units) return "FAIL units";
        if (loaded.getBuildings().size() != buildings) return "FAIL buildings";
        if (loaded.getTribes().size() != tribes) return "FAIL tribes";
        if (!original.getGameId().equals(loaded.getGameId())) return "FAIL gameId";

        int nextOriginal = original.getRandom().nextInt(100);
        int nextLoaded = loaded.getRandom().nextInt(100);
        if (nextOriginal != nextLoaded) return "FAIL rng sequence";
        return "OK";
    }
}
