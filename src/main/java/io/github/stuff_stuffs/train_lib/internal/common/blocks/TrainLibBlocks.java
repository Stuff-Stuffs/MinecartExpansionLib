package io.github.stuff_stuffs.train_lib.internal.common.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;

public final class TrainLibBlocks {
    public static final StraightTrainRailBlock STRAIGHT_TRAIN_RAIL = new StraightTrainRailBlock(FabricBlockSettings.copyOf(Blocks.RAIL));
    public static void init() {
    }

    private TrainLibBlocks() {
    }
}
