package io.github.stuff_stuffs.train_lib.api.common.cart;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.train_lib.api.common.TrainLibApi;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRailProvider;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.IntSupplier;

public abstract class CartPathfinder<T extends Rail<T>, P> {
    public static final CartPathfinder<MinecartRail, BlockPos> MINECART_PATHFINDER = new CartPathfinder<>(TrainLib.MINECART_BEHAVIOUR_MIRROR::maxPathfindingLimit) {
        @Override
        protected BlockPos extract(final MinecartRail rail) {
            return rail.railPosition();
        }

        @Override
        protected @Nullable Pair<BlockPos, RailProvider<MinecartRail>> next(final MinecartRail rail, final boolean forwards, final World world) {
            BlockPos next = forwards ? rail.exitPosition() : rail.entrancePosition();
            MinecartRailProvider provider = TrainLibApi.MINECART_RAIL_BLOCK_API.find(world, next, null);
            if (provider == null) {
                next = next.down();
                provider = TrainLibApi.MINECART_RAIL_BLOCK_API.find(world, next, null);
            }
            return provider == null ? null : Pair.of(next, provider);
        }
    };
    private final IntSupplier recursionLimit;

    protected CartPathfinder(final IntSupplier limit) {
        recursionLimit = limit;
    }

    public SwapResult swapStart(final AbstractCart<T, P> start, final AbstractCart<T, P> following, final World world) {
        if (!start.onRail() || !following.onRail()) {
            return SwapResult.BROKEN;
        }
        final P pos = extract(start.currentRail());
        final Node<?> search = search(start, following, world, pos, false);
        final Node<?> other = search(start, following, world, pos, true);
        if (search == null && other == null) {
            return SwapResult.BROKEN;
        }
        if (search == null) {
            return SwapResult.OK;
        }
        if (other == null) {
            return SwapResult.SWAP;
        }
        Node<?> first = (realDistance(search, following) <= realDistance(other, following)) ? search : other;
        while (first.prev != null) {
            first = first.prev;
        }
        return first.forwards ^ start.forwards() ? SwapResult.OK : SwapResult.SWAP;
    }

    public SwapResult swap(final AbstractCart<T, P> following, final AbstractCart<T, P> followed, final World world) {
        if (!following.onRail() || !followed.onRail()) {
            return SwapResult.BROKEN;
        }
        final P pos = extract(following.currentRail());
        final Node<?> search = search(following, followed, world, pos, false);
        final Node<?> other = search(following, followed, world, pos, true);
        if (search == null && other == null) {
            return SwapResult.BROKEN;
        }
        if (search == null) {
            return SwapResult.SWAP;
        }
        if (other == null) {
            return SwapResult.OK;
        }
        Node<?> first = (realDistance(search, followed) <= realDistance(other, followed)) ? search : other;
        while (first.prev != null) {
            first = first.prev;
        }
        return first.forwards ^ !following.forwards() ? SwapResult.SWAP : SwapResult.OK;
    }

    protected abstract P extract(T rail);

    protected abstract @Nullable Pair<P, RailProvider<T>> next(T rail, boolean forwards, World world);

    public Optional<Result> find(final AbstractCart<T, P> from, final AbstractCart<T, P> to, final double optimalDistance, final World world) {
        if (!from.onRail() || !to.onRail()) {
            return Optional.empty();
        }
        final P pos = extract(from.currentRail());
        final Node<?> search = search(from, to, world, pos, false);
        final Node<?> other = search(from, to, world, pos, true);
        if (search == null && other == null) {
            return Optional.empty();
        }
        final Node<?> end = search == null ? other : other == null ? search : (realDistance(search, to) < realDistance(other, to)) ? search : other;
        Node<?> first = end;
        while (first.prev != null) {
            first = first.prev;
        }
        final double distance = realDistance(end, to) * (first.forwards ? 1 : -1);
        final double optDist = distance - optimalDistance * (end.forwards ^ to.forwards() ? -1 : 1) * (first.forwards ? 1 : -1);
        return Optional.of(new Result(distance, optDist));
    }

    private static double realDistance(final Node<?> node, final Cart to) {
        final double d;
        if (node.prev != null) {
            d = node.prev.distance;
        } else {
            d = 0;
        }
        return d + Math.abs(to.progress() - node.progressOnEnter);
    }

    protected @Nullable Node<T> search(final AbstractCart<T, P> from, final AbstractCart<T, P> to, final World world, final P firstPos, final boolean reverse) {
        final T rail = from.currentRail();
        final Handle<P> handle = new Handle<>(firstPos, rail.id());
        final boolean forwards = reverse ^ from.forwards();
        final Node<T> current = new Node<>(rail, forwards, from.progress(), forwards ? rail.length() - from.progress() : from.progress(), 0, null);
        final Map<Handle<P>, Node<T>> nodes = new Object2ReferenceOpenHashMap<>();
        boolean first = true;
        nodes.put(handle, current);
        final Handle<P> target = new Handle<>(extract(to.currentRail()), to.currentRail().id());
        final Queue<Handle<P>> handles = new ArrayDeque<>();
        handles.add(handle);
        final int recursionLimit = this.recursionLimit.getAsInt();
        while (!handles.isEmpty()) {
            final Handle<P> poll = handles.poll();
            if (poll.equals(target)) {
                if (first) {
                    return new Node<>(rail, from.progress() < to.progress(), current.progressOnEnter, current.distance, 0, null);
                }
                return nodes.get(target);
            }
            final Node<T> node = nodes.get(poll);
            if (node.depth == recursionLimit) {
                continue;
            }
            Pair<Handle<P>, Node<T>> check = check(from, world, node, node.forwards);
            if (check != null) {
                if (nodes.putIfAbsent(check.getFirst(), check.getSecond()) == null) {
                    handles.add(check.getFirst());
                }
            }
            if (!first) {
                check = check(from, world, node, !node.forwards);
                if (check != null) {
                    if (nodes.putIfAbsent(check.getFirst(), check.getSecond()) == null) {
                        handles.add(check.getFirst());
                    }
                }
            }
            first = false;
        }
        return null;
    }

    protected @Nullable Pair<Handle<P>, Node<T>> check(final CartDataView view, final World world, final Node<T> prev, final boolean forwards) {
        final Pair<P, RailProvider<T>> pair = next(prev.rail(), forwards, world);
        if (pair == null) {
            return null;
        }
        final RailProvider<T> provider = pair.getSecond();
        final RailProvider.NextRailInfo<T> next = provider.next(view, prev.rail, forwards ? prev.rail.exitDirection() : prev.rail.entranceDirection());
        if (next == null) {
            return null;
        }
        final double d = next.forwards() ? next.rail().length() - next.progress() : next.progress();
        return Pair.of(new Handle<>(pair.getFirst(), next.rail().id()), new Node<>(next.rail(), next.forwards(), next.progress(), prev.distance + d, prev.depth + 1, prev));
    }


    public record Handle<P>(P pos, int id) {
    }

    public record Node<T extends Rail<T>>(T rail, boolean forwards, double progressOnEnter, double distance,
                                          int depth,
                                          @Nullable Node<T> prev) {
    }

    public record Result(double distance, double optimalDistance) {
    }

    public enum SwapResult {
        BROKEN,
        OK,
        SWAP
    }
}
