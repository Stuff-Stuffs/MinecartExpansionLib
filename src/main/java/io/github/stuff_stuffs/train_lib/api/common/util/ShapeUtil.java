package io.github.stuff_stuffs.train_lib.api.common.util;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class ShapeUtil {
    public static VoxelShape rotate(final VoxelShape shape, final DirectionTransformation transformation, final double featureScale) {
        return rotate(shape, transformation, featureScale, new Vec3d(0.5, 0.5, 0.5));
    }

    public static VoxelShape rotate(final VoxelShape shape, final DirectionTransformation transformation, final double featureScale, final Vec3d center) {
        final VoxelShape[] union = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> union[0] = VoxelShapes.combine(union[0], VoxelShapes.cuboid(rotate(minX, minY, minZ, maxX, maxY, maxZ, transformation, featureScale, center)), BooleanBiFunction.OR));
        return union[0].simplify();
    }

    private static Box rotate(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final DirectionTransformation transformation, final double featureScale, final Vec3d center) {
        final Vector3f lower = new Vector3f((float) minX, (float) minY, (float) minZ);
        final Vector3f upper = new Vector3f((float) maxX, (float) maxY, (float) maxZ);
        lower.sub((float) center.x, (float) center.y, (float) center.z);
        upper.sub((float) center.x, (float) center.y, (float) center.z);
        final Matrix3f matrix = transformation.getMatrix();
        matrix.transform(lower);
        matrix.transform(upper);
        final double transMinX = Math.min(round(lower.x, featureScale), round(upper.x, featureScale));
        final double transMinY = Math.min(round(lower.y, featureScale), round(upper.y, featureScale));
        final double transMinZ = Math.min(round(lower.z, featureScale), round(upper.z, featureScale));

        final double transMaxX = Math.max(round(lower.x, featureScale), round(upper.x, featureScale));
        final double transMaxY = Math.max(round(lower.y, featureScale), round(upper.y, featureScale));
        final double transMaxZ = Math.max(round(lower.z, featureScale), round(upper.z, featureScale));

        return new Box(transMinX + 0.5, transMinY + 0.5, transMinZ + 0.5, transMaxX + 0.5, transMaxY + 0.5, transMaxZ + 0.5);
    }

    private static double round(final double d, final double scale) {
        final double rounded = Math.round(d * scale);
        return rounded / scale;
    }

    private ShapeUtil() {
    }
}
