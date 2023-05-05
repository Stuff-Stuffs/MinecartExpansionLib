package io.github.stuff_stuffs.train_lib.internal.common.item;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class TrainLibItems {
    public static final MinecartItem FAST_MINECART_ITEM = new MinecartItem();

    public static void init() {
        Registry.register(Registries.ITEM, TrainLib.id("fast_minecart"), FAST_MINECART_ITEM);
    }

    private TrainLibItems() {
    }
}
