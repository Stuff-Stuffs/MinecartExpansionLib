package io.github.stuff_stuffs.train_lib.impl.client.cargo;

import io.github.stuff_stuffs.train_lib.api.client.cargo.CargoRenderer;
import io.github.stuff_stuffs.train_lib.api.client.cargo.CargoRenderingRegistry;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.CargoType;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CargoRenderingRegistryImpl implements CargoRenderingRegistry {
    private static final CargoRenderingRegistryImpl INSTANCE = new CargoRenderingRegistryImpl();
    private final Map<CargoType<?>, CargoRenderer<?>> renderers = new Reference2ReferenceOpenHashMap<>();

    private CargoRenderingRegistryImpl() {
    }

    @Override
    public <T extends Cargo> void register(final CargoType<T> type, final CargoRenderer<? super T> renderer) {
        renderers.put(type, renderer);
    }

    @Override
    public @Nullable <T extends Cargo> CargoRenderer<? super T> get(final CargoType<T> type) {
        return (CargoRenderer<? super T>) renderers.get(type);
    }

    public static CargoRenderingRegistry get() {
        return INSTANCE;
    }
}
