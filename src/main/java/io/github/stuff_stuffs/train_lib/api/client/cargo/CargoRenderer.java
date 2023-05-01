package io.github.stuff_stuffs.train_lib.api.client.cargo;

import io.github.stuff_stuffs.train_lib.api.common.cart.MinecartView;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public interface CargoRenderer<T extends Cargo> {
    void renderCargo(T cargo, MinecartView view, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
}
