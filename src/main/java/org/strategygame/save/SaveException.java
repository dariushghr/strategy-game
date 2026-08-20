package org.strategygame.save;

/** خطای ذخیره یا بارگذاری که UI باید به بازیکن نشان دهد، نه کرش کند. */
public class SaveException extends Exception {

    public enum Kind { CORRUPT, VERSION, IO, BUSY }

    private final Kind kind;

    public SaveException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public SaveException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() { return kind; }
}
