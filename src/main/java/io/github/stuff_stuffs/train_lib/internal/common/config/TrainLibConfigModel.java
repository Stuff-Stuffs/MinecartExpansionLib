package io.github.stuff_stuffs.train_lib.internal.common.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;

@Modmenu(modId = "train_lib")
@Config(name = "train-lib-config", wrapperName = "TrainLibConfig")
public class TrainLibConfigModel {
    @Sync(Option.SyncMode.NONE)
    @RangeConstraint(min = 0.0, max = 1.0)
    public double maxMinecartVolume = 0.5;
    @Nest
    public Physics physics = new Physics();
    @Nest
    public MinecartBehaviour behaviour = new MinecartBehaviour();
    @Nest
    public EntityCollisionOptions entityCollisionOptions = new EntityCollisionOptions();

    public static class EntityCollisionOptions {
        @PredicateConstraint("restrictDefault")
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption root = EntityCollisionOption.DAMAGE;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption living = EntityCollisionOption.DEFAULT;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption player = EntityCollisionOption.DEFAULT;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption passive = EntityCollisionOption.DEFAULT;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption nonLiving = EntityCollisionOption.DEFAULT;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption npc = EntityCollisionOption.DEFAULT;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption hostile = EntityCollisionOption.DEFAULT;
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public EntityCollisionOption wither = EntityCollisionOption.IGNORE;


        public static boolean restrictDefault(final EntityCollisionOption option) {
            return option != EntityCollisionOption.DEFAULT;
        }
    }

    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public static final class Physics {
        @RangeConstraint(min = 0, max = 1)
        @Hook
        public double gravity = 0.04;
        @RangeConstraint(min = 0, max = 1, decimalPlaces = 6)
        @Hook
        public double wheelFriction10 = 0.078125;
    }

    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public static final class MinecartBehaviour {
        @RangeConstraint(min = 1, max = 32)
        @Hook
        public int maxTrainSize = 8;
        @RangeConstraint(min = 1, max = 16)
        @Hook
        public int maxPathfindingLimit = 10;
        @RangeConstraint(min = 1, max = 64)
        @Hook
        public int maxRecursion = 8;
        @RangeConstraint(min = 0.01, max = 8)
        @Hook
        public double maxSpeed = 4;
    }

    public enum EntityCollisionOption {
        DEFAULT,
        IGNORE,
        DAMAGE,
        PUSH
    }
}
