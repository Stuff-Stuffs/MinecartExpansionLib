package io.github.stuff_stuffs.train_lib.internal.common.util;

import io.github.stuff_stuffs.train_lib.impl.common.AbstractCartImpl;
import io.github.stuff_stuffs.train_lib.internal.common.entity.AbstractCartEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class TrainTrackingUtil {

    private TrainTrackingUtil() {
    }

    public static boolean shouldSend(final AbstractCartEntity current, final ServerPlayerEntity player) {
        final List<? extends AbstractCartImpl<?, ?>> cars = current.cart().cars();
        for (final AbstractCartImpl<?, ?> car : cars) {
            if (car.holder() != current) {
                if (!PlayerLookup.tracking(car.holder()).contains(player)) {
                    return false;
                }
            }
        }
        return true;
    }
}
