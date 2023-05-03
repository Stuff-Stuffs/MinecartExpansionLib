package io.github.stuff_stuffs.train_lib.api.client.render.model;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class TrainRailModelGenerator {
    private static final Direction[] DIRECTIONS = Direction.values();

    public static void generate(final RailInfo info, final QuadEmitter emitter, final int railColor, final RenderMaterial railMaterial, final int tieColor, final RenderMaterial tieMaterial) {
        final double length = info.length();
        final int steps = Math.max(Math.round((float) (length * 2)), 1);
        final double stepLength = length / (double) steps;
        for (int i = 0; i < steps; i++) {
            final double t = stepLength * i;
            final Vec3d normal = info.normal(t + stepLength * 0.5);
            final Vec3d up = info.up(t + stepLength * 0.5);

            final Vec3d startLeft = info.posLeft(t);
            final Vec3d endLeft = info.posLeft(t + stepLength);
            emitCuboid(box(startLeft, endLeft, up, normal), emitter, railColor, railMaterial);

            final Vec3d startRight = info.posRight(t);
            final Vec3d endRight = info.posRight(t + stepLength);
            emitCuboid(box(startRight, endRight, up, normal), emitter, railColor, railMaterial);
        }
    }

    private static Box box(final Vec3d start, final Vec3d end, final Vec3d up, final Vec3d normal) {
        final Vec3d[] points = new Vec3d[]{
                start,
                start.add(up),
                start.add(normal),
                start.add(normal).add(up),
                end,
                end.add(up),
                end.add(normal),
                end.add(normal).add(up)
        };
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final Vec3d point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);

            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void emitCuboid(final Box box, final QuadEmitter emitter, final int color, final RenderMaterial material) {
        for (final Direction direction : DIRECTIONS) {
            for (int i = 0; i < 4; i++) {
                emitter.pos(i, vertex(box, direction, i).toVector3f());
            }
            emitter.spriteColor(0, color, color, color, color);
            emitter.material(material);
            emitter.emit();
        }
    }

    private static Vec3d vertex(final Box box, final Direction face, final int index) {
        return switch (face) {
            case UP -> switch (index) {
                case 0 -> new Vec3d(box.minX, box.maxY, box.minZ);
                case 1 -> new Vec3d(box.maxX, box.maxY, box.minZ);
                case 2 -> new Vec3d(box.maxX, box.maxY, box.maxZ);
                case 3 -> new Vec3d(box.minX, box.maxY, box.maxZ);
                default -> throw new IndexOutOfBoundsException();
            };
            case DOWN -> switch (index) {
                case 0 -> new Vec3d(box.minX, box.minY, box.minZ);
                case 1 -> new Vec3d(box.maxX, box.minY, box.minZ);
                case 2 -> new Vec3d(box.maxX, box.minY, box.maxZ);
                case 3 -> new Vec3d(box.minX, box.minY, box.maxZ);
                default -> throw new IndexOutOfBoundsException();
            };
            case EAST -> switch (index) {
                case 0 -> new Vec3d(box.maxX, box.minY, box.minZ);
                case 1 -> new Vec3d(box.maxX, box.minY, box.maxZ);
                case 2 -> new Vec3d(box.maxX, box.maxY, box.maxZ);
                case 3 -> new Vec3d(box.maxX, box.maxY, box.minZ);
                default -> throw new IndexOutOfBoundsException();
            };
            case WEST -> switch (index) {
                case 0 -> new Vec3d(box.minX, box.minY, box.maxZ);
                case 1 -> new Vec3d(box.minX, box.maxY, box.maxZ);
                case 2 -> new Vec3d(box.minX, box.maxY, box.minZ);
                case 3 -> new Vec3d(box.minX, box.minY, box.minZ);
                default -> throw new IndexOutOfBoundsException();
            };
            case NORTH -> switch (index) {
                case 0 -> new Vec3d(box.minX, box.minY, box.minZ);
                case 1 -> new Vec3d(box.minX, box.maxY, box.minZ);
                case 2 -> new Vec3d(box.maxX, box.maxY, box.minZ);
                case 3 -> new Vec3d(box.maxX, box.minY, box.minZ);
                default -> throw new IndexOutOfBoundsException();
            };
            case SOUTH -> switch (index) {
                case 0 -> new Vec3d(box.minX, box.minY, box.minZ);
                case 1 -> new Vec3d(box.maxX, box.minY, box.minZ);
                case 2 -> new Vec3d(box.maxX, box.maxY, box.minZ);
                case 3 -> new Vec3d(box.minX, box.maxY, box.minZ);
                default -> throw new IndexOutOfBoundsException();
            };
        };
    }

    public interface RailInfo {
        double length();

        Vec3d posLeft(double t);

        Vec3d posRight(double t);

        Vec3d normal(double t);

        Vec3d up(double t);
    }

    private TrainRailModelGenerator() {
    }
}
