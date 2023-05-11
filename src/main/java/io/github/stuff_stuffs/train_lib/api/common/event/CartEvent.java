package io.github.stuff_stuffs.train_lib.api.common.event;

import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import net.minecraft.util.math.BlockPos;

public interface CartEvent {
    double MAX_RANGE = 32;

    Cart cart();

    double range();

    record RailOccupied(BlockPos pos, int railId, Cart cart) implements CartEvent {
        @Override
        public double range() {
            return MAX_RANGE;
        }
    }
}
