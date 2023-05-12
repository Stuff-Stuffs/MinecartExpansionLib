package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Predicate;

public interface CartType<T extends Rail<T>,P> {
    Registry<CartType<?,?>> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<CartType<?,?>>ofRegistry(TrainLib.id("cart_types"))).buildAndRegister();

    CartPathfinder<T,P> pathfinder();

    static Predicate<CartType<?,?>> tagPredicate(TagKey<CartType<?,?>> tag) {
        return type -> REGISTRY.getEntry(type).isIn(tag);
    }

    static <T extends Rail<T>,P> CartType<T,P> of(CartPathfinder<T,P> pathfinder) {
        return new CartType<T, P>() {
            @Override
            public CartPathfinder<T, P> pathfinder() {
                return pathfinder;
            }
        };
    }
}
