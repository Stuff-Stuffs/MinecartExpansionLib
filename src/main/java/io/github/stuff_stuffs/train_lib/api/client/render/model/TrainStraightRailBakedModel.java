package io.github.stuff_stuffs.train_lib.api.client.render.model;

import io.github.stuff_stuffs.train_lib.internal.common.blocks.StraightTrainRailBlock;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class TrainStraightRailBakedModel implements BakedModel, FabricBakedModel {
    private final Sprite particle;

    public TrainStraightRailBakedModel(final Sprite particle) {
        this.particle = particle;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(final BlockRenderView blockView, final BlockState state, final BlockPos pos, final Supplier<Random> randomSupplier, final RenderContext context) {
        if (state.get(StraightTrainRailBlock.SIDE) == StraightTrainRailBlock.Side.LEFT) {
            final RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
            TrainRailModelGenerator.generate(new TrainRailModelGenerator.RailInfo() {
                @Override
                public double length() {
                    return 1;
                }

                @Override
                public Vec3d posLeft(final double t) {
                    return new Vec3d(0, 0, t);
                }

                @Override
                public Vec3d posRight(final double t) {
                    return new Vec3d(1.5, 0, t);
                }

                @Override
                public Vec3d normal(final double t) {
                    return new Vec3d(-1,0,0);
                }

                @Override
                public Vec3d up(final double t) {
                    return new Vec3d(0,1,0);
                }
            }, context.getEmitter(), 0xFF7F7F7F, material, 0xFF7F7F7F, material);
        }
    }

    @Override
    public void emitItemQuads(final ItemStack stack, final Supplier<Random> randomSupplier, final RenderContext context) {

    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction face, final Random random) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return particle;
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }
}
