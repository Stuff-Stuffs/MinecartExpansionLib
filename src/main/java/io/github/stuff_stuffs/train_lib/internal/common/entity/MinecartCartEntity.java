package io.github.stuff_stuffs.train_lib.internal.common.entity;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.impl.common.AbstractCartImpl;
import io.github.stuff_stuffs.train_lib.impl.common.MinecartImpl;
import io.github.stuff_stuffs.train_lib.internal.common.item.TrainLibItems;
import io.github.stuff_stuffs.train_lib.internal.common.util.TrainTrackingUtil;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MinecartCartEntity extends AbstractCartEntity implements MinecartHolder {
    private final MinecartImpl minecart;

    public MinecartCartEntity(final EntityType<?> type, final World world) {
        super(type, world);
        minecart = new MinecartImpl(world, createTracker(), createHandler(), this);
    }

    protected BlockPos centeredBlockPos() {
        return BlockPos.ofFloored(getPos());
    }

    @Override
    protected Item item() {
        return TrainLibItems.FAST_MINECART_ITEM;
    }

    @Override
    protected void linkAll(final List<Entity> holders) {
        final List<MinecartImpl> carts = new ArrayList<>(holders.size());
        for (final Entity holder : holders) {
            if (holder instanceof MinecartHolder minecartHolder) {
                carts.add(minecartHolder.minecart());
            } else {
                return;
            }
        }
        carts.get(0).linkAll(carts);
    }

    @Override
    protected void setAttachment(final AbstractCartImpl<?, ?> attached) {
        if (attached instanceof MinecartImpl other) {
            minecart.train().link(other);
        }
    }

    @Override
    public AbstractCartImpl<?, ?> cart() {
        return minecart;
    }

    @Override
    protected @Nullable AbstractCartImpl<?, ?> extract(final Entity entity) {
        if (entity instanceof MinecartHolder holder) {
            return holder.minecart();
        }
        return null;
    }

    @Override
    protected int writeFlags(final AbstractCartImpl<?, ?> cart) {
        int i = 0;
        if (minecart.inverted()) {
            i |= 1;
        }
        return i;
    }

    @Override
    protected void applyFlags(final int flags, final AbstractCartImpl<?, ?> cart) {
        cart.inverted((flags & 1) == 1);
    }

    @Override
    protected boolean tryLink(final AbstractCartImpl<?, ?> first, final AbstractCartImpl<?, ?> second, final boolean force) {
        if (first instanceof MinecartImpl firstImpl && second instanceof MinecartImpl secondImpl) {
            firstImpl.train().link(secondImpl);
            return true;
        }
        return false;
    }

    protected Cart.Tracker createTracker() {
        return new AbstractCartImpl.Tracker() {
            @Override
            public void onMove(final Vec3d position, final Vec3d tangent, final Vec3d up, final double time) {
                setPosition(position);
                movementTracker().addEntry(time, position, tangent, up);
            }

            @Override
            public void reset() {
                movementTracker().reset();
            }

            @Override
            public void onCargoChange() {
                if (!world.isClient()) {
                    for (final ActiveMinecraftConnection connection : CoreMinecraftNetUtil.getPlayersWatching(world, centeredBlockPos())) {
                        CARGO_CHANGE.send(connection, MinecartCartEntity.this);
                    }
                }
            }

            @Override
            public void trainChange() {
                if (world.isClient) {
                    return;
                }
                if (minecart.attached() != null) {
                    return;
                }
                for (final ServerPlayerEntity player : PlayerLookup.tracking(MinecartCartEntity.this)) {
                    if (TrainTrackingUtil.shouldSend(MinecartCartEntity.this, player)) {
                        TRAIN_UPDATE.send(CoreMinecraftNetUtil.getConnection(player), MinecartCartEntity.this);
                    }
                }
            }
        };
    }

    protected Cart.OffRailHandler createHandler() {
        return new Cart.OffRailHandler() {
            @Override
            public double handle(final Cart minecart, @Nullable final Cart following, final Vec3d position, final double time) {
                //TODO partial movements
                setPosition(position);
                if (following != null) {
                    Vec3d distance = following.position().subtract(getPos()).withAxis(Direction.Axis.Y, 0);
                    final double optimal = minecart.bufferSpace() + following.bufferSpace();
                    if (distance.lengthSquared() < 0.1) {
                        distance = new Vec3d(optimal, 0, 0);
                    }
                    final double length = distance.length();
                    final Vec3d velocity = following.holder().getVelocity().add(distance.multiply((1 / length) * (length-optimal)));
                    setVelocity(velocity);
                    move(MovementType.SELF, velocity.multiply(time));
                    minecart.position(getPos());
                    return 0;
                }
                final Vec3d add = getVelocity().multiply(0.96).add(0, -0.04, 0);
                setVelocity(add);
                move(MovementType.SELF, add.multiply(time));
                minecart.position(getPos());
                return 0;
            }

            @Override
            public boolean shouldDisconnect(final Cart minecart, final Cart following) {
                return minecart.position().squaredDistanceTo(following.position()) > (5 * 5);
            }
        };
    }

    @Override
    public MinecartImpl minecart() {
        return minecart;
    }
}
