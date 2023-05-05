package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface RailProvider<T extends Rail<T>> {
    @Nullable NextRailInfo<T> next(CartDataView view, T current, @Nullable Direction approachDirection);

    @Nullable T currentRail(CartDataView view);

    @Nullable NextRailInfo<T> snap(CartView view);

    record NextRailInfo<T extends Rail<T>>(T rail, double progress, boolean forwards) {
    }
}
