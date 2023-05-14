package io.github.stuff_stuffs.train_lib.internal.common.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;

@Modmenu(modId = "train_lib")
@Config(name = "train-lib-config", wrapperName = "TrainLibConfig")
public class TrainLibConfigModel {
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    @RangeConstraint(min = 1, max = 64)
    public int maxRecursion = 8;
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    @RangeConstraint(min = 0.01, max = 16)
    public int maxSpeed = 4;
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    @RangeConstraint(min = 1, max = 32)
    public int maxTrainSize = 8;
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    @RangeConstraint(min = 0, max = 1)
    public double gravity = 0.04;
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    @RangeConstraint(min = 1, max = 16)
    public int maxPathfindingLimit = 10;
    @Sync(Option.SyncMode.NONE)
    @RangeConstraint(min = 0.0, max = 1.0)
    public double maxMinecartVolume = 0.5;
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

    public enum EntityCollisionOption {
        DEFAULT,
        IGNORE,
        DAMAGE,
        PUSH
    }
}
