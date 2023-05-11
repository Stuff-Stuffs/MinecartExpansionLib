package io.github.stuff_stuffs.train_lib.internal.common.event;

import io.github.stuff_stuffs.train_lib.api.common.event.CartEvent;

public interface CartEventListenerAwareChunk {
    void cart_lib$emit(CartEvent event);
}
