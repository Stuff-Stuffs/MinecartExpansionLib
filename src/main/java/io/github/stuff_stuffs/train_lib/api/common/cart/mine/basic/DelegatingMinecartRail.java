package io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic;

import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import io.github.stuff_stuffs.train_lib.api.common.cart.CartView;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfigModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class DelegatingMinecartRail implements MinecartRail {
    protected final MinecartRail delegate;

    public DelegatingMinecartRail(final MinecartRail delegate) {
        this.delegate = delegate;
    }

    public MinecartRail delegate() {
        return delegate;
    }

    @Override
    public int id() {
        return delegate.id();
    }

    @Override
    public Vec3d position(final double progress) {
        return delegate.position(progress);
    }

    @Override
    public BlockPos entrancePosition() {
        return delegate.entrancePosition();
    }

    @Override
    public BlockPos exitPosition() {
        return delegate.exitPosition();
    }

    @Override
    public BlockPos railPosition() {
        return delegate.railPosition();
    }

    @Override
    public Vec3d tangent(final double progress) {
        return delegate.tangent(progress);
    }

    @Override
    public @Nullable Direction entranceDirection() {
        return delegate.entranceDirection();
    }

    @Override
    public @Nullable Direction exitDirection() {
        return delegate.exitDirection();
    }

    @Override
    public double slopeAngle() {
        return delegate.slopeAngle();
    }

    @Override
    public double length() {
        return delegate.length();
    }

    @Override
    public double friction(final CartView minecart, final double progress) {
        return delegate.friction(minecart, progress);
    }

    @Override
    public void onRail(final Cart minecart, final double startProgress, final double endProgress, final double time) {
        delegate.onRail(minecart, startProgress, endProgress, time);
    }
}
