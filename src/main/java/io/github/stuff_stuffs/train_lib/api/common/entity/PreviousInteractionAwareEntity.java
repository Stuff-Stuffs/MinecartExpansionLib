package io.github.stuff_stuffs.train_lib.api.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public interface PreviousInteractionAwareEntity {
    ActionResult previousInteractionAwareInteract(PlayerEntity player, Hand hand, Entity previousInteraction);
}
