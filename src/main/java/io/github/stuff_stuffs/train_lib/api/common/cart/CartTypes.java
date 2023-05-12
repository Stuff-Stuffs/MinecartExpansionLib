package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

public final class CartTypes {
    public static final CartType<MinecartRail, BlockPos> MINECART_CART_TYPE = CartType.of(CartPathfinder.MINECART_PATHFINDER);
    public static final TagKey<CartType<?,?>> ENTITY_RIDEABLE_TAG = TagKey.of(CartType.REGISTRY.getKey(), TrainLib.id("entity_rideable"));
    public static final TagKey<CartType<?,?>> BLOCK_RIDEABLE_TAG = TagKey.of(CartType.REGISTRY.getKey(), TrainLib.id("entity_rideable"));


    public static void init() {
        Registry.register(CartType.REGISTRY, TrainLib.id("minecart"), MINECART_CART_TYPE);
    }

    private CartTypes() {
    }
}
