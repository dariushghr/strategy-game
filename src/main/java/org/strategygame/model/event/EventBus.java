package org.strategygame.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** گذرگاه رویداد درون‌پردازه‌ای. منطق بازی را به UI وصل می‌کند بدون وابستگی دوطرفه. */
public class EventBus {

    private final List<Consumer<Object>> listeners = new ArrayList<>();

    public void subscribe(Consumer<Object> listener) {
        if (listener != null) listeners.add(listener);
    }

    public void publish(Object event) {
        if (event == null) return;
        for (Consumer<Object> listener : List.copyOf(listeners)) {
            listener.accept(event);
        }
    }
}
