package io.github.stuff_stuffs.train_lib.internal.common.event;

import io.github.stuff_stuffs.train_lib.api.common.event.CartEvent;
import io.github.stuff_stuffs.train_lib.api.common.event.CartEventListener;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CartEventListenerDispatcher {
    private final List<BlockEntity> listeners = new ArrayList<>();
    private final Set<BlockEntity> toRemove = new ReferenceOpenHashSet<>();
    private final Set<BlockEntity> toAdd = new ReferenceOpenHashSet<>();
    private int dispatchingDepth = 0;

    public void clear() {
        if (dispatchingDepth != 0) {
            throw new RuntimeException();
        }
        listeners.clear();
        toRemove.clear();
        toAdd.clear();
    }

    public void addBlockEntity(final BlockEntity blockEntity) {
        if (blockEntity instanceof CartEventListener) {
            if (dispatchingDepth != 0) {
                toAdd.add(blockEntity);
            } else {
                listeners.add(blockEntity);
            }
        }
    }

    public void removeBlockEntity(final BlockEntity blockEntity) {
        if (blockEntity instanceof CartEventListener) {
            if (dispatchingDepth != 0) {
                toRemove.add(blockEntity);
            } else {
                listeners.remove(blockEntity);
            }
        }
    }

    public void emit(final CartEvent event) {
        if (dispatchingDepth == 0) {
            listeners.addAll(toAdd);
            toAdd.clear();
        }
        dispatchingDepth++;
        final double sq = event.range() * event.range();
        final Vec3d pos = event.cart().position();
        for (final BlockEntity listener : listeners) {
            final BlockPos listenerPos = listener.getPos();
            if (pos.squaredDistanceTo(listenerPos.getX() + 0.5, listenerPos.getY() + 0.5, listenerPos.getZ() + 0.5) <= sq) {
                ((CartEventListener) listener).listener(event);
            }
        }
        dispatchingDepth--;
        if (dispatchingDepth == 0) {
            listeners.removeAll(toRemove);
            toRemove.clear();
        }
    }
}
