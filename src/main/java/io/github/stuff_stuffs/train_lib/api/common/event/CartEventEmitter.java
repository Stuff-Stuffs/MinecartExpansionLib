package io.github.stuff_stuffs.train_lib.api.common.event;

import io.github.stuff_stuffs.train_lib.impl.common.CartEventEmitterImpl;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface CartEventEmitter {
    void setPos(Vec3d position);

    void emit(CartEvent event);

    static CartEventEmitter create(final World world) {
        return new CartEventEmitterImpl(world);
    }
}
