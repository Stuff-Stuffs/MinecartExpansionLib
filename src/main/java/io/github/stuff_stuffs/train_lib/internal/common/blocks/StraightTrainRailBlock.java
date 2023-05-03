package io.github.stuff_stuffs.train_lib.internal.common.blocks;

import io.github.stuff_stuffs.train_lib.api.common.util.ShapeUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class StraightTrainRailBlock extends Block {
    public static final Property<Side> SIDE = EnumProperty.of("side", Side.class);
    public static final Property<Direction.Axis> FACING = Properties.HORIZONTAL_AXIS;
    protected static final Map<Direction.Axis, VoxelShape> LEFT_SHAPES;
    protected static final Map<Direction.Axis, VoxelShape> RIGHT_SHAPES;


    public StraightTrainRailBlock(final Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(SIDE, FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(final ItemPlacementContext ctx) {
        final Direction facing = ctx.getHorizontalPlayerFacing();
        final Direction.Axis axis = facing.getAxis();
        final Direction preferred = facing.getDirection() == Direction.AxisDirection.NEGATIVE ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
        if (ctx.getWorld().getBlockState(ctx.getBlockPos()).isReplaceable() && ctx.getWorld().getBlockState(ctx.getBlockPos().offset(preferred)).isReplaceable()) {
            return getDefaultState().with(FACING, axis).with(SIDE, Side.LEFT);
        } else if (ctx.getWorld().getBlockState(ctx.getBlockPos()).isReplaceable() && ctx.getWorld().getBlockState(ctx.getBlockPos().offset(preferred.getOpposite())).isReplaceable()) {
            return getDefaultState().with(FACING, axis).with(SIDE, Side.RIGHT);
        }
        return null;
    }

    @Override
    public VoxelShape getOutlineShape(final BlockState state, final BlockView world, final BlockPos pos, final ShapeContext context) {
        return (state.get(SIDE) == Side.LEFT ? LEFT_SHAPES : RIGHT_SHAPES).get(state.get(FACING));
    }

    @Override
    public PistonBehavior getPistonBehavior(final BlockState state) {
        return PistonBehavior.PUSH_ONLY;
    }

    public enum Side implements StringIdentifiable {
        RIGHT("right"),
        LEFT("left");
        private final String name;

        Side(final String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    static {
        final VoxelShape leftBase = createCuboidShape(4, 0, 0, 16, 4, 16);
        LEFT_SHAPES = Map.of(Direction.Axis.Z, leftBase, Direction.Axis.X, ShapeUtil.rotate(leftBase, DirectionTransformation.SWAP_XZ, 16));
        final VoxelShape rightBase = ShapeUtil.rotate(leftBase, DirectionTransformation.INVERT_X, 16);
        RIGHT_SHAPES = Map.of(Direction.Axis.Z, rightBase, Direction.Axis.X, ShapeUtil.rotate(rightBase, DirectionTransformation.SWAP_XZ, 16));
    }
}
