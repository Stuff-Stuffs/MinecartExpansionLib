package io.github.stuff_stuffs.train_lib.impl.common;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.train_lib.api.common.cart.*;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

public final class MinecartPathfinder {
    private static final int MAX_RAIL_SEARCH = 8;

    private MinecartPathfinder() {
    }

    public record Result(double distance, double optimalDistance) {
    }

    public enum SwapResult {
        BROKEN,
        OK,
        SWAP
    }

    public static SwapResult swap(final MinecartImpl front, final MinecartImpl back, final World world) {
        if (!front.onRail() || !back.onRail()) {
            return SwapResult.BROKEN;
        }
        final Node search = search(back, front, world, back.currentRail().railPosition(), false);
        final Node other = search(back, front, world, back.currentRail().railPosition(), true);
        if (search == null && other == null) {
            return SwapResult.BROKEN;
        }
        final Node end = search == null ? other : other == null ? search : (realDistance(search, back) < realDistance(other, back)) ? search : other;
        return back.speed() >= 0 ^ !end.forwards() ? SwapResult.OK : SwapResult.SWAP;
    }


    public static Optional<Result> find(final MinecartImpl from, final MinecartImpl to, final double optimalDistance, final World world) {
        if (!from.onRail() || !to.onRail()) {
            return Optional.empty();
        }
        final Node search = search(from, to, world, from.currentRail().railPosition(), false);
        final Node other = search(from, to, world, from.currentRail().railPosition(), true);
        if (search == null && other == null) {
            return Optional.empty();
        }
        final Node end = search == null ? other : other == null ? search : (realDistance(search, to) < realDistance(other, to)) ? search : other;
        Node first = end;
        while (first.prev != null) {
            first = first.prev;
        }
        final double distance = realDistance(end, to) * (first.forwards ? 1 : -1);
        final double optDist = distance - optimalDistance * (end.forwards ^ to.speed() >= 0 ? -1 : 1) * (first.forwards ? 1 : -1);
        return Optional.of(new Result(distance, optDist));
    }

    private static double realDistance(final Node node, final Minecart to) {
        final double d;
        if (node.prev != null) {
            d = node.prev.distance;
        } else {
            d = 0;
        }
        return d + Math.abs(to.progress() - node.progressOnEnter);
    }

    private static @Nullable Node search(final MinecartImpl from, final MinecartImpl to, final World world, final BlockPos firstBlockPos, final boolean reverse) {
        final MinecartRail rail = from.currentRail();
        final Handle handle = new Handle(firstBlockPos.toImmutable(), rail.id());
        final boolean forwards = reverse ^ from.speed() >= 0;
        final Node current = new Node(rail, forwards, from.progress(), forwards ? rail.length() - from.progress() : from.progress(), 0, null);
        final Map<Handle, Node> nodes = new Object2ReferenceOpenHashMap<>();
        boolean first = true;
        nodes.put(handle, current);
        final Handle target = new Handle(to.currentRail().railPosition(), to.currentRail().id());
        final Queue<Handle> handles = new ArrayDeque<>();
        handles.add(handle);
        while (!handles.isEmpty()) {
            final Handle poll = handles.poll();
            if (poll.equals(target)) {
                return nodes.get(target);
            }
            final Node node = nodes.get(poll);
            if (node.depth == MAX_RAIL_SEARCH) {
                continue;
            }
            Pair<Handle, Node> check = check(from, world, node, node.forwards);
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

    private static @Nullable Pair<Handle, Node> check(final MinecartDataView view, final World world, final Node prev, final boolean forwards) {
        BlockPos search = forwards ? prev.rail.exitPosition() : prev.rail.entrancePosition();
        @Nullable final Direction direction = forwards ? prev.rail.exitDirection() : prev.rail.entranceDirection();
        MinecartRailProvider provider = MinecartAPI.MINECART_RAIL_BLOCK_API.find(world, search, null);
        if (provider == null) {
            search = search.offset(Direction.DOWN);
            provider = MinecartAPI.MINECART_RAIL_BLOCK_API.find(world, search, null);
            if (provider == null) {
                return null;
            }
        }
        final MinecartRailProvider.NextRailInfo next = provider.next(view, prev.rail, direction);
        if (next == null) {
            return null;
        }
        final double d = next.forwards() ? next.rail().length() - next.progress() : next.progress();
        return Pair.of(new Handle(search.toImmutable(), next.rail().id()), new Node(next.rail(), next.forwards(), next.progress(), prev.distance + d, prev.depth + 1, prev));
    }


    private record Handle(BlockPos pos, int id) {
    }

    private record Node(MinecartRail rail, boolean forwards, double progressOnEnter, double distance, int depth,
                        @Nullable Node prev) {
    }
}
