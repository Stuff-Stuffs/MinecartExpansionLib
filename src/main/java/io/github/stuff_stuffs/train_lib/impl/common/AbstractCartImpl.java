package io.github.stuff_stuffs.train_lib.impl.common;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import io.github.stuff_stuffs.train_lib.api.common.cart.Rail;
import io.github.stuff_stuffs.train_lib.api.common.cart.RailProvider;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.util.MathUtil;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;

public abstract class AbstractCartImpl<T extends Rail<T>, P> implements Cart {
    private static final double OPTIMAL_DISTANCE = 1.25;
    private static final double CART_MASS = 1;
    protected final World world;
    protected final Tracker tracker;
    protected final OffRailHandler offRailHandler;
    protected final Entity holder;
    private final CartPathfinder<T, P> pathfinder;
    private double speed = 0;
    private double progress = 0;
    private Vec3d position = Vec3d.ZERO;
    private Vec3d velocity = Vec3d.ZERO;
    private @Nullable T lastRail = null;
    private @Nullable T currentRail = null;
    private boolean onRail = false;
    private boolean forwards = true;
    private @Nullable AbstractCartImpl<T, P> attached;
    private @Nullable AbstractCartImpl<T, P> attachment;
    private @Nullable Cargo cargo;
    private double massAhead = 0;
    private double massBehind = 0;
    private boolean optimalDirection = false;
    private boolean forwardsAligned = true;

    public AbstractCartImpl(final World world, final Tracker tracker, final OffRailHandler offRailHandler, final Entity holder, final CartPathfinder<T, P> pathfinder) {
        this.world = world;
        this.tracker = tracker;
        this.offRailHandler = offRailHandler;
        this.holder = holder;
        this.pathfinder = pathfinder;
    }

    public void save(final NbtCompound nbt) {
        nbt.putDouble("speed", speed);
        nbt.putDouble("progress", progress);
        nbt.putBoolean("forwardsAligned", forwardsAligned);
        if (cargo != null) {
            final DataResult<NbtElement> result = Cargo.CODEC.encodeStart(NbtOps.INSTANCE, cargo);
            if (result.result().isPresent()) {
                nbt.put("cargo", result.get().orThrow());
            }
        }
    }

    public void load(final NbtCompound nbt) {
        progress = nbt.getDouble("progress");
        speed = nbt.getDouble("speed");
        if (nbt.contains("cargo")) {
            final DataResult<Cargo> cargo = Cargo.CODEC.parse(NbtOps.INSTANCE, nbt.get("cargo"));
            if (cargo.result().isPresent()) {
                this.cargo = cargo.get().orThrow();
            } else {
                this.cargo = null;
            }
        }
        forwards = speed >= 0;
        forwardsAligned = nbt.getBoolean("forwardsAligned");
    }

    public void forwardsAligned(final boolean b) {
        forwardsAligned = b;
    }

    public void tick() {
        if (attached() != null) {
            return;
        }
        Cart minecart = this;
        while (minecart != null) {
            minecart.position(minecart.holder().getPos());
            minecart = minecart.attachment();
        }
        tryMoveOnRail();
        minecart = this;
        while (minecart != null) {
            final Vec3d position = minecart.position();
            minecart.holder().resetPosition();
            minecart.holder().setPosition(position);
            if (((AbstractCartImpl<T, ?>) minecart).onRail) {
                minecart.holder().setVelocity(minecart.velocity());
            }
            minecart = minecart.attachment();
        }
        if (attachment != null && !world.isClient) {
            final CartPathfinder.SwapResult result = pathfinder.swap(this, attachment, world);
            if (result == CartPathfinder.SwapResult.SWAP) {
                swap(speed);
            }
        }
    }

    protected Optional<CartPathfinder.Result> tryFollow(final AbstractCartImpl<T, P> other) {
        final double speed = this.speed;
        this.speed = Math.abs(speed) * (optimalDirection ? 1 : -1);
        forwards = this.speed >= 0;
        final Optional<CartPathfinder.Result> optimalSpeed = pathfinder.find(other, this, OPTIMAL_DISTANCE, world);
        this.speed = speed;
        forwards = this.speed >= 0;
        return optimalSpeed;
    }

    protected abstract P positionFromRail(T rail);

