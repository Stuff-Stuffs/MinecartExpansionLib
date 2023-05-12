package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.train_lib.api.common.cart.CartType;
import io.github.stuff_stuffs.train_lib.api.common.cart.CartTypes;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Predicate;

public final class CargoType<T extends Cargo> {
    public static final Registry<CargoType<?>> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<CargoType<?>>ofRegistry(TrainLib.id("cargo_types"))).buildAndRegister();
    public static final CargoType<BlockCargo> BLOCK_CARGO_TYPE = new CargoType<>(BlockCargo.CODEC, CartType.tagPredicate(CartTypes.BLOCK_RIDEABLE_TAG));
    public static final CargoType<EntityCargo> ENTITY_CARGO_TYPE = new CargoType<>(EntityCargo.CODEC, CartType.tagPredicate(CartTypes.ENTITY_RIDEABLE_TAG));
    private final Codec<T> codec;
    private final Predicate<CartType<?,?>> typePredicate;

    public CargoType(final Codec<T> codec, Predicate<CartType<?, ?>> predicate) {
        this.codec = codec;
        typePredicate = predicate;
    }

    public boolean check(CartType<?,?> type) {
        return typePredicate.test(type);
    }

    public Codec<T> codec() {
        return codec;
    }

    public static void init() {
        Registry.register(REGISTRY, TrainLib.id("block"), BLOCK_CARGO_TYPE);
        Registry.register(REGISTRY, TrainLib.id("entity"), ENTITY_CARGO_TYPE);
    }
}
