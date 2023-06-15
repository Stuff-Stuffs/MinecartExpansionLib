package io.github.stuff_stuffs.train_lib.internal.common.config.mirror;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfig;

public class PhysicsMirror {
    private double gravity;
    private double wheelFriction10;

    public PhysicsMirror() {
        final TrainLibConfig.Physics_ physics = TrainLib.CONFIG.physics;
        gravity = physics.gravity();
        wheelFriction10 = physics.wheelFriction10();
        physics.subscribeToGravity(d -> gravity = d);
        physics.subscribeToWheelFriction10(d -> wheelFriction10 = d);
    }

    public double wheelFriction10() {
        return wheelFriction10;
    }

    public double gravity() {
        return gravity;
    }
}
