package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface Rail<T extends Rail<T>> {
    Vec3d DEFAULT_UP = new Vec3d(0, 1, 0);

    int id();

    Vec3d position(double progress);

    Vec3d tangent(double progress);

    @Nullable Direction entranceDirection();

    @Nullable Direction exitDirection();

    double slopeAngle();

    double length();

    default double friction(final CartView minecart, final double progress) {
        //default minecart slowdown
        final double g = 0.0078125;
        final double abs = Math.abs(g / minecart.speed());
        if (abs > 1) {
            return 1;
        }
        return abs;
    }

    default void onRail(final Cart minecart, final double startProgress, final double endProgress, final double time) {
    }

    default void onEnter(final Cart minecart) {
    }

    default void onExit(final Cart minecart) {
    }
}
