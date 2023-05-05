package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface CartView extends CartDataView {
    Vec3d velocity();

    @Override
    @Nullable CartView attached();

    @Override
    @Nullable CartView attachment();
}
