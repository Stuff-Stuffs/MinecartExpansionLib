package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class DelegatingMinecartRailProvider implements MinecartRailProvider {
    protected final MinecartRailProvider delegate;

    public DelegatingMinecartRailProvider(final MinecartRailProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public @Nullable NextRailInfo next(final MinecartDataView view, final MinecartRail current, @Nullable final Direction approachDirection) {
        final NextRailInfo next = delegate.next(view, current, approachDirection);
        if (next != null) {
            return new NextRailInfo(wrap(next.rail()), next.progress(), next.forwards());
        }
        return null;
    }

    @Override
    public @Nullable MinecartRail currentRail(final MinecartDataView view) {
        final MinecartRail rail = delegate.currentRail(view);
        if (rail != null) {
            return wrap(rail);
        }
        return null;
    }

    @Override
    public @Nullable NextRailInfo snap(final MinecartView view) {
        final NextRailInfo snap = delegate.snap(view);
        if (snap != null) {
            return new NextRailInfo(wrap(snap.rail()), snap.progress(), snap.forwards());
        }
        return null;
    }

    protected MinecartRail wrap(final MinecartRail rail) {
        return rail;
    }
}
