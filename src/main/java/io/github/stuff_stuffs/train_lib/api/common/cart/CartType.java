package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

public interface CartType<T extends Rail<T>, P> {
    Registry<CartType<?, ?>> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<CartType<?, ?>>ofRegistry(TrainLib.id("cart_types"))).buildAndRegister();

    CartPathfinder<T, P> pathfinder();

    int maxTrainSize();

    int maxRecursion();

    double maxSpeed();

    static Predicate<CartType<?, ?>> tagPredicate(final TagKey<CartType<?, ?>> tag) {
        return type -> REGISTRY.getEntry(type).isIn(tag);
    }

    static <T extends Rail<T>, P> CartType<T, P> of(final CartPathfinder<T, P> pathfinder, final IntSupplier maxTrainSize, final IntSupplier maxRecursion, final DoubleSupplier maxSpeed) {
        return new CartType<>() {
            @Override
            public CartPathfinder<T, P> pathfinder() {
                return pathfinder;
            }

            @Override
            public int maxTrainSize() {
                return maxTrainSize.getAsInt();
            }

            @Override
            public int maxRecursion() {
                return maxRecursion.getAsInt();
            }

            @Override
            public double maxSpeed() {
                return maxSpeed.getAsDouble();
            }
        };
    }
}
