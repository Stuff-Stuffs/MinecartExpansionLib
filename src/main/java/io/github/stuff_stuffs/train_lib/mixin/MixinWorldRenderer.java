package io.github.stuff_stuffs.train_lib.mixin;

import io.github.stuff_stuffs.train_lib.internal.client.render.FastMountEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void fastEntityHook(final Entity entity, final double cameraX, final double cameraY, final double cameraZ, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final CallbackInfo ci) {
        if (entity.getVehicle() instanceof FastMountEntity fastMount) {
            fastMount.updateFastPassengerPosition(entity, tickDelta);
        }
    }
}
