package io.github.stuff_stuffs.train_lib.internal.client;

import io.github.stuff_stuffs.train_lib.api.client.cargo.CargoRenderingRegistry;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.CargoType;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.internal.client.render.entity.MinecartCartEntityRenderer;
import io.github.stuff_stuffs.train_lib.internal.client.sound.TrainLibMinecartSoundInstance;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.entity.TrainLibEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainLibClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(TrainLib.MOD_ID + "[client]");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MinecartCartEntityRenderer.WHEEL_LAYER, MinecartCartEntityRenderer::createModelData);
        EntityRendererRegistry.register(TrainLibEntities.MINECART_CART_ENTITY_TYPE, ctx -> new MinecartCartEntityRenderer<>(ctx, 1));
        CargoRenderingRegistry.getInstance().register(CargoType.BLOCK_CARGO_TYPE, (cargo, view, tickDelta, matrices, vertexConsumers, light) -> {
            final BlockState state = cargo.blockState();
            matrices.push();
            matrices.translate(1 / 16.0, 1 / 16.0, 1 / 16.0);
            matrices.scale(14 / 16.0F, 14 / 16.0F, 14 / 16.0F);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(state, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        });
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if(entity instanceof MinecartHolder holder) {
                MinecraftClient.getInstance().getSoundManager().play(new TrainLibMinecartSoundInstance(holder.minecart()));
            }
        });
        CargoRenderingRegistry.getInstance().register(CargoType.ENTITY_CARGO_TYPE, (cargo, view, tickDelta, matrices, vertexConsumers, light) -> {
        });
    }
}
