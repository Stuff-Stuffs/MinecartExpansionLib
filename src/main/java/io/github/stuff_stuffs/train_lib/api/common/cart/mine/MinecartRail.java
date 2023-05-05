package io.github.stuff_stuffs.train_lib.api.common.cart.mine;

import io.github.stuff_stuffs.train_lib.api.common.cart.Rail;
import net.minecraft.util.math.BlockPos;

public interface MinecartRail extends Rail<MinecartRail> {
    BlockPos entrancePosition();

    BlockPos exitPosition();

    BlockPos railPosition();
}
