package io.github.stuff_stuffs.train_lib.impl.common;

import io.github.stuff_stuffs.train_lib.api.common.event.CartEvent;
import io.github.stuff_stuffs.train_lib.api.common.event.CartEventEmitter;
import io.github.stuff_stuffs.train_lib.internal.common.event.CartEventListenerAwareChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class CartEventEmitterImpl implements CartEventEmitter {
    private static final int NEEDED_CHUNKS = (int) Math.ceil(CartEvent.MAX_RANGE / 16);
    private static final int[][] OFFSETS;
    private final @Nullable WeakReference<WorldChunk>[] chunks;
    private final World world;
    private ChunkPos pos;

    public CartEventEmitterImpl(final World world) {
        this.world = world;
        chunks = new WeakReference[(NEEDED_CHUNKS * 2 + 1) * (NEEDED_CHUNKS * 2 + 1)];
        pos = new ChunkPos(0, 0);
    }

    @Override
    public void setPos(final Vec3d position) {
        final ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(position));
        if (!chunkPos.equals(pos)) {
            Arrays.fill(chunks, null);
            pos = chunkPos;
        }
    }

    @Override
    public void emit(final CartEvent event) {
        for (int i = 0; i < chunks.length; i++) {
            WeakReference<WorldChunk> chunk = chunks[i];
            if (chunk == null) {
                final int[] offset = OFFSETS[i];
                final WorldChunk worldChunk = (WorldChunk) world.getChunk(pos.x + offset[0], pos.z + offset[1], ChunkStatus.FULL, false);
                if (worldChunk != null) {
                    chunks[i] = chunk = new WeakReference<>(worldChunk);
                }
            }
            if (chunk != null) {
                final WorldChunk worldChunk = chunk.get();
                if (worldChunk != null) {
                    ((CartEventListenerAwareChunk) worldChunk).cart_lib$emit(event);
                } else {
                    chunks[i] = null;
                }
            }
        }
    }

    static {
        final int size = NEEDED_CHUNKS * 2 + 1;
        OFFSETS = new int[size * size][2];
        for (int i = 0; i < OFFSETS.length; i++) {
            OFFSETS[i] = new int[]{i / size - NEEDED_CHUNKS, (i % size) - NEEDED_CHUNKS};
        }
    }
}
