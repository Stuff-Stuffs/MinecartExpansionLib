package io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic;

import io.github.stuff_stuffs.train_lib.api.common.cart.CartDataView;
import io.github.stuff_stuffs.train_lib.api.common.cart.CartView;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRailProvider;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class DelegatingMinecartRailProvider implements MinecartRailProvider {
    protected final MinecartRailProvider delegate;

    public DelegatingMinecartRailProvider(final MinecartRailProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public @Nullable NextRailInfo<MinecartRail> next(final CartDataView view, final MinecartRail current, @Nullable final Direction approachDirection) {
        final NextRailInfo<MinecartRail> next = delegate.next(view, current, approachDirection);
        if (next != null) {
            return new NextRailInfo<>(wrap(next.rail()), next.progress(), next.forwards());
        }
        return null;
    }

    @Override
    public @Nullable MinecartRail currentRail(final CartDataView view) {
        final MinecartRail rail = delegate.currentRail(view);
        if (rail != null) {
            return wrap(rail);
        }
        return null;
    }

    @Override
    public @Nullable NextRailInfo<MinecartRail> snap(final CartView view) {
        final NextRailInfo<MinecartRail> snap = delegate.snap(view);
        if (snap != null) {
            return new NextRailInfo<>(wrap(snap.rail()), snap.progress(), snap.forwards());
        }
        return null;
    }

    protected MinecartRail wrap(final MinecartRail rail) {
        return rail;
    }
}
