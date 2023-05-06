package io.github.stuff_stuffs.train_lib.internal.common.entity;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.impl.common.AbstractCartImpl;
import io.github.stuff_stuffs.train_lib.impl.common.MinecartImpl;
import io.github.stuff_stuffs.train_lib.internal.common.item.TrainLibItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
    protected void setAttached(final AbstractCartImpl<?, ?> attached) {
        if (attached instanceof MinecartImpl other) {
            minecart.setAttached(other);
        }
    }

    @Override
    protected void setAttachment(final AbstractCartImpl<?, ?> attached) {
        if (attached instanceof MinecartImpl other) {
            other.setAttached(minecart);
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
        if (minecart.forwardsAligned()) {
            i |= 1;
        }
        return i;
    }

    @Override
    protected void applyFlags(final int flags, final AbstractCartImpl<?, ?> cart) {
        cart.forwardsAligned((flags & 1) == 1);
    }

    @Override
    protected boolean tryLink(final AbstractCartImpl<?, ?> first, final AbstractCartImpl<?, ?> second, final boolean force) {
        if (first instanceof MinecartImpl firstImpl && second instanceof MinecartImpl secondImpl) {
            final boolean off = first.attached() != null;
            final boolean ofb = first.attachment() != null;

            final boolean obf = second.attached() != null;
            final boolean obb = second.attachment() != null;

            if (!obb && !off) {
                firstImpl.setAttached(secondImpl);
                return true;
            }

            if (!obf && !ofb) {
                secondImpl.setAttached(firstImpl);
                return true;
            }

            if (!force) {
                return false;
            }

            secondImpl.setAttached(firstImpl);
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
            public void onAttachedChange() {
                if (!world.isClient()) {
                    for (final ActiveMinecraftConnection connection : CoreMinecraftNetUtil.getPlayersWatching(world, centeredBlockPos())) {
                        ATTACHED_CHANGE.send(connection, MinecartCartEntity.this);
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
                    final Vec3d distance = following.position().subtract(getPos());
                    final double length = distance.length();
                    final double optimal = 1.5;
                    final Vec3d velocity = following.holder().getVelocity().add(distance.multiply(1 / length).multiply(length - optimal));
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
            public boolean shouldDisconnect(final Cart minecart, @Nullable final Cart following) {
                return following == null || minecart.position().squaredDistanceTo(following.position()) > (5 * 5);
            }
        };
    }

    @Override
    public MinecartImpl minecart() {
        return minecart;
    }
}
