package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public final class CargoType<T extends Cargo> {
    public static final Registry<CargoType<?>> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<CargoType<?>>ofRegistry(TrainLib.id("cargo_types"))).buildAndRegister();
    public static final CargoType<BlockCargo> BLOCK_CARGO_TYPE = new CargoType<>(BlockCargo.CODEC);
    private final Codec<T> codec;

    public CargoType(final Codec<T> codec) {
        this.codec = codec;
    }

    public Codec<T> codec() {
        return codec;
    }

    public static void init() {
        Registry.register(REGISTRY, TrainLib.id("block"), BLOCK_CARGO_TYPE);
    }
}
