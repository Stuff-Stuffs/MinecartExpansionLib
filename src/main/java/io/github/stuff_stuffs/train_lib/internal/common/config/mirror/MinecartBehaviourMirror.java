package io.github.stuff_stuffs.train_lib.internal.common.config.mirror;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfig;

public class MinecartBehaviourMirror {
    private int maxTrainSize;
    private int maxPathfindingLimit;
    private int maxRecursion;
    private double maxSpeed;

    public MinecartBehaviourMirror() {
        final TrainLibConfig.Behaviour behaviour = TrainLib.CONFIG.behaviour;
        maxTrainSize = behaviour.maxTrainSize();
        maxPathfindingLimit = behaviour.maxPathfindingLimit();
        maxRecursion = behaviour.maxRecursion();
        maxSpeed = behaviour.maxSpeed();
        behaviour.subscribeToMaxTrainSize(i -> maxTrainSize = i);
        behaviour.subscribeToMaxPathfindingLimit(i -> maxPathfindingLimit = i);
        behaviour.subscribeToMaxRecursion(i -> maxRecursion = i);
        behaviour.subscribeToMaxSpeed(d -> maxSpeed = d);
    }

    public int maxTrainSize() {
        return maxTrainSize;
    }

    public int maxPathfindingLimit() {
        return maxPathfindingLimit;
    }

    public int maxRecursion() {
        return maxRecursion;
    }

    public double maxSpeed() {
        return maxSpeed;
    }
}
