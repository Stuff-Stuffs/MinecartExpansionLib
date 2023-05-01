package io.github.stuff_stuffs.train_lib.mixin;

import io.github.stuff_stuffs.train_lib.api.common.entity.PreviousInteractionAwareEntity;
import io.github.stuff_stuffs.train_lib.internal.client.ExtendedClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements ExtendedClientPlayerInteractionManager {
    @Shadow
    public abstract GameMode getCurrentGameMode();

    @Unique
    private @Nullable WeakReference<Entity> lastInteracted = null;

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void callHook(final PlayerEntity player, final Entity entity, final Hand hand, final CallbackInfoReturnable<ActionResult> cir) {
        @Nullable final Entity previous = getLastInteractedEntity();
        if (previous != null && entity instanceof PreviousInteractionAwareEntity aware && entity != previous) {
            final ActionResult result = aware.previousInteractionAwareInteract(player, hand, previous);
            if (result != ActionResult.PASS) {
                cir.setReturnValue(result);
                lastInteracted = null;
            }
        }
    }

    @Inject(method = "interactEntity", at = @At("RETURN"))
    private void hook(final PlayerEntity player, final Entity entity, final Hand hand, final CallbackInfoReturnable<ActionResult> cir) {
        if (getCurrentGameMode() != GameMode.SPECTATOR && !cir.getReturnValue().isAccepted()) {
            lastInteracted = new WeakReference<>(entity);
        } else {
            lastInteracted = null;
        }
    }

    @Override
    public @Nullable Entity getLastInteractedEntity() {
        return lastInteracted == null ? null : lastInteracted.get();
    }
}
