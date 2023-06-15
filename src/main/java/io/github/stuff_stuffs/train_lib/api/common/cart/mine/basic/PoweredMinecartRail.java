package io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic;

import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PoweredMinecartRail extends SimpleMinecartRail {
    private final double accelerationFactor;

    public PoweredMinecartRail(final Vec3d start, final Vec3d end, final int id, final BlockPos railPosition, final BlockPos entrance, final BlockPos exit, final Direction entranceDirection, final Direction exitDirection, final double accelerationFactor) {
        super(start, end, id, railPosition, entrance, exit, entranceDirection, exitDirection);
        this.accelerationFactor = accelerationFactor;
    }

    @Override
    public void onRail(final Cart minecart, final double startProgress, final double endProgress, final double time) {
        super.onRail(minecart, startProgress, endProgress, time);
        final double speed = minecart.speed();
        final double absolute = Math.abs(speed);
        final double maxSpeed = minecart.type().maxSpeed();
        if (absolute < maxSpeed && absolute != 0) {
            final double s = Math.signum(speed) * (maxSpeed * accelerationFunction(time * accelerationFactor, absolute / maxSpeed));
            minecart.addSpeed(s);
        }
    }

    protected double accelerationFunction(final double duration, final double entranceSpeed) {
        final double st = duration + entranceSpeed;
        final double st2 = st * st;
        final double st3 = st2 * st;
        return -2 * Math.min(1, st3) + 3 * Math.min(1, st2) + (entranceSpeed * entranceSpeed) * (2 * entranceSpeed - 3);
    }
}
