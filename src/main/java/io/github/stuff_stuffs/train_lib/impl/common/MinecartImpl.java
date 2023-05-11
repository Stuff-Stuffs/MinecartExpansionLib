package io.github.stuff_stuffs.train_lib.impl.common;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.train_lib.api.common.cart.RailProvider;
import io.github.stuff_stuffs.train_lib.api.common.cart.TrainLibApi;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRailProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MinecartImpl extends AbstractCartImpl<MinecartRail, BlockPos> {
    private static final double BUFFER_SPACE = 0.625;
    private static final double EMPTY_CART_MASS = 1;

    public <T extends Entity & MinecartHolder> MinecartImpl(final World world, final Tracker tracker, final OffRailHandler offRailHandler, final T holder) {
        super(world, tracker, offRailHandler, holder, CartPathfinder.MINECART_PATHFINDER);
    }

    @Override
    public @Nullable MinecartImpl attached() {
        return (MinecartImpl) super.attached();
    }

    @Override
    public @Nullable MinecartImpl attachment() {
        return (MinecartImpl) super.attachment();
    }

    @Override
    protected BlockPos positionFromRail(final MinecartRail rail) {
        return rail.railPosition();
    }

    @Override
    protected Optional<RailProvider.NextRailInfo<MinecartRail>> next(final BlockPos pos, final Direction exitDirection, final double time, final boolean forwards) {
        final MinecartRail rail = currentRail();
        if ((forwards && rail.exitPosition().equals(pos)) || (!forwards && rail.entrancePosition().equals(pos))) {
            return Optional.empty();
        }
        final BlockPos blockPos = forwards ? rail.exitPosition() : rail.entrancePosition();
        final MinecartRailProvider provider = (MinecartRailProvider) tryGetProvider(blockPos);
        if (provider == null) {
            return Optional.empty();
        }
        final RailProvider.NextRailInfo<MinecartRail> railInfo = provider.next(this, rail, exitDirection);
        if (railInfo == null) {
            return Optional.empty();
        }
        return Optional.of(railInfo);
    }

    @Override
    protected BlockPos findOrDefault(final Vec3d position, final World world) {
        return BlockPos.ofFloored(position);
    }

    @Override
    protected double emptyCartMass() {
        return EMPTY_CART_MASS;
    }

    @Override
    public double bufferSpace() {
        return BUFFER_SPACE;
    }

    @Override
    protected @Nullable RailProvider<MinecartRail> tryGetProvider(final BlockPos pos) {
        MinecartRailProvider provider = TrainLibApi.MINECART_RAIL_BLOCK_API.find(world, pos, null);
        if (provider == null) {
            provider = TrainLibApi.MINECART_RAIL_BLOCK_API.find(world, pos.down(), null);
        }
        return provider;
    }

    @Override
    public BlockPos currentPosition(final MinecartRail currentRail) {
        return currentRail.railPosition();
    }
}
