package org.strategygame.common;

/** نتیجه‌ی یک اکشن بازیکن به همراه پیامی که باید به او نشان داده شود. */
public record ActionResult(boolean success, String message) {

    public static ActionResult ok(String message)   { return new ActionResult(true, message); }
    public static ActionResult fail(String message) { return new ActionResult(false, message); }
}
