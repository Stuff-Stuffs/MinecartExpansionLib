package io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic;

import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class SimpleMinecartRail implements MinecartRail {
    private final int id;
    private final Vec3d start;
    private final Vec3d deltaNorm;
    private final double slope;
    private final double length;
    private final BlockPos railPosition;
    private final BlockPos entrance;
    private final BlockPos exit;
    private final @Nullable Direction entranceDirection;
    private final @Nullable Direction exitDirection;

    public SimpleMinecartRail(final Vec3d start, final Vec3d end, final int id, final BlockPos railPosition, final BlockPos entrance, final BlockPos exit, final @Nullable Direction entranceDirection, final @Nullable Direction exitDirection) {
        this.start = start;
        this.id = id;
        this.railPosition = railPosition;
        this.entrance = entrance;
        this.entranceDirection = entranceDirection;
        this.exitDirection = exitDirection;
        final Vec3d delta = end.subtract(start);
        deltaNorm = delta.normalize();
        slope = Math.asin(deltaNorm.y);
        length = delta.length();
        this.exit = exit;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Vec3d position(final double progress) {
        return start.add(deltaNorm.multiply(progress));
    }

    @Override
    public BlockPos entrancePosition() {
        return entrance;
    }

    @Override
    public BlockPos exitPosition() {
        return exit;
    }

    @Override
    public BlockPos railPosition() {
        return railPosition;
    }

    @Override
    public Vec3d tangent(final double progress) {
        return deltaNorm;
    }

    @Override
    public @Nullable Direction entranceDirection() {
        return entranceDirection;
    }

    @Override
    public @Nullable Direction exitDirection() {
        return exitDirection;
    }

    @Override
    public double slopeAngle() {
        return slope;
    }

    @Override
    public double length() {
        return length;
    }
}