    protected void tryMoveOnRail() {
        int count = 0;
        final AbstractCartImpl<T, P> attached = attached();
        P pos;
        if (currentRail != null) {
            pos = positionFromRail(currentRail);
        } else {
            pos = findOrDefault(position, world);
        }
        if (cargo != null) {
            cargo.tick(this);
        }
        tracker.reset();
        double time = 1.0;
        while (time > 0 && count < TrainLib.MINECART_RECURSION_LIMIT) {
            if (attached != null) {
                final Optional<CartPathfinder.Result> optimalSpeed = attached.tryFollow(this);
                if (optimalSpeed.isEmpty()) {
                    if (!attached.onRail || !onRail) {
                        time = offRailHandler.handle(this, attached, position, time);
                        if (!world.isClient && offRailHandler.shouldDisconnect(this, attached)) {
                            setAttached(null);
                        }
                        currentRail = null;
                        onRail = false;
                        continue;
                    } else if (!world.isClient()) {
                        setAttached(null);
                    }
                } else {
                    optimalDirection = optimalSpeed.get().distance() >= 0;
                    speed = optimalSpeed.get().optimalDistance() * 0.05 + Math.abs(attached.speed) * Math.copySign(1.0, optimalSpeed.get().distance());
                    forwards = speed >= 0;
                }
            } else {
                optimalDirection = forwards;
            }
            final MoveInfo<P> info = tryMove(pos, time);
            if (info == null) {
                time = offRailHandler.handle(this, this.attached, position, time);
                if (!world.isClient && this.attached != null && offRailHandler.shouldDisconnect(this, this.attached)) {
                    setAttached(null);
                }
                onRail = false;
                currentRail = null;
            } else if (info.pos() == null) {
                time = offRailHandler.handle(this, this.attached, position, info.time());
                if (!world.isClient && this.attached != null && offRailHandler.shouldDisconnect(this, this.attached)) {
                    setAttached(null);
                }
                onRail = false;
                currentRail = null;
            } else {
                time = info.time();
                pos = info.pos();
                count++;
                onRail = true;
            }
        }
        if (this.attached != null) {
            speed = this.attached.speed;
        }
        final AbstractCartImpl<T, P> attachment = attachment();
        if (attachment != null) {
            if (attachment.currentRail != null) {
                attachment.tryMoveOnRail();
            } else {
                attachment.tryMoveOnRail();
            }
        }
    }

    protected abstract Either<MoveInfo<P>, Pair<P, RailProvider.NextRailInfo<T>>> next(P pos, Direction exitDirection, double time);

    protected abstract P findOrDefault(Vec3d position, World world);

    protected @Nullable MoveInfo<P> tryMove(final P pos, final double timeRemaining) {
        final RailProvider<T> provider = tryGetProvider(pos);
        if (provider == null) {
            lastRail = null;
            forwards = true;
            progress = 0;
            forwardsAligned = true;
            return null;
        }
        if (lastRail == null) {
            final RailProvider.NextRailInfo<T> info = provider.snap(this);
            if (info != null) {
                lastRail = info.rail();
                progress = info.progress();
                forwards = info.forwards();
                forwardsAligned = true;
            } else {
                return null;
            }
        }
        final T rail = provider.currentRail(this);
        if (rail == null) {
            lastRail = null;
            forwards = true;
            forwardsAligned = true;
            return null;
        }
        currentRail = rail;
        final double length = rail.length();
        final double m = applyVelocityModifier(rail, timeRemaining);
        double speed = speed();
        final double maxMove = speed * m;
        final double oldProgress = progress;
        if ((forwards && progress + maxMove > length) || (!forwards && progress + maxMove < 0)) {
            final double overflow;
            if (speed == 0.0) {
                overflow = maxMove;
            } else if (forwards) {
                overflow = (progress + maxMove - length) / speed;
            } else {
                overflow = (progress + maxMove) / speed;
            }
            lastRail = rail;
            RailProvider.NextRailInfo<T> railInfo = provider.next(this, lastRail, null);
            P nextPos = pos;
            if (railInfo == null) {
                final Either<MoveInfo<P>, Pair<P, RailProvider.NextRailInfo<T>>> next = next(pos, forwards ? currentRail.exitDirection() : currentRail.entranceDirection(), overflow);
                final Optional<MoveInfo<P>> left = next.left();
                if (left.isPresent()) {
                    return left.get();
                } else {
                    final Pair<P, RailProvider.NextRailInfo<T>> pair = next.right().get();
                    nextPos = pair.getFirst();
                    railInfo = pair.getSecond();
                }
            }
            position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
            velocity = rail.tangent(forwards ? length : 0).multiply(speed);
            speed = speed();
            rail.onRail(this, oldProgress, progress, m - overflow);
            position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
            final Vec3d tangent = rail.tangent(forwards ? length : 0);
            velocity = tangent.multiply(this.speed);
            if (cargo != null) {
                cargo.onRail(rail, this, m - overflow);
            }
            if (railInfo.forwards()) {
                this.speed = Math.abs(speed);
            } else {
                this.speed = -Math.abs(speed);
            }
            currentRail.onExit(this);
            currentRail = railInfo.rail();
            currentRail.onEnter(this);
            progress = railInfo.progress();
            if (forwards ^ railInfo.forwards()) {
                forwardsAligned = !forwardsAligned;
            }
            forwards = railInfo.forwards();
            if (attached != null) {
                applyVelocityModifierTrain(rail, Math.max(overflow, MathUtil.EPS));
            }
            tracker.onMove(position(), tangent.multiply(forwardsAligned ? 1 : -1), Rail.DEFAULT_UP, 1 - (timeRemaining - (m - overflow) * 0.5));
            return new MoveInfo<>(Math.max(overflow, MathUtil.EPS), nextPos);
        }
        progress = progress + maxMove;
        if (cargo != null) {
            cargo.onRail(rail, this, m);
        }
        velocity = rail.tangent(progress).multiply(this.speed);
        position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
        rail.onRail(this, oldProgress, progress, m);
        final Vec3d tangent = rail.tangent(progress);
        velocity = tangent.multiply(this.speed);
        position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
        tracker.onMove(position(), tangent.multiply(forwardsAligned ? 1 : -1), Rail.DEFAULT_UP, 1 - (timeRemaining - m));
        if (attached != null) {
            applyVelocityModifierTrain(rail, Math.max(m, MathUtil.EPS));
        }
        return new MoveInfo<>(timeRemaining - m, pos);
    }

