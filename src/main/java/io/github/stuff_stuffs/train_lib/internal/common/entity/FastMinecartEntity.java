package io.github.stuff_stuffs.train_lib.internal.common.entity;

import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.ParentNetIdSingle;
import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.McNetworkStack;
import io.github.stuff_stuffs.train_lib.api.common.cart.Minecart;
import io.github.stuff_stuffs.train_lib.api.common.cart.MinecartHolder;
import io.github.stuff_stuffs.train_lib.api.common.cart.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.entity.PreviousInteractionAwareEntity;
import io.github.stuff_stuffs.train_lib.impl.common.MinecartImpl;
import io.github.stuff_stuffs.train_lib.internal.client.TrainLibClient;
import io.github.stuff_stuffs.train_lib.internal.client.render.FastMountEntity;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLibDamageSources;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class FastMinecartEntity extends Entity implements MinecartHolder, FastMountEntity, PreviousInteractionAwareEntity {
    private static final String ATTACHED_KEY = "attached_minecart";
    public static final ParentNetIdSingle<FastMinecartEntity> NET_PARENT = McNetworkStack.ENTITY.subType(FastMinecartEntity.class, TrainLib.MOD_ID + ":minecart");
    public static final NetIdDataK<FastMinecartEntity> CARGO_CHANGE = NET_PARENT.idData("cargo_change").setReadWrite((obj, buffer, ctx) -> {
        ctx.assertClientSide();
        if (buffer.readBoolean()) {
            final Cargo cargo = buffer.decode(NbtOps.INSTANCE, Cargo.CODEC);
            obj.minecart.cargo(cargo);
        } else {
            obj.minecart.cargo(null);
        }
    }, (obj, buffer, ctx) -> {
        ctx.assertServerSide();
        final Cargo cargo = obj.minecart.cargo();
        if (cargo != null) {
            buffer.writeBoolean(true);
            buffer.encode(NbtOps.INSTANCE, Cargo.CODEC, cargo);
        } else {
            buffer.writeBoolean(false);
        }
    }).toClientOnly();
    public static final NetIdDataK<FastMinecartEntity> ATTACHED_CHANGE = NET_PARENT.idData("attached_change", Integer.BYTES).setReadWrite((obj, buffer, ctx) -> {
        final int attachedId = buffer.readInt();
        if (attachedId == -1) {
            obj.minecart.setAttached(null);
        } else {
            final Entity entity = obj.world.getEntityById(attachedId);
            if (entity instanceof MinecartHolder holder) {
                obj.minecart.setAttached((MinecartImpl) holder.minecart());
            } else {
                TrainLibClient.LOGGER.error("Tried to attach minecart {} to {}, but could not find {}", obj.getId(), attachedId, attachedId);
            }
        }
    }, (obj, buffer, ctx) -> {
        final MinecartImpl attached = obj.minecart.attached();
        if (attached != null) {
            if (attached.holder() instanceof MinecartHolder) {
                buffer.writeInt(attached.holder().getId());
            } else {
                TrainLib.LOGGER.error("Minecart {} exists without a MinecartHolder holder, maybe null", obj.getId());
            }
        } else {
            buffer.writeInt(-1);
        }
    }).withTinySize().toClientOnly();
    public static final NetIdDataK<FastMinecartEntity> SPEED_POS_UPDATE = NET_PARENT.idData("speed_pos", Double.BYTES * 3 + Double.BYTES + Double.BYTES).setReadWrite((obj, buffer, ctx) -> {
        final double x = buffer.readFloat();
        final double y = buffer.readFloat();
        final double z = buffer.readFloat();
        final double progress = buffer.readFloat();
        final double speed = buffer.readFloat();
        obj.minecart.position(new Vec3d(x, y, z));
        obj.minecart.progress(progress);
        obj.minecart.speed(speed);
    }, (obj, buffer, ctx) -> {
        buffer.writeFloat((float)obj.getX());
        buffer.writeFloat((float)obj.getY());
        buffer.writeFloat((float)obj.getZ());
        buffer.writeFloat((float)obj.minecart.progress());
        buffer.writeFloat((float)obj.minecart.speed());
    }).withTinySize().toClientOnly();
    public static final NetIdDataK<FastMinecartEntity> TRY_LINK = NET_PARENT.idData("try_link", Integer.BYTES).setReceiver((obj, buffer, ctx) -> {
        final int otherId = buffer.readInt();
        final Entity other = obj.world.getEntityById(otherId);
        if (other instanceof MinecartHolder holder) {
            ((MinecartImpl) holder.minecart()).setAttached(obj.minecart);
        }
    }).withTinySize().toServerOnly();
    private final MinecartMovementTracker movementTracker;
    private final MinecartImpl minecart;
    public float wheelAngle;

    public FastMinecartEntity(final EntityType<?> type, final World world) {
        super(type, world);
        movementTracker = new MinecartMovementTracker(() -> new MinecartMovementTracker.Entry(1.0F, getPos(), Vec3d.fromPolar(0, 0), MinecartRail.DEFAULT_UP));
        minecart = new MinecartImpl(world, new MinecartImpl.Tracker() {
            @Override
            public void onMove(final Vec3d position, final Vec3d tangent, final Vec3d up, final double time) {
                setPosition(position);
                movementTracker.addEntry(time, position, tangent, up);
            }

            @Override
            public void reset() {
                movementTracker.reset();
            }

            @Override
            public void onCargoChange() {
                if (!world.isClient()) {
                    for (final ActiveMinecraftConnection connection : CoreMinecraftNetUtil.getPlayersWatching(world, centeredBlockPos())) {
                        CARGO_CHANGE.send(connection, FastMinecartEntity.this);
                    }
                }
            }

            @Override
            public void onAttachedChange() {
                if (!world.isClient()) {
                    for (final ActiveMinecraftConnection connection : CoreMinecraftNetUtil.getPlayersWatching(world, centeredBlockPos())) {
                        ATTACHED_CHANGE.send(connection, FastMinecartEntity.this);
                    }
                }
            }
        }, createHandler(), this);
    }

    protected Minecart.OffRailHandler createHandler() {
        return new Minecart.OffRailHandler() {
            @Override
            public double handle(final Minecart minecart, @Nullable final Minecart following, final Vec3d position, final double time) {
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
            public boolean shouldDisconnect(final Minecart minecart, @Nullable final Minecart following) {
                return following == null || minecart.position().squaredDistanceTo(following.position()) > (5 * 5);
            }
        };
    }

    @Override
    public void updatePassengerPosition(final Entity passenger) {
        super.updatePassengerPosition(passenger);
    }

    public MinecartMovementTracker movementTracker() {
        return movementTracker;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        wheelAngle = wheelAngle + (float) (minecart.speed() * 9 / Math.PI);
        minecart.tick(centeredBlockPos());
        if (minecart.onRail()) {
            if (!world.isClient) {
                final List<Vec3d> positions = movementTracker.positions();
                final List<Box> boxes = new ArrayList<>();
                final EntityDimensions dimensions = getDimensions(EntityPose.STANDING);
                final int size = positions.size();
                for (int i = 0; i < size; i++) {
                    if (i + 1 < size) {
                        final Vec3d position = positions.get(i);
                        final Vec3d next = positions.get(i + 1);
                        final Vec3d delta = next.subtract(position);
                        boxes.add(dimensions.getBoxAt(position).stretch(delta.x, delta.y, delta.z));
                    } else {
                        boxes.add(dimensions.getBoxAt(positions.get(i)));
                    }
                }
                final List<Entity> connected = minecart.cars().stream().map(MinecartImpl::holder).filter(Objects::nonNull).toList();
                final double speed = Math.abs(minecart.speed()) + 0.9;
                final float damage = (float) (speed * speed * ((minecart.mass() + minecart.massAhead() + minecart.massBehind()) / 10 + 0.9));
                for (final Box box : boxes) {
                    final List<Entity> entities = world.getOtherEntities(this, box, i -> !connected.contains(i) && !connected.contains(i.getRootVehicle()));
                    for (final Entity entity : entities) {
                        entity.damage(TrainLibDamageSources.createTrain(this), damage);
                    }
                }
            }
        }
        if (age % 32 == 0 && !world.isClient) {
            for (final ServerPlayerEntity player : PlayerLookup.tracking(this)) {
                final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(player);
                SPEED_POS_UPDATE.send(connection, this);
            }
        }
    }

    @Override
    public boolean handleAttack(final Entity attacker) {
        minecart.addSpeed(-1.0);
        return super.handleAttack(attacker);
    }

    @Override
    public boolean canHit() {
        return true;
    }

    protected BlockPos centeredBlockPos() {
        return BlockPos.ofFloored(getPos());
    }

    @Override
    public void setVelocity(final Vec3d velocity) {
        super.setVelocity(velocity);
        if (!minecart.onRail()) {
            minecart.speed(velocity.length());
        }
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    protected void readCustomDataFromNbt(final NbtCompound nbt) {
        if (nbt.contains("cart", NbtElement.COMPOUND_TYPE)) {
            minecart.load(nbt.getCompound("cart"));
        }
        if (nbt.contains(ATTACHED_KEY, NbtElement.COMPOUND_TYPE)) {
            final Entity entity = EntityType.loadEntityWithPassengers(nbt.getCompound(ATTACHED_KEY), world, Function.identity());
            if (entity instanceof MinecartHolder holder) {
                world.spawnEntity(entity);
                ((MinecartImpl) holder.minecart()).setAttached(minecart);
            }
        }
    }

    @Override
    protected void writeCustomDataToNbt(final NbtCompound nbt) {
        final MinecartImpl attachment = minecart.attachment();
        if (attachment != null) {
            final Entity holder = attachment.holder();
            if (holder != null) {
                final NbtCompound sub = new NbtCompound();
                holder.saveSelfNbt(sub);
                nbt.put(ATTACHED_KEY, sub);
            }
        }
        final NbtCompound cart = new NbtCompound();
        minecart.save(cart);
        nbt.put("cart", cart);
    }

    @Override
    public double getMountedHeightOffset() {
        return 0;
    }

    @Override
    public void updateFastPassengerPosition(final Entity entity, final float tickDelta) {
        final Vec3d position = fastPosition(tickDelta).add(0, 1, 0);
        entity.setPosition(position);
        entity.lastRenderX = position.x;
        entity.lastRenderY = position.y;
        entity.lastRenderZ = position.z;
    }

    @Override
    public Vec3d fastPosition(final float tickDelta) {
        final MinecartMovementTracker.Entry current = movementTracker.at(tickDelta);
        final MinecartMovementTracker.Entry next = movementTracker.next(tickDelta);
        if (current.equals(next)) {
            return current.position();
        } else {
            final float weight = (tickDelta - current.time()) / (next.time() - current.time());
            final double x = MathHelper.lerp(weight, current.position().x, next.position().x);
            final double y = MathHelper.lerp(weight, current.position().y, next.position().y);
            final double z = MathHelper.lerp(weight, current.position().z, next.position().z);
            return new Vec3d(x, y, z);
        }
    }

    @Override
    public boolean collidesWith(final Entity other) {
        return other.isCollidable() && minecart.cars().stream().filter(car -> car.holder() != null).noneMatch(car -> car.holder().isConnectedThroughVehicle(other));
    }

    @Override
    public void pushAwayFrom(final Entity entity) {
        if (minecart.cars().stream().filter(car -> car.holder() != null).noneMatch(car -> car.holder().isConnectedThroughVehicle(entity))) {
            super.pushAwayFrom(entity);
        }
    }

    @Override
    public void setPosition(final double x, final double y, final double z) {
        super.setPosition(x, y, z);
    }

    @Override
    public boolean saveNbt(final NbtCompound nbt) {
        return shouldSave() && saveSelfNbt(nbt);
    }

    @Override
    public boolean shouldSave() {
        return super.shouldSave() && minecart.attached() == null;
    }

    @Override
    public Minecart minecart() {
        return minecart;
    }

    @Override
    public void onStartedTrackingBy(final ServerPlayerEntity player) {
        final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(player);
        CARGO_CHANGE.send(connection, this);
        ATTACHED_CHANGE.send(connection, this);
        SPEED_POS_UPDATE.send(connection, this);
        final MinecartImpl attachment = minecart.attachment();
        if (attachment != null && attachment.holder() instanceof FastMinecartEntity holder) {
            ATTACHED_CHANGE.send(connection, holder);
        }
    }

    @Override
    public void onStoppedTrackingBy(final ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
    }

    @Override
    public ActionResult previousInteractionAwareInteract(final PlayerEntity player, final Hand hand, final Entity previousInteraction) {
        if (world.isClient) {
            if (player == MinecraftClient.getInstance().player) {
                if (previousInteraction instanceof MinecartHolder) {
                    TRY_LINK.send(CoreMinecraftNetUtil.getConnection(player), this, (obj, buffer, ctx) -> buffer.writeInt(previousInteraction.getId()));
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }
}
