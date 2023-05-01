package io.github.stuff_stuffs.train_lib.internal.client.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public interface FastMountEntity {
    void updateFastPassengerPosition(Entity entity, float tickDelta);

    Vec3d fastPosition(float tickDelta);
}
