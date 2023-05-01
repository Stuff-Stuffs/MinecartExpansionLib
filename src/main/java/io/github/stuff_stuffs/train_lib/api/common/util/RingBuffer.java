package io.github.stuff_stuffs.train_lib.api.common.util;

import org.jetbrains.annotations.Nullable;

public class RingBuffer<T> {
    private final Object[] slots;
    private final int capacity;
    private int size = 0;
    private int offset = 0;

    public RingBuffer(final int capacity) {
        slots = new Object[capacity];
        this.capacity = capacity;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    public T get(final int index) {
        if (size == 0) {
            throw new IndexOutOfBoundsException();
        }
        return (T) slots[(index + offset) % Math.min(capacity, size)];
    }

    public @Nullable T append(final T value) {
        final int idx = offset;
        final T prev = (T) slots[idx];
        slots[idx] = value;
        offset = offset + 1;
        size = Math.max(size, offset);
        if (offset == capacity) {
            offset = 0;
        }
        return prev;
    }
}
