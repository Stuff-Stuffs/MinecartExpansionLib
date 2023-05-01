package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface MinecartView extends MinecartDataView {
    Vec3d velocity();

    @Override
    @Nullable MinecartView attached();

    @Override
    @Nullable MinecartView attachment();
}
