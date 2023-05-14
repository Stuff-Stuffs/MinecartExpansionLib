package io.github.stuff_stuffs.train_lib.api.common.cart;

import com.mojang.serialization.DataResult;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRailProvider;
import io.github.stuff_stuffs.train_lib.api.common.event.CartEvent;
import io.github.stuff_stuffs.train_lib.api.common.event.CartEventEmitter;
import io.github.stuff_stuffs.train_lib.api.common.util.MathUtil;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfig;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class AbstractCart<T extends Rail<T>, P> implements Cart {
    protected final World world;
    protected final Tracker tracker;
    protected final OffRailHandler offRailHandler;
    protected final Entity holder;
    private final CartType<T, P> type;
    private final CartEventEmitter emitter;
    private double progress;
    private Vec3d position = Vec3d.ZERO;
    private Vec3d velocity = Vec3d.ZERO;
    private @Nullable T lastRail = null;
    private @Nullable T currentRail = null;
    private @Nullable Cargo cargo;
    private boolean inverted = false;
    private Train<T, P> train;
    private boolean destroyed = false;

    public AbstractCart(final World world, final Tracker tracker, final OffRailHandler offRailHandler, final Entity holder, final CartType<T, P> type) {
        this.world = world;
        this.tracker = tracker;
        this.offRailHandler = offRailHandler;
        this.holder = holder;
        this.type = type;
        emitter = CartEventEmitter.create(world);
        train = new Train<>(this);
    }

    protected abstract P positionFromRail(T rail);

    protected abstract Optional<RailProvider.NextRailInfo<T>> next(P pos, Direction exitDirection, double time, boolean forwards);

    protected abstract P findOrDefault(Vec3d position, World world);

    protected abstract double emptyCartMass();

    @Override
    public abstract double bufferSpace();

    public abstract BlockPos currentPosition(T currentRail);

    protected abstract @Nullable RailProvider<T> tryGetProvider(P pos);

    public int randomOffset() {
        return train.carts.get(0).holder.getId();
    }

    @Override
    public CartType<T, P> type() {
        return type;
    }

    public void save(final NbtCompound nbt) {
        if (cargo != null) {
            final DataResult<NbtElement> result = Cargo.CODEC.encodeStart(NbtOps.INSTANCE, cargo);
            if (result.result().isPresent()) {
                nbt.put("cargo", result.get().orThrow());
            }
        }
    }

    public void load(final NbtCompound nbt) {
        if (nbt.contains("cargo")) {
            final DataResult<Cargo> cargo = Cargo.CODEC.parse(NbtOps.INSTANCE, nbt.get("cargo"));
            if (cargo.result().isPresent()) {
                this.cargo = cargo.get().orThrow();
            } else {
                this.cargo = null;
            }
        }
    }

    public void tick() {
        if (train.carts.get(0) != this) {
            return;
        }
        train.tick();
    }

    public void destroy() {
        if (!destroyed) {
            train.remove(this);
            train = null;
            destroyed = true;
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new IllegalStateException();
        }
    }

    public void inverted(final boolean b) {
        inverted = b;
    }

    @Override
    public @Nullable AbstractCart<T, P> attached() {
        checkDestroyed();
        return train.attached(this);
    }

    @Override
    public @Nullable AbstractCart<T, P> attachment() {
        checkDestroyed();
        return train.attachment(this);
    }

    @Override
    public double mass() {
        checkDestroyed();
        return emptyCartMass() + (cargo != null ? cargo.mass() : 0);
    }

    public boolean inverted() {
        checkDestroyed();
        return inverted;
    }

    public List<AbstractCart<T, ?>> cars() {
        checkDestroyed();
        return new ArrayList<>(train.carts);
    }

    public boolean onRail() {
        checkDestroyed();
        return currentRail != null;
    }

    public T currentRail() {
        checkDestroyed();
        return currentRail;
    }

    @Override
    public void speed(final double speed) {
        checkDestroyed();
        train.speed(this, speed);
    }

    @Override
    public void progress(final double progress) {
        checkDestroyed();
        this.progress = progress;
    }

    @Override
    public boolean cargo(final @Nullable Cargo cargo) {
        checkDestroyed();
        if (cargo != null && !cargo.type().check(type)) {
            return false;
        }
        this.cargo = cargo;
        tracker.onCargoChange();
        return true;
    }

    @Override
    public void addSpeed(final double speed) {
        checkDestroyed();
        train.addSpeed(this, speed);
    }

    @Override
    public Entity holder() {
        return holder;
    }

    public boolean forwards() {
        checkDestroyed();
        if (train.speed == 0.0) {
            return !inverted;
        }
        return train.speed * (inverted ? -1 : 1) >= 0;
    }

    @Override
    public double speed() {
        checkDestroyed();
        return train.speed * (inverted ? -1 : 1);
    }

    @Override
    public double progress() {
        checkDestroyed();
        return progress;
    }

    @Override
    public void position(final Vec3d position) {
        checkDestroyed();
        this.position = position;
        emitter.setPos(position);
        if (currentRail != null) {
            currentRail.onExit(this);
        }
        currentRail = null;
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
        this.position = currentRail.position(progress);
        emitter.setPos(position);
        currentRail.onEnter(this);
    }

    @Override
    public Vec3d position() {
        checkDestroyed();
        return position;
    }

    @Override
    public @Nullable Cargo cargo() {
        return cargo;
    }

    @Override
    public Vec3d velocity() {
        checkDestroyed();
        return velocity;
    }

    @Nullable
    private MoveInfo<P> tryMove(final P pos, final double timeRemaining, @Nullable final AbstractCart<T, P> following) {
        checkDestroyed();
        final RailProvider<T> provider = tryGetProvider(pos);
        if (provider == null) {
            progress = 0;
            return null;
        }
        if (lastRail == null) {
            final RailProvider.NextRailInfo<T> info = provider.snap(this);
            if (info != null) {
                lastRail = info.rail();
                progress = info.progress();
            } else {
                return null;
            }
        }
        final T rail = provider.currentRail(this);
        if (rail == null) {
            return null;
        }
        currentRail = rail;
        final double length = rail.length();
        final double m = train.applyVelocityModifier(this, rail, timeRemaining);
        double d = 0;
        if (following != null) {
            final double bufferSpace = bufferSpace() + following.bufferSpace();
            final Optional<CartPathfinder.Result> result = type.pathfinder().find(this, following, bufferSpace + Math.abs(train.speed) * timeRemaining, world);
            if (result.isPresent()) {
                d = result.get().optimalDistance();
            }
        }
        final double speed = train.speed * (inverted ? -1 : 1) + d * 0.1;
        final double maxMove = speed * m;
        final double oldProgress = progress;
        if ((progress + maxMove > length) || (progress + maxMove < 0)) {
            final boolean forwards = speed >= 0;
            final double overflow;
            if (forwards) {
                overflow = (progress + maxMove - length) / speed;
            } else {
                overflow = (progress + maxMove) / speed;
            }
            lastRail = rail;
            MinecartRailProvider.NextRailInfo<T> railInfo = provider.next(this, lastRail, null);
            if (railInfo == null) {
                final Optional<RailProvider.NextRailInfo<T>> next = next(pos, forwards ? rail.exitDirection() : rail.entranceDirection(), overflow, forwards);
                if (next.isEmpty()) {
                    return new MoveInfo<>(overflow, null);
                }
                railInfo = next.get();
            }
            final P nextPos = positionFromRail(railInfo.rail());
            position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
            rail.onRail(this, oldProgress, progress, m - overflow);
            position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
            emitter.setPos(position);
            currentRail.onExit(this);
            if (cargo != null) {
                cargo.onRail(rail, this, m - overflow);
            }
            currentRail = railInfo.rail();
            currentRail.onEnter(this);
            if (speed() >= 0 ^ railInfo.forwards()) {
                inverted = !inverted;
            }
            progress = railInfo.progress();
            final Vec3d velocity = rail.tangent(progress).multiply(speed());
            this.velocity = velocity;
            holder.setVelocity(velocity);
            return new MoveInfo<>(Math.max(overflow, MathUtil.EPS), nextPos);
        }
        progress = progress + maxMove;
        position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
        rail.onRail(this, oldProgress, progress, m);
        position = rail.position(Math.min(Math.max(progress, MathUtil.EPS), length - MathUtil.EPS));
        emitter.setPos(position);
        tracker.onMove(position, rail.tangent(progress), MinecartRail.DEFAULT_UP, 1 - (timeRemaining - m));
        final Vec3d velocity = rail.tangent(progress).multiply(speed());
        this.velocity = velocity;
        holder.setVelocity(velocity);
        if (cargo != null) {
            cargo.onRail(rail, this, m);
        }
        return new MoveInfo<>(timeRemaining - m, pos);
    }

    public Train<T, P> train() {
        return train;
    }

    public void linkAll(final List<? extends AbstractCart<T, P>> carts) {
        if (!world.isClient) {
            throw new IllegalStateException();
        }
        for (final AbstractCart<T, P> cart : carts) {
            cart.train.remove(cart);
        }
        final Train<T, P> train = new Train<>(carts, this.train.speed);
        for (final AbstractCart<T, P> cart : carts) {
            cart.train = train;
        }
    }

    public record MoveInfo<P>(double time, @Nullable P pos) {
    }

    public static final class Train<T extends Rail<T>, P> {
        private final CartType<T, P> type;
        private final List<AbstractCart<T, P>> carts = new ArrayList<>();
        private double mass = 1.0;
        private double speed = 0.0;

        public Train(final AbstractCart<T, P> cart) {
            this.type = cart.type();
            carts.add(cart);
            updateMass();
        }

        public Train(final List<? extends AbstractCart<T, P>> carts, final double speed) {
            this.type = carts.get(0).type();
            for (final AbstractCart<T, P> cart : carts) {
                if (cart.type() != type) {
                    throw new IllegalArgumentException();
                }
            }
            this.carts.addAll(carts);
            updateMass();
            this.speed = speed;
        }

        public void updateMass() {
            mass = 0.0;
            for (final AbstractCart<T, P> cart : carts) {
                mass += cart.mass();
            }
        }

        public boolean link(final AbstractCart<T, P> other, final boolean force) {
            if (other.train == this) {
                return true;
            }
            if (other.type != this.type) {
                return false;
            }
            if (carts.size() + other.train.carts.size() > TrainLib.CONFIG.maxTrainSize()) {
                return false;
            }
            if (force) {
                carts.addAll(other.train.carts);
                for (final AbstractCart<T, P> cart : other.train.carts) {
                    cart.train = this;
                }
                speed = Math.min(Math.abs(other.speed()), Math.abs(speed));
                updateMass();
                for (final AbstractCart<T, P> cart : other.train.carts) {
                    cart.tracker.trainChange();
                }
                return true;
            } else {
                final int size = carts.size();
                final int otherSize = other.train.carts.size();
                if (size == 1 && otherSize == 1) {
                    return linkSingleSingle(carts.get(0), other);
                } else if (size == 1) {
                    return linkSingleMulti(other.train, carts.get(0));
                } else if (otherSize == 1) {
                    return linkSingleMulti(this, other);
                } else {
                    return linkMultiMulti(this, other.train);
                }
            }
        }

        private static <T extends Rail<T>, P> boolean linkSingleSingle(final AbstractCart<T, P> first, final AbstractCart<T, P> second) {
            first.train.carts.addAll(second.train.carts);
            for (final AbstractCart<T, P> cart : second.train.carts) {
                cart.train = first.train;
            }
            first.train.speed = 0;
            first.train.updateMass();
            for (final AbstractCart<T, P> cart : second.train.carts) {
                cart.tracker.trainChange();
            }
            return true;
        }

        private static <T extends Rail<T>, P> void link(final Train<T, P> train, final List<? extends AbstractCart<T, P>> carts, final boolean insert) {
            if (insert) {
                train.carts.addAll(0, carts);

            } else {
                train.carts.addAll(carts);
            }
            for (final AbstractCart<T, P> cart : carts) {
                cart.train = train;
            }
            for (final AbstractCart<T, P> cart : carts) {
                cart.tracker.trainChange();
            }
            train.updateMass();
            train.speed = 0;
        }

        private static <T extends Rail<T>, P> boolean linkSingleMulti(final Train<T, P> train, final AbstractCart<T, P> cart) {
            final CartPathfinder<T, P> pathfinder = train.type.pathfinder();
            final Optional<CartPathfinder.Result> frontDist = pathfinder.find(train.carts.get(0), cart, 0.0, cart.world);
            final Optional<CartPathfinder.Result> backDist = pathfinder.find(train.carts.get(train.carts.size() - 1), cart, 0.0, cart.world);
            if (frontDist.isEmpty() && backDist.isEmpty()) {
                return false;
            }
            if (frontDist.isEmpty() || (backDist.isPresent() && Math.abs(frontDist.get().distance()) < Math.abs(backDist.get().distance()))) {
                link(train, List.of(cart), true);
                return true;
            }
            link(train, List.of(cart), false);
            return true;
        }

        private static <T extends Rail<T>, P> boolean linkMultiMulti(final Train<T, P> first, final Train<T, P> second) {
            final AbstractCart<T, P> firstFront = first.carts.get(0);
            final AbstractCart<T, P> firstBack = first.carts.get(first.carts.size() - 1);
            final AbstractCart<T, P> secondFront = second.carts.get(0);
            final AbstractCart<T, P> secondBack = second.carts.get(second.carts.size() - 1);
            final CartPathfinder<T, P> pathfinder = first.type.pathfinder();
            final World world = firstFront.world;
            final Optional<CartPathfinder.Result> frontFront = pathfinder.find(firstFront, secondFront, 0.0, world);
            final Optional<CartPathfinder.Result> frontBack = pathfinder.find(firstFront, secondBack, 0.0, world);
            final Optional<CartPathfinder.Result> backFront = pathfinder.find(firstBack, secondFront, 0.0, world);
            final Optional<CartPathfinder.Result> backBack = pathfinder.find(firstBack, secondBack, 0.0, world);
            boolean insert = false;
            boolean flipSecond = false;
            double best = Double.POSITIVE_INFINITY;

            if (frontFront.isPresent() && Math.abs(frontFront.get().distance()) < best) {
                best = Math.abs(frontFront.get().distance());
                insert = true;
                flipSecond = true;
            }

            if (frontBack.isPresent() && Math.abs(frontBack.get().distance()) < best) {
                best = Math.abs(frontBack.get().distance());
                insert = true;
                flipSecond = false;
            }

            if (backFront.isPresent() && Math.abs(backFront.get().distance()) < best) {
                best = Math.abs(backFront.get().distance());
                insert = false;
                flipSecond = false;
            }

            if (backBack.isPresent() && Math.abs(backBack.get().distance()) < best) {
                best = Math.abs(backBack.get().distance());
                insert = false;
                flipSecond = true;
            }

            if (best == Double.POSITIVE_INFINITY) {
                return false;
            }

            final List<AbstractCart<T, P>> carts = new ArrayList<>(second.carts);
            if (flipSecond) {
                Collections.reverse(carts);
            }
            link(first, carts, insert);
            return true;
        }

        public void remove(final AbstractCart<T, P> cart) {
            final int index = carts.indexOf(cart);
            if (index == -1) {
                return;
            }
            final List<AbstractCart<T, P>> first = carts.subList(0, index);
            if (!first.isEmpty()) {
                final Train<T, P> train = new Train<>(first, speed);
                for (final AbstractCart<T, P> firstSplitCart : first) {
                    firstSplitCart.train = train;
                }
                for (final AbstractCart<T, P> c : train.carts) {
                    c.tracker.trainChange();
                }
            }
            final int size = carts.size();
            if (index + 1 < size) {
                final List<AbstractCart<T, P>> second = carts.subList(index + 1, size);
                if (!second.isEmpty()) {
                    final Train<T, P> train = new Train<>(second, speed);
                    for (final AbstractCart<T, P> secondSplitCart : second) {
                        secondSplitCart.train = train;
                    }
                    for (final AbstractCart<T, P> c : train.carts) {
                        c.tracker.trainChange();
                    }
                }
            }
            for (final AbstractCart<T, P> c : carts) {
                c.tracker.trainChange();
            }
            cart.train = new Train<>(cart);
        }

        public double speed() {
            return speed;
        }

        public void speed(final double speed) {
            this.speed = speed;
        }

        public void addSpeed(final AbstractCart<T, P> cart, double speed) {
            speed = this.speed + (cart.inverted ? -1 : 1) * speed * cart.mass() / mass;
            this.speed = Math.copySign(Math.min(Math.abs(speed), TrainLib.CONFIG.maxSpeed()), speed);
        }

        public void speed(final AbstractCart<T, P> cart, final double speed) {
            this.speed = (cart.inverted ? -1 : 1) * speed;
        }

        private boolean resetForwardsEnd(final AbstractCart<T, P> end, final AbstractCart<T, P> previous) {
            final CartPathfinder.SwapResult swap = end.type.pathfinder().swap(end, previous, end.world);
            if (swap == CartPathfinder.SwapResult.SWAP) {
                end.inverted = !end.inverted;
            }
            return swap != CartPathfinder.SwapResult.BROKEN;
        }

        private boolean resetForwards(final AbstractCart<T, P> front, final AbstractCart<T, P> back) {
            final CartPathfinder.SwapResult swap = front.type.pathfinder().swap(back, front, front.world);
            if (swap == CartPathfinder.SwapResult.OK) {
                back.inverted = !back.inverted;
            }
            return swap != CartPathfinder.SwapResult.BROKEN;
        }

        public IntSet resetForwards() {
            final int size = carts.size();
            if (size == 1) {
                return IntSet.of();
            }
            final IntSet breaks = new IntOpenHashSet();
            if (speed >= 0) {
                for (int i = 0; i < size; i++) {
                    if (i == 0) {
                        final AbstractCart<T, P> prev = carts.get(i);
                        final AbstractCart<T, P> current = carts.get(i + 1);
                        if (!resetForwardsEnd(prev, current) && current.onRail() && prev.onRail()) {
                            breaks.add(i);
                        }
                    } else {
                        final AbstractCart<T, P> prev = carts.get(i - 1);
                        final AbstractCart<T, P> current = carts.get(i);
                        if (!resetForwards(prev, current) && current.onRail() && prev.onRail()) {
                            breaks.add(i - 1);
                        }
                    }
                }
            } else {
                for (int i = size - 1; i >= 0; i--) {
                    if (i == size - 1) {
                        final AbstractCart<T, P> prev = carts.get(i);
                        final AbstractCart<T, P> current = carts.get(i - 1);
                        if (!resetForwardsEnd(prev, current) && current.onRail() && prev.onRail()) {
                            breaks.add(i - 1);
                        }
                    } else {
                        final AbstractCart<T, P> prev = carts.get(i + 1);
                        final AbstractCart<T, P> current = carts.get(i);
                        if (!resetForwards(prev, current) && current.onRail() && prev.onRail()) {
                            breaks.add(i);
                        }
                    }
                }
            }
            return breaks;
        }

        public void tick() {
            if (Math.abs(speed) < Float.MIN_VALUE) {
                speed = 0;
            }
            carts.removeIf(AbstractCart::isDestroyed);
            IntSet breaks = resetForwards();
            for (final AbstractCart<T, P> cart : carts) {
                cart.position(cart.holder.getPos());
            }
            final IntSet offRailBreaks = new IntOpenHashSet();
            final int size = carts.size();
            if (speed >= 0) {
                for (int i = 0; i < size; i++) {
                    final AbstractCart<T, P> cart = carts.get(i);
                    final AbstractCart<T, P> following = i == 0 ? null : carts.get(i - 1);
                    cart.tracker.reset();
                    move(cart, following);
                    if (following != null && !cart.onRail() && cart.offRailHandler.shouldDisconnect(cart, following)) {
                        offRailBreaks.add(i - 1);
                    }
                }
            } else {
                for (int i = size - 1; i >= 0; i--) {
                    final AbstractCart<T, P> cart = carts.get(i);
                    final AbstractCart<T, P> following = i == size - 1 ? null : carts.get(i + 1);
                    cart.tracker.reset();
                    move(cart, following);
                    if (following != null && !cart.onRail() && cart.offRailHandler.shouldDisconnect(cart, following)) {
                        offRailBreaks.add(i);
                    }
                }
            }
            for (final AbstractCart<T, P> cart : carts) {
                cart.holder.setPosition(cart.position);
                cart.holder.resetPosition();
                if (cart.onRail()) {
                    cart.holder.setVelocity(cart.velocity);
                }
            }
            if (!carts.get(0).world.isClient && !breaks.isEmpty() || !offRailBreaks.isEmpty()) {
                //check that it hasn't been fixed by movement
                breaks = resetForwards();
                if (!breaks.isEmpty() || !offRailBreaks.isEmpty()) {
                    final IntSortedSet sorted = new IntRBTreeSet(breaks);
                    sorted.addAll(offRailBreaks);
                    split(this, sorted);
                }
            }
        }

        private void move(final AbstractCart<T, P> cart, @Nullable final AbstractCart<T, P> following) {
            double time = 1.0;
            int count = 0;
            P pos = cart.currentRail == null ? cart.findOrDefault(cart.position, cart.world) : cart.positionFromRail(cart.currentRail);
            if (cart.currentRail != null) {
                cart.emitter.emit(new CartEvent.RailOccupied(cart.currentPosition(cart.currentRail), cart.currentRail.id(), cart));
            }
            if (cart.cargo != null) {
                cart.cargo.tick(cart);
            }
            final int recursionLimit = TrainLib.CONFIG.maxRecursion();
            while (time > 0.0 && count < recursionLimit) {
                final MoveInfo<P> info = cart.tryMove(pos, time, following);
                if (info == null) {
                    cart.offRailHandler.handle(cart, following, cart.position, time);
                    offRailTrack(cart, time);
                    break;
                }
                if (info.pos == null) {
                    cart.offRailHandler.handle(cart, following, cart.position, info.time);
                    offRailTrack(cart, info.time);
                    break;
                }
                pos = info.pos;
                time = info.time;
                if (time > 0) {
                    cart.emitter.emit(new CartEvent.RailOccupied(cart.currentPosition(cart.currentRail), cart.currentRail.id(), cart));
                }
                count++;
            }
        }

        private void offRailTrack(final AbstractCart<T, P> cart, final double time) {
            final Vec3d velocity = cart.holder.getVelocity();
            final Vec3d tangent;
            if (velocity.lengthSquared() < MathUtil.EPS) {
                tangent = new Vec3d(1, 0, 0);
            } else {
                tangent = velocity.normalize();
            }
            cart.tracker.onMove(cart.position, tangent, Rail.DEFAULT_UP, time);
        }

        public double applyVelocityModifier(final AbstractCart<T, P> cart, final T rail, final double remaining) {
            final double factor = cart.mass() / mass;
            final TrainLibConfig config = TrainLib.CONFIG;
            speed = speed - speed * MathHelper.clamp(factor * (rail.friction(cart, cart.progress) * remaining * remaining * 0.5), 0, 1);
            final double gravity = config.gravity();
            final double angle = rail.slopeAngle() * (cart.inverted ? -1 : 1) * gravity;
            speed = speed - factor * (remaining * remaining * 0.5 * angle);
            return remaining;
        }

        public @Nullable AbstractCart<T, P> attached(final AbstractCart<T, P> cart) {
            return nextCart(cart, true);
        }

        public @Nullable AbstractCart<T, P> attachment(final AbstractCart<T, P> cart) {
            return nextCart(cart, false);
        }

        private @Nullable AbstractCart<T, P> nextCart(final AbstractCart<T, P> cart, final boolean reverse) {
            final int index = carts.indexOf(cart);
            if (index == -1) {
                return null;
            }
            if (speed < 0 ^ reverse) {
                if (index - 1 >= 0) {
                    return carts.get(index - 1);
                }
            } else {
                if (index + 1 < carts.size()) {
                    return carts.get(index + 1);
                }
            }
            return null;
        }

        private static <T extends Rail<T>, P> void split(final Train<T, P> train, final IntSortedSet breaks) {
            final IntBidirectionalIterator iterator = breaks.iterator();
            int last = -1;
            int first = -1;
            while (iterator.hasNext()) {
                final int current = iterator.nextInt();
                if (first == -1) {
                    first = current;
                }
                final List<AbstractCart<T, P>> carts = train.carts.subList(last + 1, current + 1);
                final Train<T, P> section = new Train<>(carts, train.speed);
                for (final AbstractCart<T, P> cart : carts) {
                    cart.train = section;
                }
                for (final AbstractCart<T, P> cart : carts) {
                    cart.tracker.trainChange();
                }
                last = current;
            }
            if (first != -1) {
                train.carts.subList(first + 1, train.carts.size()).clear();
            }
        }
    }
}
