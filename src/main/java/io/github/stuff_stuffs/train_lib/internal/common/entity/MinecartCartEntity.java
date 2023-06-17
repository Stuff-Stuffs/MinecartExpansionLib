package io.github.stuff_stuffs.train_lib.internal.common.entity;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import io.github.stuff_stuffs.train_lib.api.common.cart.AbstractCart;
import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import io.github.stuff_stuffs.train_lib.api.common.cart.entity.AbstractCartEntity;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.impl.common.MinecartImpl;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.item.TrainLibItems;
import io.github.stuff_stuffs.train_lib.internal.common.util.TrainTrackingUtil;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
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
    public MinecartImpl cart() {
        return minecart;
    }

    @Override
    protected @Nullable AbstractCart<?, ?> extract(final Entity entity) {
        if (entity instanceof MinecartHolder holder) {
            return holder.minecart();
        }
        return null;
    }

    @Override
    protected boolean tryLink(final AbstractCart<?, ?> first, final AbstractCart<?, ?> second) {
        if (first instanceof MinecartImpl firstImpl && second instanceof MinecartImpl secondImpl) {
            return firstImpl.train().link(secondImpl);
        }
        return false;
    }

    protected Cart.Tracker createTracker() {
        return new AbstractCart.Tracker() {
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
                if (!getWorld().isClient()) {
                    for (final ActiveMinecraftConnection connection : CoreMinecraftNetUtil.getPlayersWatching(getWorld(), centeredBlockPos())) {
                        CARGO_CHANGE.send(connection, MinecartCartEntity.this);
                    }
                }
            }

            @Override
            public void trainChange() {
                if (getWorld().isClient) {
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
                final double gravity = TrainLib.PHYSICS_MIRROR.gravity();
                if (following != null) {
                    Vec3d distance = following.position().subtract(getPos());
                    final double optimal = minecart.bufferSpace() + following.bufferSpace();
                    if (distance.lengthSquared() < 0.00001) {
                        distance = Vec3d.ZERO;
                    } else {
                        if (distance.lengthSquared() < optimal * optimal) {
                            distance = distance.subtract(distance.normalize().multiply(optimal));
                        } else {
                            final double length = distance.length();
                            distance = distance.subtract(distance.multiply(optimal / length));
                        }
                    }
                    final Vec3d velocity = distance.add(0, -gravity, 0);
                    move(MovementType.SELF, velocity.multiply(time));
                    minecart.position(getPos());
                    return 0;
                }
                final Vec3d velocity = getVelocity().multiply(0.96).add(0, -gravity, 0);
                move(MovementType.SELF, velocity.multiply(time));
                setVelocity(getPos().subtract(position));
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