    protected void applyVelocityModifierTrain(final Rail<T> rail, final double duration) {
        applyGravityTrain(rail, duration);
        final double friction = MathHelper.clamp(rail.friction(this, progress), 0, 1);
        AbstractCartImpl<T, ?> minecart = this;
        while (minecart.attached != null) {
            minecart = minecart.attached;
        }
        minecart.addSpeed(-minecart.speed * (friction * duration * duration * 0.5));
    }

    protected double applyVelocityModifier(final Rail<T> rail, final double duration) {
        if (attached() != null) {
            return duration;
        }
        final double t = applyGravity(rail, duration);
        final double speed = speed();
        final double friction = MathHelper.clamp(rail.friction(this, progress), 0, 1);
        addSpeed(-speed * friction * t * t * 0.5);
        return t;
    }

    protected void addSpeedTrain(final double amount) {
        if (amount == 0) {
            return;
        }
        AbstractCartImpl<T, P> minecart = this;
        while (minecart.attached() != null) {
            minecart = minecart.attached();
        }
        final OptionalDouble speed = pathfinder.trainAddSpeed(this, attached, minecart, minecart.attachment, amount, world);
        if (speed.isPresent()) {
            minecart.addSpeed(speed.getAsDouble());
        }
    }

    protected void applyGravityTrain(final Rail<T> rail, final double duration) {
        final double angle = rail.slopeAngle();
        final double gravity = 0.04 * angle;
        addSpeedTrain(-gravity * duration * duration * 0.5);
    }

    protected double applyGravity(final Rail<T> rail, double duration) {
        final double angle = rail.slopeAngle();
        final double gravity = 0.04 * angle;
        final double speed = speed();
        if ((angle > 0 && speed > 0) || (angle < 0 && speed < 0) || MathUtil.approxEquals(speed, 0)) {
            final double massFraction = mass() / (mass() + massAhead() + massBehind());
            final double flipTime = MathUtil.SQRT_2 * Math.sqrt(Math.abs(speed)) * (1 / (Math.sqrt(Math.abs(gravity)) * Math.sqrt(massFraction))) + MathUtil.EPS;
            if (flipTime < duration) {
                duration = flipTime;
            }
        }
        addSpeed(-gravity * duration * duration * 0.5);
        return duration;
    }

    @Override
    public @Nullable AbstractCartImpl<T, P> attached() {
        return attached;
    }

    @Override
    public @Nullable AbstractCartImpl<T, P> attachment() {
        return attachment;
    }

    @Override
    public double mass() {
        return CART_MASS + (cargo != null ? cargo.mass() : 0);
    }

    @Override
    public double massBehind() {
        return massBehind;
    }

    @Override
    public double massAhead() {
        return massAhead;
    }

    @Override
    public boolean forwardsAligned() {
        return forwardsAligned;
    }

    public boolean forwards() {
        return forwards;
    }

    public boolean optimalDirection() {
        return optimalDirection;
    }

    public List<AbstractCartImpl<T, ?>> cars() {
        if (attached != null) {
            return attached.cars();
        }
        final List<AbstractCartImpl<T, ?>> cars = new ArrayList<>();
        AbstractCartImpl<T, ?> m = this;
        while (m != null) {
            cars.add(m);
            m = m.attachment;
        }
        return cars;
    }

