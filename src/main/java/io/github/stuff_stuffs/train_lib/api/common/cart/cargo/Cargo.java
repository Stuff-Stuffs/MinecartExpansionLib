package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;

public interface Cargo {
    Codec<Cargo> CODEC = CargoType.REGISTRY.getCodec().dispatchStable(Cargo::type, CargoType::codec);

    CargoType<?> type();

    double mass();
}
