package io.github.stuff_stuffs.train_lib.internal.common.entity;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class MinecartMovementTracker {
    private final Supplier<Entry> fallback;
    private final List<Entry> entries = new ArrayList<>();

    public MinecartMovementTracker(final Supplier<Entry> fallback) {
        this.fallback = fallback;
    }

    public void addEntry(final double time, final Vec3d position, final Vec3d tangent, final Vec3d up) {
        entries.add(new Entry((float) time, position, tangent, up));
    }

    public void reset() {
        if (!entries.isEmpty()) {
            final Entry last = entries.get(entries.size() - 1);
            entries.clear();
            entries.add(new Entry(0, last.position, last.forward, last.up));
        }
    }

    public List<Vec3d> positions() {
        final List<Vec3d> vectors = new ArrayList<>(entries.size());
        for (final Entry entry : entries) {
            vectors.add(entry.position);
        }
        return vectors;
    }

    public Entry at(final float time) {
        if (entries.isEmpty()) {
            return fallback.get();
        }
        final int i = Collections.binarySearch(entries, new Entry(time, null, null, null));
        if (i >= 0) {
            return entries.get(i);
        } else {
            final int realIndex = Math.max(-(i + 2), 0);
            if (realIndex >= entries.size()) {
                return entries.get(entries.size() - 1);
            } else {
                return entries.get(realIndex);
            }
        }
    }

    public Entry next(final float time) {
        if (entries.isEmpty()) {
            return fallback.get();
        }
        final int i = Collections.binarySearch(entries, new Entry(time, null, null, null));
        if (i + 1 == entries.size()) {
            return entries.get(i);
        } else if (i >= 0) {
            return entries.get(i + 1);
        } else {
            final int realIndex = -(i + 1);
            if (realIndex == entries.size()) {
                return entries.get(entries.size() - 1);
            } else {
                return entries.get(realIndex);
            }
        }
    }

    public record Entry(float time, Vec3d position, Vec3d forward, Vec3d up) implements Comparable<Entry> {
        @Override
        public int compareTo(final MinecartMovementTracker.Entry o) {
            return Float.compare(time, o.time);
        }
    }
}
