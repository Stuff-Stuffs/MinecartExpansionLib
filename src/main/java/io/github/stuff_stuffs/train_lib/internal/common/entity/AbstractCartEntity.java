package io.github.stuff_stuffs.train_lib.internal.common.entity;

import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.ParentNetIdSingle;
import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.McNetworkStack;
import io.github.stuff_stuffs.train_lib.api.common.cart.AbstractCart;
import io.github.stuff_stuffs.train_lib.api.common.cart.Rail;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.EntityCargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartHolder;
import io.github.stuff_stuffs.train_lib.api.common.entity.PreviousInteractionAwareEntity;
import io.github.stuff_stuffs.train_lib.api.common.util.MathUtil;
import io.github.stuff_stuffs.train_lib.internal.client.render.FastMountEntity;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLibDamageSources;
import io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfigModel;
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
    private static final String ATTACHMENT_KEY = "attached_minecart";
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
    public static final NetIdDataK<AbstractCartEntity> SPEED_POS_UPDATE = NET_PARENT.idData("speed_pos").setReceiver((obj, buffer, ctx) -> {
        float speed = buffer.readFloat();
        int count = buffer.readVarInt();
        for (int i = 0; i < count; i++) {
            final int id = buffer.readInt();
            int flags = buffer.readInt();
            float progress = buffer.readFloat();
            float x = buffer.readFloat();
            float y = buffer.readFloat();
            float z = buffer.readFloat();
            final Entity entity = obj.getWorld().getEntityById(id);
            if (entity instanceof AbstractCartEntity cart) {
                cart.applyUpdate(flags, progress, new Vec3d(x, y, z), speed);
            }
        }
    }).toClientOnly();
    public static final NetIdDataK<AbstractCartEntity> TRY_LINK = NET_PARENT.idData("try_link", Integer.BYTES * 2).setReceiver((obj, buffer, ctx) -> {
        final int otherId = buffer.readInt();
        final Entity other = obj.getWorld().getEntityById(otherId);
        Entity actor = obj.getWorld().getEntityById(buffer.readInt());
        if (actor instanceof ServerPlayerEntity player) {
            if (player.getStackInHand(Hand.MAIN_HAND).isOf(Items.CHAIN)) {
                AbstractCart<?, ?> cart = obj.extract(other);
                if (cart != null && !obj.cart().isDestroyed() && !cart.isDestroyed()) {
                    if (obj.tryLink(obj.cart(), cart, true)) {
                        if (player.interactionManager.getGameMode().isSurvivalLike()) {
                            player.getStackInHand(Hand.MAIN_HAND).decrement(1);
                        }
                        obj.getWorld().playSoundFromEntity(null, obj, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.0F, 1.0F);
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
            Entity entity = obj.getWorld().getEntityById(iterator.nextInt());
            if (entity != null) {
                holders.add(entity);
            }
        }
        obj.linkAll(holders);
    }, (obj, buffer, ctx) -> {
        final List<? extends AbstractCart<?, ?>> cars = obj.cart().cars();
        buffer.writeVarInt(cars.size());
        for (AbstractCart<?, ?> car : cars) {
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
        final AbstractCart<?, ?> cart = cart();
        wheelAngle = wheelAngle + (float) (cart.speed() * 9 / Math.PI);
        cart.tick();
        if (cart.onRail()) {
            if (!getWorld().isClient) {
                final List<Vec3d> positions = movementTracker.positions();
                final List<MovementData> movementData = new ArrayList<>();
                final EntityDimensions dimensions = getDimensions(EntityPose.STANDING);
                final int size = positions.size();
                for (int i = 0; i < size; i++) {
                    if (i + 1 < size) {
                        final Vec3d position = positions.get(i);
                        final Vec3d next = positions.get(i + 1);
                        final Vec3d delta = next.subtract(position);
                        movementData.add(new MovementData(dimensions.getBoxAt(position).stretch(delta.x, delta.y, delta.z), delta));
                    } else {
                        movementData.add(new MovementData(dimensions.getBoxAt(positions.get(i)), Vec3d.ZERO));
                    }
                }
                final List<Entity> connected = cart.cars().stream().map(AbstractCart::holder).filter(Objects::nonNull).toList();
                final double speed = Math.abs(cart.speed()) + 0.9;
                final double massContribution = cart.mass();
                final float damage = (float) (speed * speed * massContribution);
                for (final MovementData data : movementData) {
                    final List<Entity> entities = getWorld().getOtherEntities(this, data.box(), i -> !connected.contains(i) && !connected.contains(i.getRootVehicle()) && i instanceof LivingEntity);
                    for (final Entity entity : entities) {
                        final TrainLibConfigModel.EntityCollisionOption option = TrainLib.optionOf(entity);
                        switch (option) {
                            case DEFAULT, IGNORE -> {
                            }
                            case DAMAGE -> entity.damage(TrainLibDamageSources.createTrain(this), damage);
                            case PUSH -> entity.addVelocity(data.velocity());
                        }
                    }
                }
            }
        } else {
            cart.speed(getVelocity().length());
        }
        if (cart.attached() == null && !getWorld().isClient) {
            final long offsetTime = getWorld().getTime() + cart.randomOffset();
            final boolean speedUpdate = offsetTime % 20 == 0;
            final boolean trainUpdate = offsetTime % 127 == 0;
            if (speedUpdate || trainUpdate) {
                sendSyncPackets();
            }
            if (trainUpdate) {
                sendTrainSyncPacket();
            }
        }
    }

    private record MovementData(Box box, Vec3d velocity) {

    }

    protected void sendSyncPackets() {
        for (final ServerPlayerEntity player : PlayerLookup.tracking(this)) {
            if (TrainTrackingUtil.shouldSend(this, player)) {
                final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(player);
                sendSyncPacket(connection);
            }
        }
    }

    protected void sendSyncPacket(final ActiveMinecraftConnection connection) {
        SPEED_POS_UPDATE.send(connection, this, (obj, buffer, ctx) -> {
            final List<? extends AbstractCart<?, ?>> cars = cart().cars();
            buffer.writeFloat((float) cars.get(0).train().speed());
            buffer.writeVarInt(cars.size());
            for (final AbstractCart<?, ?> car : cars) {
                buffer.writeInt(car.holder().getId());
                buffer.writeInt(writeFlags(car));
                buffer.writeFloat((float) car.progress());
                final Vec3d pos = car.holder().getPos();
                buffer.writeFloat((float) pos.x);
                buffer.writeFloat((float) pos.y);
                buffer.writeFloat((float) pos.z);
            }
        });
    }

    private void sendTrainSyncPacket() {
        for (final ServerPlayerEntity player : PlayerLookup.tracking(this)) {
            if (TrainTrackingUtil.shouldSend(this, player)) {
                final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(player);
                TRAIN_UPDATE.send(connection, this);
            }
        }
    }

    @Override
    public boolean damage(final DamageSource source, final float amount) {
        if (getWorld().isClient || isRemoved()) {
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
        if (getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
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
            cart().speed(velocity.length());
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
        if (nbt.contains(ATTACHMENT_KEY, NbtElement.COMPOUND_TYPE)) {
            final Entity entity = EntityType.loadEntityWithPassengers(nbt.getCompound(ATTACHMENT_KEY), getWorld(), Function.identity());
            final AbstractCart<?, ?> extract = extract(entity);
            if (extract != null) {
                getWorld().spawnEntity(entity);
                attach(extract);
            }
        }
        if (nbt.contains("speed", NbtElement.DOUBLE_TYPE)) {
            cart().train().speed(nbt.getDouble("speed"));
        }
    }

    @Override
    protected boolean canStartRiding(final Entity entity) {
        return false;
    }

    @Override
    protected void writeCustomDataToNbt(final NbtCompound nbt) {
        final AbstractCart<?, ?> attachment = cart().attachment();
        if (attachment != null) {
            final Entity holder = attachment.holder();
            if (holder != null) {
                final NbtCompound sub = new NbtCompound();
                holder.saveSelfNbt(sub);
                nbt.put(ATTACHMENT_KEY, sub);
            }
        }
        final NbtCompound cart = new NbtCompound();
        cart().save(cart);
        nbt.put("cart", cart);
        final AbstractCart<?, ?> attached = cart().attached();
        if (attached == null) {
            nbt.putDouble("speed", cart().train().speed());
        }
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
        final AbstractCart<?, ?> cart = cart();
        final List<? extends AbstractCart<?, ?>> cars = cart.cars();
        if (cars.stream().noneMatch(car -> car.holder().isConnectedThroughVehicle(entity))) {
            if (cart.onRail()) {
                final double massAhead = cart.massAhead();
                final double massBehind = cart.massBehind();
                if (MathUtil.approxEquals(massAhead, massBehind)) {
                    final Vec3d push = cart.position().subtract(entity.getPos());
                    final Vec3d forward = cart.forward();
                    final double dot = push.dotProduct(forward);
                    if (dot >= 0) {
                        cart.addSpeed(Math.copySign(0.05, cart.forwards() ? 1 : -1));
                    } else {
                        cart.addSpeed(Math.copySign(0.05, cart.forwards() ? -1 : 1));
                    }
                } else if (massAhead > massBehind) {
                    cart.addSpeed(Math.copySign(0.05, cart.speed()));
                } else {
                    cart.addSpeed(Math.copySign(0.05, -cart.speed()));
                }
                if (entity instanceof ServerPlayerEntity playerEntity && TrainTrackingUtil.shouldSend(this, playerEntity)) {
                    final ActiveMinecraftConnection connection = CoreMinecraftNetUtil.getConnection(playerEntity);
                    sendSyncPacket(connection);
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
            if (getWorld().isClient) {
                return ActionResult.success(cart().cargo() == null);
            } else {
                if (cart().cargo() != null) {
                    return ActionResult.CONSUME;
                }
                final boolean cargoCheck = cart().cargo(new EntityCargo(player.getUuid()));
                final boolean ridingCheck = player.startRiding(this);
                if (cargoCheck && ridingCheck) {
                    return ActionResult.CONSUME;
                } else {
                    if (cargoCheck) {
                        cart().cargo(null);
                    }
                    if (ridingCheck) {
                        player.stopRiding();
                    }
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
        boolean sent = false;
        if (cart().attached() == null) {
            sendSyncPackets();
            sent = true;
        }
        final boolean sendTrainUpdate = TrainTrackingUtil.shouldSend(this, player);
        if (!sent && sendTrainUpdate) {
            AbstractCart<?, ?> cart = cart();
            while (cart.attached() != null) {
                cart = cart.attached();
            }
            if (cart.holder() instanceof AbstractCartEntity entity) {
                entity.sendSyncPackets();
            }
        }
        if (sendTrainUpdate) {
            TRAIN_UPDATE.send(connection, this);
        }
    }

    @Override
    public void updatePassengerPosition(final Entity passenger, final Entity.PositionUpdater positionUpdater) {
        final Vec3d position = fastPosition(1.0F).add(0, passenger.getHeightOffset(), 0);
        positionUpdater.accept(passenger, position.x, position.y, position.z);
    }

    public MinecartMovementTracker movementTracker() {
        return movementTracker;
    }

    @Override
    public ActionResult previousInteractionAwareInteract(final PlayerEntity player, final Hand hand, final Entity previousInteraction) {
        if (getWorld().isClient) {
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

    protected abstract void applyUpdate(int flags, float progress, Vec3d pos, float speed);

    protected abstract Item item();

    protected abstract void linkAll(List<Entity> holders);

    protected abstract void attach(AbstractCart<?, ?> toAttach);

    public abstract AbstractCart<?, ?> cart();

    protected abstract @Nullable AbstractCart<?, ?> extract(Entity entity);

    protected abstract int writeFlags(final AbstractCart<?, ?> cart);

    protected abstract boolean tryLink(final AbstractCart<?, ?> first, final AbstractCart<?, ?> second, final boolean force);
}
