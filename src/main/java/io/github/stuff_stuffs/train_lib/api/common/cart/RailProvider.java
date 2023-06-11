package io.github.stuff_stuffs.train_lib.api.common.cart;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface RailProvider<T extends Rail<T>> {
    @Nullable NextRailInfo<T> next(CartDataView view, T current, @Nullable Direction approachDirection);

    @Nullable T currentRail(CartDataView view);

    @Nullable NextRailInfo<T> snap(CartView view);

    IntSet ids();

    T fromId(int id);

    IntSet intersecting(int id);

    @Nullable RailReflectionInfo<T> nextReflect(T current, @Nullable Direction approachDirection);

    @Nullable RailReflectionInfo<T> snapReflect(@Nullable Direction approachDirection);

    record NextRailInfo<T extends Rail<T>>(T rail, double progress, boolean forwards) {
    }

    record RailReflectionInfo<T extends Rail<T>>(T rail, boolean forwards) {
    }
}
