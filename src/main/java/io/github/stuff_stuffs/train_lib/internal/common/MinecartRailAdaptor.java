package io.github.stuff_stuffs.train_lib.internal.common;

import io.github.stuff_stuffs.train_lib.api.common.cart.CartDataView;
import io.github.stuff_stuffs.train_lib.api.common.cart.CartView;
import io.github.stuff_stuffs.train_lib.api.common.cart.RailProvider;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRailProvider;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic.DelegatingMinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic.DelegatingMinecartRailProvider;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic.PoweredMinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic.SimpleMinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.util.MathUtil;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class MinecartRailAdaptor {
    private MinecartRailAdaptor() {
    }

    public static MinecartRailProvider unpoweredShapeBased(final RailShape shape, final BlockPos pos) {
        return new DelegatingMinecartRailProvider(shapeBased(shape, pos)) {
            @Override
            protected MinecartRail wrap(final MinecartRail rail) {
                return new DelegatingMinecartRail(rail) {
                    @Override
                    public double friction(final CartView minecart, final double progress) {
                        return super.friction(minecart, progress) * 10;
                    }
                };
            }
        };
    }

    private static MinecartRail fromInfoPowered(final Info info, final BlockPos railPosition) {
        return new PoweredMinecartRail(info.start, info.end, 0, railPosition, info.entrance, info.exit, info.entranceDirection, info.exitDirection, 0.4);
    }

    public static MinecartRailProvider poweredShapeBased(final RailShape shape, final BlockPos pos) {
        final Info info = fromShape(shape, pos);
        final MinecartRail rail = fromInfoPowered(info, pos);
        return new MinecartRailProvider() {
            @Override
            public @Nullable NextRailInfo<MinecartRail> next(final CartDataView view, final MinecartRail current, @Nullable final Direction approachDirection) {
                if (approachDirection != null) {
                    final double progress = MathUtil.unAppliedProject(info.start, info.end, view.position(), true);
                    return new NextRailInfo<>(rail, progress, approachDirection == info.entranceDirection.getOpposite());
                }
                return null;
            }

            @Override
            public MinecartRail currentRail(final CartDataView view) {
                return rail;
            }

            @Override
            public NextRailInfo<MinecartRail> snap(final CartView view) {
                final double progress = MathUtil.unAppliedProject(info.start, info.end, view.position(), true);
                if (view.velocity().dotProduct(info.end.subtract(info.start)) < 0) {
                    return new NextRailInfo<>(rail, progress, false);
                } else {
                    return new NextRailInfo<>(rail, progress, true);
                }
            }

            @Override
            public IntSet ids() {
                return IntSet.of(0);
            }

            @Override
            public MinecartRail fromId(final int id) {
                return rail;
            }

            @Override
            public IntSet intersecting(final int id) {
                return IntSet.of();
            }

            @Override
            public RailReflectionInfo<MinecartRail> nextReflect(final MinecartRail current, @Nullable final Direction approachDirection) {
                return snapReflect(approachDirection);
            }

            @Override
            public RailReflectionInfo<MinecartRail> snapReflect(@Nullable final Direction approachDirection) {
                return new RailReflectionInfo<>(rail, approachDirection == info.entranceDirection.getOpposite());
            }
        };
    }

    private static MinecartRail fromInfoUnpowered(final Info info, final BlockPos railPosition) {
        return new SimpleMinecartRail(info.start, info.end, 0, railPosition, info.entrance, info.exit, info.entranceDirection, info.exitDirection);
    }

    public static MinecartRailProvider shapeBased(final RailShape shape, final BlockPos pos) {
        final Info info = fromShape(shape, pos);
        final MinecartRail rail = fromInfoUnpowered(info, pos);
        return new MinecartRailProvider() {
            @Override
            public @Nullable NextRailInfo<MinecartRail> next(final CartDataView view, final MinecartRail current, @Nullable final Direction approachDirection) {
                if (approachDirection != null) {
                    final double progress = MathUtil.unAppliedProject(info.start, info.end, view.position(), true);
                    return new NextRailInfo<>(rail, progress, approachDirection == info.entranceDirection.getOpposite());
                }
                return null;
            }

            @Override
            public MinecartRail currentRail(final CartDataView view) {
                return rail;
            }

            @Override
            public NextRailInfo<MinecartRail> snap(final CartView view) {
                final double progress = MathUtil.unAppliedProject(info.start, info.end, view.position(), true);
                if (view.velocity().dotProduct(info.end.subtract(info.start)) < 0) {
                    return new NextRailInfo<>(rail, progress, false);
                } else {
                    return new NextRailInfo<>(rail, progress, true);
                }
            }

            @Override
            public IntSet ids() {
                return IntSet.of(0);
            }

            @Override
            public MinecartRail fromId(final int id) {
                if (id == 0) {
                    return rail;
                }
                throw new IllegalArgumentException();
            }

            @Override
            public IntSet intersecting(final int id) {
                return IntSet.of();
            }

            @Override
            public RailProvider.RailReflectionInfo<MinecartRail> nextReflect(final MinecartRail current, @Nullable final Direction approachDirection) {
                return snapReflect(approachDirection);
            }

            @Override
            public RailReflectionInfo<MinecartRail> snapReflect(@Nullable final Direction approachDirection) {
                return new RailReflectionInfo<>(rail, approachDirection == info.entranceDirection.getOpposite());
            }
        };
    }

    public static Info fromShape(final RailShape shape, final BlockPos pos) {
        final Vec3d start;
        final Vec3d end;
        final Offset offset;
        final Vec3d center = Vec3d.ofBottomCenter(pos);
        switch (shape) {
            case NORTH_SOUTH -> {
                start = center.add(0, 0, -0.5);
                end = center.add(0, 0, 0.5);
                offset = new Offset(pos.offset(Direction.SOUTH), pos.offset(Direction.NORTH), Direction.SOUTH, Direction.NORTH);
            }
            case EAST_WEST -> {
                start = center.add(0.5, 0, 0);
                end = center.add(-0.5, 0, 0);
                offset = new Offset(pos.offset(Direction.WEST), pos.offset(Direction.EAST), Direction.WEST, Direction.EAST);
            }
            case ASCENDING_EAST -> {
                start = center.add(-0.5, 0, 0);
                end = center.add(0.5, 1, 0);
                offset = new Offset(pos.offset(Direction.EAST).offset(Direction.UP), pos.offset(Direction.WEST), Direction.EAST, Direction.WEST);
            }
            case ASCENDING_WEST -> {
                start = center.add(0.5, 0, 0);
                end = center.add(-0.5, 1, 0);
                offset = new Offset(pos.offset(Direction.WEST).offset(Direction.UP), pos.offset(Direction.EAST), Direction.WEST, Direction.EAST);
            }
            case ASCENDING_NORTH -> {
                start = center.add(0, 0, 0.5);
                end = center.add(0, 1, -0.5);
                offset = new Offset(pos.offset(Direction.NORTH).offset(Direction.UP), pos.offset(Direction.SOUTH), Direction.NORTH, Direction.SOUTH);
            }
            case ASCENDING_SOUTH -> {
                start = center.add(0, 0, -0.5);
                end = center.add(0, 1, 0.5);
                offset = new Offset(pos.offset(Direction.SOUTH).offset(Direction.UP), pos.offset(Direction.NORTH), Direction.SOUTH, Direction.NORTH);
            }
            case NORTH_EAST -> {
                start = center.add(0, 0, -0.5);
                end = center.add(0.5, 0, 0);
                offset = new Offset(pos.offset(Direction.EAST), pos.offset(Direction.NORTH), Direction.EAST, Direction.NORTH);
            }
            case NORTH_WEST -> {
                start = center.add(0, 0, -0.5);
                end = center.add(-0.5, 0, 0);
                offset = new Offset(pos.offset(Direction.WEST), pos.offset(Direction.NORTH), Direction.WEST, Direction.NORTH);
            }
            case SOUTH_WEST -> {
                start = center.add(0, 0, 0.5);
                end = center.add(-0.5, 0, 0);
                offset = new Offset(pos.offset(Direction.WEST), pos.offset(Direction.SOUTH), Direction.WEST, Direction.SOUTH);
            }
            case SOUTH_EAST -> {
                start = center.add(0, 0, 0.5);
                end = center.add(0.5, 0, 0);
                offset = new Offset(pos.offset(Direction.EAST), pos.offset(Direction.SOUTH), Direction.EAST, Direction.SOUTH);
            }
            default -> throw new NullPointerException();
        }
        return new Info(start, end, offset.entrance, offset.exit, offset.entranceDirection, offset.exitDirection);
    }

    public record Info(Vec3d start, Vec3d end, BlockPos entrance, BlockPos exit, Direction entranceDirection,
                       Direction exitDirection) {
    }

    private record Offset(BlockPos exit, BlockPos entrance, Direction exitDirection, Direction entranceDirection) {
    }
}
