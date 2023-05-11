package io.github.stuff_stuffs.train_lib.internal.common.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.Sync;

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
}
