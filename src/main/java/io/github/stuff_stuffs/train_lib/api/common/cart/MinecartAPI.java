package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;

public final class MinecartAPI {
    public static final BlockApiLookup<MinecartRailProvider, Void> MINECART_RAIL_BLOCK_API = BlockApiLookup.get(TrainLib.id("rail"), MinecartRailProvider.class, Void.class);

    private MinecartAPI() {
    }
}
