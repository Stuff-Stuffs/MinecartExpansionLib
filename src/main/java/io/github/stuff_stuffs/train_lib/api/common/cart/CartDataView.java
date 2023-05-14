package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface CartDataView {
    CartType<?, ?> type();

    @Nullable Cargo cargo();

    double speed();

    Vec3d position();

    double progress();

    @Nullable CartDataView attached();

    @Nullable CartDataView attachment();

    double mass();

    double bufferSpace();

    Entity holder();
}
