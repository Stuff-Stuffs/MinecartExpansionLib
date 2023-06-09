package io.github.stuff_stuffs.train_lib.internal.client.render.entity;

import io.github.stuff_stuffs.train_lib.api.client.cargo.CargoRenderer;
import io.github.stuff_stuffs.train_lib.api.client.cargo.CargoRenderingRegistry;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.CargoType;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.entity.FastMinecartEntity;
import io.github.stuff_stuffs.train_lib.internal.common.entity.MinecartMovementTracker;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.MinecartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class FastMinecartEntityRenderer<T extends FastMinecartEntity> extends EntityRenderer<T> {
    public static final EntityModelLayer WHEEL_LAYER = new EntityModelLayer(TrainLib.id("cart_wheels"), "wheels");
    private static final Identifier TEXTURE = new Identifier("textures/entity/minecart.png");
    private static final Identifier WHEEL_TEXTURE = TrainLib.id("textures/entity/minecart_wheel.png");
    protected final EntityModel<T> model;
    protected final ModelPart wheelPart;
    protected final BlockRenderManager blockRenderManager;


    public FastMinecartEntityRenderer(final EntityRendererFactory.Context ctx) {
        super(ctx);
        model = new MinecartEntityModel<>(ctx.getPart(EntityModelLayers.MINECART));
        wheelPart = ctx.getPart(WHEEL_LAYER);
        blockRenderManager = ctx.getBlockRenderManager();
    }

    @Override
    public void render(final T entity, final float yaw, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        final double heightOffset = 0.375;
        final MinecartMovementTracker tracker = entity.movementTracker();
        final MinecartMovementTracker.Entry entry = tracker.at(tickDelta);
        final MinecartMovementTracker.Entry next = tracker.next(tickDelta);
        final Quaternionf quat;
        final Vec3d forward = entry.forward();
        if (entry.equals(next)) {
            final float y = (float) -(Math.atan2(forward.z, forward.x) + Math.PI);
            final float p = (float) -Math.asin(forward.y);
            quat = new Quaternionf().rotationYXZ(y, 0, p);
        } else {
            final float y0 = (float) -(Math.atan2(forward.z, forward.x) + Math.PI);
            final float p0 = (float) -Math.asin(forward.y);
            final float y1 = (float) -(Math.atan2(next.forward().z, next.forward().x) + Math.PI);
            final float p1 = (float) -Math.asin(next.forward().y);
            final float weight = (tickDelta - entry.time()) / (next.time() - entry.time());
            quat = new Quaternionf().rotationYXZ(y0, 0, p0).slerp(new Quaternionf().rotationYXZ(y1, 0, p1), weight);
        }
        matrices.push();
        final Vec3d position = entity.fastPosition(tickDelta);
        matrices.translate(position.x - entity.lastRenderX, position.y - entity.lastRenderY, position.z - entity.lastRenderZ);
        matrices.multiply(quat);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.sin(entity.age) * entity.getDataTracker().get(FastMinecartEntity.DAMAGE_WOBBLE_STRENGTH) / 10.0F));
        matrices.translate(0, heightOffset, 0);
        matrices.push();
        matrices.scale(-1, -1, 1);
        model.setAngles(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(model.getLayer(getTexture(entity)));
        model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

        wheelPart.setAngles(0, 0, entity.wheelAngle);
        matrices.pop();

        matrices.push();
        matrices.translate(4.5 / 16.0, -4.5 / 16.0, 8 / 16.0);
        final VertexConsumer wheelBuffer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(WHEEL_TEXTURE));
        wheelPart.render(matrices, wheelBuffer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        matrices.pop();

        matrices.push();
        matrices.translate(4.5 / 16.0, -4.5 / 16.0, -9 / 16.0);
        wheelPart.render(matrices, wheelBuffer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        matrices.pop();

        matrices.push();
        matrices.translate(-4.5 / 16.0, -4.5 / 16.0, 8 / 16.0);
        wheelPart.render(matrices, wheelBuffer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        matrices.pop();

        matrices.push();
        matrices.translate(-4.5 / 16.0, -4.5 / 16.0, -9 / 16.0);
        wheelPart.render(matrices, wheelBuffer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        matrices.pop();

        final Cargo cargo = entity.minecart().cargo();
        if (cargo != null) {
            final CargoType<?> type = cargo.type();
            matrices.push();
            matrices.translate(-0.5, -heightOffset, -0.5);
            renderCargo(type, cargo, entity, tickDelta, matrices, vertexConsumers, light);
            matrices.pop();
        }
        matrices.pop();
    }

    protected static <T extends Cargo> void renderCargo(final CargoType<T> type, final Cargo cargo, final FastMinecartEntity entity, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final int light) {
        final CargoRenderer<? super T> renderer = CargoRenderingRegistry.getInstance().get(type);
        if (renderer != null) {
            renderer.renderCargo((T) cargo, entity.minecart(), tickDelta, matrices, vertexConsumers, light);
        }
    }

    @Override
    public Identifier getTexture(final T entity) {
        return TEXTURE;
    }

    public static TexturedModelData createModelData() {
        final ModelData data = new ModelData();
        final ModelPartData modelPart = data.getRoot();
        modelPart.addChild("wheel", ModelPartBuilder.create().uv(0, 0).cuboid(0, 0, 0, 3, 3, 1), ModelTransform.pivot(-1.5F, -1.5F, 0));
        return TexturedModelData.of(data, 16, 16);
    }
}
