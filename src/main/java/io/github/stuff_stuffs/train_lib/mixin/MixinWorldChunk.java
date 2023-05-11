package io.github.stuff_stuffs.train_lib.mixin;

import io.github.stuff_stuffs.train_lib.api.common.event.CartEvent;
import io.github.stuff_stuffs.train_lib.internal.common.event.CartEventListenerAwareChunk;
import io.github.stuff_stuffs.train_lib.internal.common.event.CartEventListenerDispatcher;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk implements CartEventListenerAwareChunk {
    @Shadow
    public abstract Map<BlockPos, BlockEntity> getBlockEntities();

    @Shadow
    public abstract @Nullable BlockEntity getBlockEntity(BlockPos pos);

    @Shadow
    protected abstract boolean canTickBlockEntities();

    private final CartEventListenerDispatcher dispatcher = new CartEventListenerDispatcher();

    @Inject(method = "updateAllBlockEntities", at = @At("RETURN"))
    private void updateHook(final CallbackInfo ci) {
        dispatcher.clear();
        for (final BlockEntity blockEntity : getBlockEntities().values()) {
            dispatcher.addBlockEntity(blockEntity);
        }
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void clearHook(final CallbackInfo ci) {
        dispatcher.clear();
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void setHook(final BlockEntity blockEntity, final CallbackInfo ci) {
        final BlockEntity entity = getBlockEntity(blockEntity.getPos());
        if (entity != null) {
            dispatcher.removeBlockEntity(entity);
        }
        dispatcher.addBlockEntity(blockEntity);
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void removeHook(final BlockPos pos, final CallbackInfo ci) {
        if (canTickBlockEntities()) {
            final BlockEntity entity = getBlockEntity(pos);
            if (entity != null) {
                dispatcher.removeBlockEntity(entity);
            }
        }
    }

    @Inject(method = "addBlockEntity", at = @At("HEAD"))
    private void addHook(final BlockEntity blockEntity, final CallbackInfo ci) {
        if (canTickBlockEntities()) {
            dispatcher.addBlockEntity(blockEntity);
        }
    }

    @Override
    public void cart_lib$emit(final CartEvent event) {
        dispatcher.emit(event);
    }
}
