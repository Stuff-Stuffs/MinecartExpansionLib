package io.github.stuff_stuffs.train_lib.api.client.cargo;

import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.CargoType;
import io.github.stuff_stuffs.train_lib.impl.client.cargo.CargoRenderingRegistryImpl;
import org.jetbrains.annotations.Nullable;

public interface CargoRenderingRegistry {
    <T extends Cargo> void register(CargoType<T> type, CargoRenderer<? super T> renderer);

    <T extends Cargo> @Nullable CargoRenderer<? super T> get(CargoType<T> type);

    static CargoRenderingRegistry getInstance() {
        return CargoRenderingRegistryImpl.get();
    }
}