    private void updateBehindMass(final double value) {
        massBehind = value;
        if (attached != null && attached.attachment == this) {
            attached.updateBehindMass(value + mass());
        }
    }

    private void updateAheadMass(final double value) {
        massAhead = value;
        if (attachment != null && attachment.attached == this) {
            attachment.updateAheadMass(value + mass());
        }
    }

    public boolean setAttached(@Nullable final AbstractCartImpl<T, P> attached) {
        return setAttached(attached, (stack, minecart) -> ItemScatterer.spawn(world, minecart.position().x, minecart.position().y, minecart.position().z, stack));
    }

    public boolean setAttached(final @Nullable AbstractCartImpl<T, P> attached, final BiConsumer<ItemStack, ? super Cart> dropper) {
        if (attached != null) {
            if (cars().contains(attached)) {
                return false;
            }
        }
        if (this.attached != null) {
            if (this.attached.attachment == this) {
                this.attached.attachment = null;
                this.attached.updateBehindMass(0);
                dropper.accept(new ItemStack(Blocks.CHAIN), this);
            }
        }
        this.attached = attached;
        if (this.attached != null && this.attached.attachment != null) {
            if (!this.attached.attachment.setAttached(null, dropper)) {
                throw new RuntimeException();
            }
        }
        if (this.attached != null) {
            this.attached.attachment = this;
            updateAheadMass(attached.massAhead + attached.mass());
            attached.updateBehindMass(massBehind + mass());
        } else {
            updateAheadMass(0);
        }
        tracker.onAttachedChange();
        return true;
    }

    public boolean onRail() {
        return onRail;
    }

    public T currentRail() {
        return currentRail;
    }

    @Override
    public void speed(final double speed) {
        if (attached() == null) {
            final double capped = Math.min(Math.abs(speed), TrainLib.MAX_SPEED) * Math.signum(speed);
            final boolean old = forwards;
            forwards = capped >= 0;
            this.speed = capped;
            if (old ^ forwards) {
                forwardsAligned = !forwardsAligned;
            }
        }
    }

    private void swap(final double speed) {
        if (attachment != null) {
            this.speed = Math.copySign(this.speed, optimalDirection ? -1 : 1);
            optimalDirection = !optimalDirection;
            forwards = this.speed >= 0;
            attachment.swap(speed);
        } else {
            final Optional<CartPathfinder.Result> result = pathfinder.find(attached, this, 0.0, world);
            if (result.isPresent()) {
                this.speed = Math.copySign(1.0, result.get().distance()) * Math.abs(speed);
            } else {
                this.speed = Math.copySign(1.0, this.speed) * -Math.abs(speed);
            }
            final boolean old = forwards;
            forwards = this.speed >= 0;
            if (old ^ forwards) {
                optimalDirection = !optimalDirection;
                forwardsAligned = !forwardsAligned;
            }
        }
        final AbstractCartImpl<T, P> tmp = attached;
        attached = attachment;
        attachment = tmp;
        final double tmpMass = massAhead;
        massAhead = massBehind;
        massBehind = tmpMass;
        tracker.onAttachedChange();
    }

    @Override
    public void progress(final double progress) {
        this.progress = progress;
    }

    @Override
    public void cargo(final Cargo cargo) {
        this.cargo = cargo;
        updateAheadMass(massAhead);
        updateBehindMass(massBehind);
        tracker.onCargoChange();
    }

    @Override
    public void addSpeed(final double speed) {
        if (attached != null) {
            addSpeedTrain(speed);
        } else {
            addSpeed0(speed);
        }
    }

    @Override
    public Entity holder() {
        return holder;
    }

    public void addSpeed0(final double speed) {
        speed(this.speed + (speed * mass()) / (mass() + massAhead() + massBehind()));
    }


    @Override
    public double speed() {
        return speed;
    }

    @Override
    public double progress() {
        return progress;
    }

    protected abstract @Nullable RailProvider<T> tryGetProvider(P pos);

    @Override
    public void position(final Vec3d position) {
        this.position = position;
        currentRail = null;
        onRail = false;
        final RailProvider<T> provider = tryGetProvider(findOrDefault(position, world));
        if (provider == null) {
            return;
        }
        final RailProvider.NextRailInfo<T> snap = provider.snap(this);
        if (snap == null) {
            return;
        }
        currentRail = snap.rail();
        progress = snap.progress();
        onRail = true;
        currentRail.onEnter(this);
    }

    @Override
    public Vec3d position() {
        return position;
    }

    @Override
    public @Nullable Cargo cargo() {
        return cargo;
    }

    @Override
    public Vec3d velocity() {
        return velocity;
    }

    public record MoveInfo<P>(double time, @Nullable P pos) {
    }
}
