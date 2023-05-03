package io.github.stuff_stuffs.train_lib.api.common.util;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class ShapeUtil {
    public static VoxelShape rotate(final VoxelShape shape, final DirectionTransformation transformation, final double scale) {
        final VoxelShape[] union = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> union[0] = VoxelShapes.combine(union[0], VoxelShapes.cuboid(rotate(minX, minY, minZ, maxX, maxY, maxZ, transformation, scale)), BooleanBiFunction.OR));
        return union[0].simplify();
    }

    private static Box rotate(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final DirectionTransformation transformation, final double scale) {
        final Vector3f lower = new Vector3f((float) minX, (float) minY, (float) minZ);
        final Vector3f upper = new Vector3f((float) maxX, (float) maxY, (float) maxZ);
        lower.sub(0.5F, 0.5F, 0.5F);
        upper.sub(0.5F, 0.5F, 0.5F);
        final Matrix3f matrix = transformation.getMatrix();
        matrix.transform(lower);
        matrix.transform(upper);
        final double transMinX = Math.min(round(lower.x, scale), round(upper.x, scale));
        final double transMinY = Math.min(round(lower.y, scale), round(upper.y, scale));
        final double transMinZ = Math.min(round(lower.z, scale), round(upper.z, scale));

        final double transMaxX = Math.min(round(lower.x, scale), round(upper.x, scale));
        final double transMaxY = Math.min(round(lower.y, scale), round(upper.y, scale));
        final double transMaxZ = Math.min(round(lower.z, scale), round(upper.z, scale));

        return new Box(transMinX, transMinY, transMinZ, transMaxX, transMaxY, transMaxZ);
    }

    private static double round(final double d, final double scale) {
        final double rounded = Math.round(d * scale);
        return rounded / scale;
    }

    private ShapeUtil() {
    }
}
