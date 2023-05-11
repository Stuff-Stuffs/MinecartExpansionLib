package io.github.stuff_stuffs.train_lib.internal.common.entity;

import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.ParentNetIdSingle;
import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.McNetworkStack;
import io.github.stuff_stuffs.train_lib.api.common.cart.Rail;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.EntityCargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.api.common.entity.PreviousInteractionAwareEntity;
import io.github.stuff_stuffs.train_lib.api.common.util.MathUtil;
import io.github.stuff_stuffs.train_lib.impl.common.AbstractCartImpl;
import io.github.stuff_stuffs.train_lib.internal.client.render.FastMountEntity;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLibDamageSources;
import io.github.stuff_stuffs.train_lib.internal.common.util.TrainTrackingUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractCartEntity extends Entity implements FastMountEntity, PreviousInteractionAwareEntity {
    private static final String ATTACHED_KEY = "attached_minecart";
    public static final ParentNetIdSingle<AbstractCartEntity> NET_PARENT = McNetworkStack.ENTITY.subType(AbstractCartEntity.class, TrainLib.MOD_ID + ":cart");
    public static final NetIdDataK<AbstractCartEntity> CARGO_CHANGE = NET_PARENT.idData("cargo_change").setReadWrite((obj, buffer, ctx) -> {
        if (buffer.readBoolean()) {
            final Cargo cargo = buffer.decode(NbtOps.INSTANCE, Cargo.CODEC);
            obj.cart().cargo(cargo);
        } else {
            obj.cart().cargo(null);
        }
    }, (obj, buffer, ctx) -> {
        final Cargo cargo = obj.cart().cargo();
        if (cargo != null) {
            buffer.writeBoolean(true);
            buffer.encode(NbtOps.INSTANCE, Cargo.CODEC, cargo);
        } else {
            buffer.writeBoolean(false);
        }
    }).toClientOnly();
    public static final NetIdDataK<AbstractCartEntity> SPEED_POS_UPDATE = NET_PARENT.idData("speed_pos", Float.BYTES * 3 + Float.BYTES + Float.BYTES + Integer.BYTES).setReadWrite((obj, buffer, ctx) -> {
        final double x = buffer.readFloat();
        final double y = buffer.readFloat();
        final double z = buffer.readFloat();
        final double progress = buffer.readFloat();
        final double speed = buffer.readFloat();
        int flags = buffer.readInt();
        final AbstractCartImpl<?, ?> cart = obj.cart();
        cart.position(new Vec3d(x, y, z));
        obj.setPos(x, y, z);
        cart.progress(progress);
        obj.applyFlags(flags, obj.cart());
        cart.speed(speed);
    }, (obj, buffer, ctx) -> {
        final AbstractCartImpl<?, ?> cart = obj.cart();
        buffer.writeFloat((float) cart.position().getX());
        buffer.writeFloat((float) cart.position().getY());
        buffer.writeFloat((float) cart.position().getZ());
        buffer.writeFloat((float) cart.progress());
        buffer.writeFloat((float) cart.speed());
        buffer.writeInt(obj.writeFlags(cart));
    }).withTinySize().toClientOnly();
    public static final NetIdDataK<AbstractCartEntity> TRY_LINK = NET_PARENT.idData("try_link", Integer.BYTES * 2).setReceiver((obj, buffer, ctx) -> {
        final int otherId = buffer.readInt();
        final Entity other = obj.world.getEntityById(otherId);
        Entity actor = obj.world.getEntityById(buffer.readInt());
        if (actor instanceof ServerPlayerEntity player) {
            if (player.getStackInHand(Hand.MAIN_HAND).isOf(Items.CHAIN)) {
                AbstractCartImpl<?, ?> cart = obj.extract(other);
                if (cart != null && !obj.cart().isDestroyed() && !cart.isDestroyed()) {
                    if (obj.tryLink(obj.cart(), cart, true)) {
                        if (player.interactionManager.getGameMode().isSurvivalLike()) {
                            player.getStackInHand(Hand.MAIN_HAND).decrement(1);
                        }
                        obj.world.playSoundFromEntity(null, obj, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    }
                }
            }
        }
    }).withTinySize().toServerOnly();
    public static final NetIdDataK<AbstractCartEntity> TRAIN_UPDATE = NET_PARENT.idData("train_update").setReadWrite((obj, buffer, ctx) -> {
        final int carCount = buffer.readVarInt();
        IntList carIds = new IntArrayList(carCount);
        for (int i = 0; i < carCount; i++) {
            carIds.add(buffer.readInt());
        }
        List<Entity> holders = new ArrayList<>(carCount);
        final IntListIterator iterator = carIds.iterator();
        while (iterator.hasNext()) {
            Entity entity = obj.world.getEntityById(iterator.nextInt());
            if (entity != null) {
                holders.add(entity);
            } else {
                return;
            }
        }
        obj.linkAll(holders);
    }, (obj, buffer, ctx) -> {
        final List<? extends AbstractCartImpl<?, ?>> cars = obj.cart().cars();
        buffer.writeVarInt(cars.size());
        for (AbstractCartImpl<?, ?> car : cars) {
            buffer.writeInt(car.holder().getId());
        }
    }).toClientOnly();
    private final MinecartMovementTracker movementTracker;
    public float wheelAngle;
    public static final TrackedData<Float> DAMAGE_WOBBLE_STRENGTH = DataTracker.registerData(AbstractCartEntity.class, TrackedDataHandlerRegistry.FLOAT);


    public AbstractCartEntity(final EntityType<?> type, final World world) {
        super(type, world);
        movementTracker = new MinecartMovementTracker(() -> new MinecartMovementTracker.Entry(1.0F, getPos(), Vec3d.fromPolar(0, 0), Rail.DEFAULT_UP));
    }

    @Override
    public void tick() {
        super.tick();
        final float damageWobble = dataTracker.get(DAMAGE_WOBBLE_STRENGTH);
        if (damageWobble > 0.0F) {
            dataTracker.set(DAMAGE_WOBBLE_STRENGTH, Math.max(damageWobble - 1, 0));
        }
        final AbstractCartImpl<?, ?> cart = cart();
        wheelAngle = wheelAngle + (float) (cart.speed() * 9 / Math.PI);
        cart.tick();
        if (cart.onRail()) {
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
                final List<Entity> connected = cart.cars().stream().map(AbstractCartImpl::holder).filter(Objects::nonNull).toList();
                final double speed = Math.abs(cart.speed()) + 0.9;
                final double massContribution = cart.mass();
                final float damage = (float) (speed * speed * massContribution);
                for (final Box box : boxes) {
                    final List<Entity> entities = world.getOtherEntities(this, box, i -> !connected.contains(i) && !connected.contains(i.getRootVehicle()) && i instanceof LivingEntity);
                    for (final Entity entity : entities) {
                        entity.damage(TrainLibDamageSources.createTrain(this), damage);
                    }
                }
            }
        }
        final long offsetTime = world.getTime() + cart.randomOffset();
        final boolean speedUpdate = offsetTime % 32 == 0;
        final boolean trainUpdate = offsetTime % 128 == 0;
        if (!world.isClient && speedUpdate | trainUpdate) {
            for (final ServerPlayerEntity player : PlayerLookup.tracking(this)) {
                final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(player);
                if (speedUpdate) {
                    SPEED_POS_UPDATE.send(connection, this);
                }
                if (trainUpdate) {
                    if (TrainTrackingUtil.shouldSend(this, player)) {
                        TRAIN_UPDATE.send(connection, this);
                    }
                }
            }
        }
    }

    @Override
    public boolean damage(final DamageSource source, final float amount) {
        if (world.isClient || isRemoved()) {
            return true;
        } else if (isInvulnerableTo(source)) {
            return false;
        } else {
            final float damageWobble = dataTracker.get(DAMAGE_WOBBLE_STRENGTH);
            emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
            final boolean creative = source.getAttacker() instanceof PlayerEntity && ((PlayerEntity) source.getAttacker()).getAbilities().creativeMode;
            if (creative || amount + damageWobble > 4.0F) {
                removeAllPassengers();
                if (creative && !hasCustomName()) {
                    discard();
                } else {
                    dropItems(source);
                }
            } else {
                dataTracker.set(DAMAGE_WOBBLE_STRENGTH, damageWobble + amount);
            }
            return true;
        }
    }

    public void dropItems(final DamageSource damageSource) {
        kill();
        if (world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            final ItemStack itemStack = new ItemStack(item());
            if (hasCustomName()) {
                itemStack.setCustomName(getCustomName());
            }
            dropStack(itemStack);
            final Cargo cargo = cart().cargo();
            if (cargo != null) {
                for (final ItemStack drop : cargo.drops(damageSource)) {
                    dropStack(drop);
                }
            }
        }
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public void setVelocity(final Vec3d velocity) {
        super.setVelocity(velocity);
        if (!cart().onRail()) {
            cart().speed(velocity.length() * 0.01);
        }
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(DAMAGE_WOBBLE_STRENGTH, 0.0F);
    }

    @Override
    protected void readCustomDataFromNbt(final NbtCompound nbt) {
        if (nbt.contains("cart", NbtElement.COMPOUND_TYPE)) {
            cart().load(nbt.getCompound("cart"));
        }
        if (nbt.contains(ATTACHED_KEY, NbtElement.COMPOUND_TYPE)) {
            final Entity entity = EntityType.loadEntityWithPassengers(nbt.getCompound(ATTACHED_KEY), world, Function.identity());
            final AbstractCartImpl<?, ?> extract = extract(entity);
            if (extract != null) {
                world.spawnEntity(entity);
                attach(extract);
            }
        }
    }

    @Override
    protected void writeCustomDataToNbt(final NbtCompound nbt) {
        final AbstractCartImpl<?, ?> attachment = cart().attachment();
        if (attachment != null) {
            final Entity holder = attachment.holder();
            if (holder != null) {
                final NbtCompound sub = new NbtCompound();
                holder.saveSelfNbt(sub);
                nbt.put(ATTACHED_KEY, sub);
            }
        }
        final NbtCompound cart = new NbtCompound();
        cart().save(cart);
        nbt.put("cart", cart);
    }

    @Override
    public double getMountedHeightOffset() {
        return 0;
    }

    @Override
    public void updateFastPassengerPosition(final Entity entity, final float tickDelta) {
        final Vec3d position = fastPosition(tickDelta).add(0, entity.getHeightOffset(), 0);
        entity.setPosition(position);
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
        return other.isCollidable() && cart().cars().stream().noneMatch(car -> car.holder().isConnectedThroughVehicle(other));
    }

    @Override
    public void pushAwayFrom(final Entity entity) {
        final AbstractCartImpl<?, ?> cart = cart();
        final List<? extends AbstractCartImpl<?, ?>> cars = cart.cars();
        if (cars.stream().noneMatch(car -> car.holder().isConnectedThroughVehicle(entity))) {
            if (cart.onRail()) {
                double massAhead = 0;
                double massBehind = 0;
                AbstractCartImpl<?, ?> ahead = cart.attached();
                while (ahead != null) {
                    massAhead += ahead.mass();
                    ahead = ahead.attached();
                }
                AbstractCartImpl<?, ?> behind = cart.attachment();
                while (behind != null) {
                    massBehind += behind.mass();
                    behind = behind.attachment();
                }
                if (MathUtil.approxEquals(massAhead, massBehind)) {
                    final Vec3d push = cart.position().subtract(entity.getPos());
                    final Vec3d velocity = cart.velocity();
                    final double dot = push.dotProduct(velocity);
                    if (MathUtil.approxEquals(dot, 0) || dot > 0) {
                        cart.addSpeed(Math.copySign(0.05, cart.speed()));
                    } else {
                        cart.addSpeed(Math.copySign(0.05, -cart.speed()));
                    }
                } else if (massAhead > massBehind) {
                    cart.addSpeed(Math.copySign(0.05, cart.speed()));
                } else {
                    cart.addSpeed(Math.copySign(0.05, -cart.speed()));
                }
            } else {
                super.pushAwayFrom(entity);
            }
        }
    }

    @Override
    public void remove(final RemovalReason reason) {
        cart().destroy();
        super.remove(reason);
    }

    @Override
    public boolean saveNbt(final NbtCompound nbt) {
        return shouldSave() && saveSelfNbt(nbt);
    }

    @Override
    public boolean shouldSave() {
        return super.shouldSave() && cart().attached() == null;
    }

    @Override
    public ActionResult interact(final PlayerEntity player, final Hand hand) {
        if (hand == Hand.MAIN_HAND && player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            if (hasPassengers() || cart().cargo() != null) {
                return ActionResult.PASS;
            }
            if (world.isClient) {
                return ActionResult.SUCCESS;
            } else {
                if (player.startRiding(this)) {
                    cart().cargo(new EntityCargo(player.getUuid()));
                    return ActionResult.CONSUME;
                } else {
                    return ActionResult.PASS;
                }
            }
        }
        return super.interact(player, hand);
    }

    @Override
    protected boolean canAddPassenger(final Entity passenger) {
        return super.canAddPassenger(passenger) && cart().cargo() == null;
    }

    @Nullable
    @Override
    public ItemStack getPickBlockStack() {
        return new ItemStack(item());
    }

    @Override
    public void onStartedTrackingBy(final ServerPlayerEntity player) {
        final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(player);
        CARGO_CHANGE.send(connection, this);
        SPEED_POS_UPDATE.send(connection, this);
        if (TrainTrackingUtil.shouldSend(this, player)) {
            TRAIN_UPDATE.send(connection, this);
        }
    }

    @Override
    public void updatePassengerPosition(final Entity passenger) {
        final Vec3d position = fastPosition(1.0F).add(0, passenger.getHeightOffset(), 0);
        passenger.setPosition(position);
    }

    public MinecartMovementTracker movementTracker() {
        return movementTracker;
    }

    @Override
    public ActionResult previousInteractionAwareInteract(final PlayerEntity player, final Hand hand, final Entity previousInteraction) {
        if (world.isClient) {
            if (player == MinecraftClient.getInstance().player && player.getStackInHand(hand).isOf(Items.CHAIN)) {
                if (previousInteraction instanceof MinecartHolder) {
                    TRY_LINK.send(CoreMinecraftNetUtil.getConnection(player), this, (obj, buffer, ctx) -> {
                        buffer.writeInt(previousInteraction.getId());
                        buffer.writeInt(player.getId());
                    });
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    protected abstract Item item();

    protected abstract void linkAll(List<Entity> holders);

    protected abstract void attach(AbstractCartImpl<?, ?> toAttach);

    public abstract AbstractCartImpl<?, ?> cart();

    protected abstract @Nullable AbstractCartImpl<?, ?> extract(Entity entity);

    protected abstract int writeFlags(final AbstractCartImpl<?, ?> cart);

    protected abstract void applyFlags(final int flags, final AbstractCartImpl<?, ?> cart);

    protected abstract boolean tryLink(final AbstractCartImpl<?, ?> first, final AbstractCartImpl<?, ?> second, final boolean force);
}
