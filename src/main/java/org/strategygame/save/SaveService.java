package org.strategygame.save;

import org.strategygame.model.GameState;
import org.strategygame.save.json.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * ذخیره و بارگذاری فایل. نوشتن اتمیک است: اول فایل موقت، بعد اعتبارسنجی JSON،
 * بعد جایگزینی فایل اصلی. اسلات خودکار هرگز اسلات دستی را بازنویسی نمی‌کند.
 */
public class SaveService {

    private final Path directory;

    public SaveService() {
        this(Path.of(System.getProperty("user.home"), ".strategygame", "saves"));
    }

    public SaveService(Path directory) {
        this.directory = directory;
    }

    public Path directory() { return directory; }

    public void save(GameState state, SaveSlot slot) throws SaveException {
        if (state == null || slot == null) {
            throw new SaveException(SaveException.Kind.IO, "وضعیت بازی برای ذخیره آماده نیست");
        }
        String json = SaveCodec.encode(state);
        try {
            writeAtomic(slot, json);
        } catch (IOException e) {
            throw new SaveException(SaveException.Kind.IO, "نوشتن فایل ذخیره ناموفق بود", e);
        }
    }

    public GameState load(SaveSlot slot) throws SaveException {
        Path file = file(slot);
        if (!Files.isRegularFile(file)) {
            throw new SaveException(SaveException.Kind.IO, slot.label() + " خالی است");
        }
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SaveException(SaveException.Kind.IO, "خواندن فایل ذخیره ناموفق بود", e);
        }
        return SaveCodec.decode(json);
    }

    public List<SaveInfo> list() {
        List<SaveInfo> list = new ArrayList<>();
        for (SaveSlot slot : SaveSlot.values()) list.add(inspect(slot));
        return list;
    }

    public SaveInfo inspect(SaveSlot slot) {
        Path path = file(slot);
        if (!Files.isRegularFile(path)) return SaveInfo.empty(slot);
        try {
            var root = Json.parseObject(Files.readString(path, StandardCharsets.UTF_8));
            int version = root.i("version", -1);
            if (version != SaveCodec.VERSION) {
                return new SaveInfo(slot, true, root.lng("savedAt", 0), root.i("turn", 0),
                        root.str("gameId"), "نسخه " + version);
            }
            return new SaveInfo(slot, true, root.lng("savedAt", 0),
                    root.i("turn", 0), root.str("gameId"), null);
        } catch (Exception e) {
            return new SaveInfo(slot, true, 0, 0, null, "فایل خراب است");
        }
    }

    private void writeAtomic(SaveSlot slot, String json) throws IOException, SaveException {
        Files.createDirectories(directory);
        Path target = file(slot);
        Path tmp = directory.resolve(slot.fileName() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        try {
            Json.parseObject(Files.readString(tmp, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw new SaveException(SaveException.Kind.CORRUPT, "خروجی ذخیره معتبر نبود", e);
        }

        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path file(SaveSlot slot) {
        return directory.resolve(slot.fileName());
    }
}
