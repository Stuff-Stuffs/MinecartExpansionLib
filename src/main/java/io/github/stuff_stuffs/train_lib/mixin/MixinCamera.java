package io.github.stuff_stuffs.train_lib.mixin;

import io.github.stuff_stuffs.train_lib.internal.client.render.FastMountEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER))
    private void fastEntityHook(final BlockView area, final Entity focusedEntity, final boolean thirdPerson, final boolean inverseView, final float tickDelta, final CallbackInfo ci) {
        if (focusedEntity instanceof FastMountEntity fastMount) {
            final Vec3d position = fastMount.fastPosition(tickDelta);
            setPos(position.x, position.y, position.z);
        } else if (focusedEntity.getVehicle() instanceof FastMountEntity fastMount) {
            fastMount.updateFastPassengerPosition(focusedEntity, tickDelta);
            setPos(focusedEntity.getX(), focusedEntity.getY(), focusedEntity.getZ());
        }
    }
}
