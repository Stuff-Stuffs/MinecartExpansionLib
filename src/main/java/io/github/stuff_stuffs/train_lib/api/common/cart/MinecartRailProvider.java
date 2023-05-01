package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface MinecartRailProvider {
    @Nullable NextRailInfo next(MinecartDataView view, MinecartRail current, @Nullable Direction approachDirection);

    @Nullable MinecartRail currentRail(MinecartDataView view);

    @Nullable NextRailInfo snap(MinecartView view);

    record NextRailInfo(MinecartRail rail, double progress, boolean forwards) {
    }
}
