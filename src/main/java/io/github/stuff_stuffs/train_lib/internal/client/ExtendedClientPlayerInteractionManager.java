package io.github.stuff_stuffs.train_lib.internal.client;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface ExtendedClientPlayerInteractionManager {
    @Nullable Entity getLastInteractedEntity();
}
