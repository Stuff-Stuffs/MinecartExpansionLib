package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface MinecartDataView {
    @Nullable Cargo cargo();

    double speed();

    Vec3d position();

    double progress();

    @Nullable MinecartDataView attached();

    @Nullable MinecartDataView attachment();

    double mass();

    double massBehind();

    double massAhead();

    boolean forwardsAligned();
}
