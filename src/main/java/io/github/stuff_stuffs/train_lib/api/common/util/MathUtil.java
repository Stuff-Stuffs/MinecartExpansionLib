package io.github.stuff_stuffs.train_lib.api.common.util;

import net.minecraft.util.math.Vec3d;

public final class MathUtil {
    public static final double EPS = 0.000001;
    public static final double SQRT_2 = Math.sqrt(2);
    public static final double INV_SQRT2 = 1 / SQRT_2;

    public static boolean approxEquals(final double a, final double b) {
        return Math.abs(a - b) < EPS;
    }

    public static boolean approxEquals(final Vec3d first, final Vec3d second) {
        return first.squaredDistanceTo(second) < EPS * EPS;
    }

    public static double unAppliedProject(final Vec3d start, final Vec3d end, final Vec3d source, final boolean clamp) {
        final Vec3d delta = end.subtract(start);
        final Vec3d deltaNorm = delta.normalize();
        final Vec3d translated = source.subtract(start);
        double t = deltaNorm.dotProduct(translated);
        if (clamp) {
            final double len;
            if (t < 0) {
                t = 0;
            } else if (t > (len = delta.length())) {
                t = len;
            }
        }
        return t;
    }

    public static Vec3d project(final Vec3d start, final Vec3d end, final Vec3d source, final boolean clamp) {
        final Vec3d delta = end.subtract(start);
        final Vec3d deltaNorm = delta.normalize();
        final Vec3d translated = source.subtract(start);
        double t = deltaNorm.dotProduct(translated);
        if (clamp) {
            final double len;
            if (t < 0) {
                t = 0;
            } else if (t > (len = delta.length())) {
                t = len;
            }
        }
        return deltaNorm.multiply(t).add(start);
    }

    public static Vec3d slerp(final Vec3d start, final Vec3d end, final double alpha) {
        final Vec3d res;
        final double dot = start.dotProduct(end);
        if (dot > 0.99999) {
            res = start.multiply(1 - alpha).add(end.multiply(alpha)).normalize();
        } else {
            final double acos = Math.acos(dot);
            final double div = 1 / Math.sin(acos);
            res = start.multiply(Math.sin((1 - alpha) * acos * div)).add(end.multiply(Math.sin(alpha * acos * div)));
        }
        return res;
    }

    private MathUtil() {
    }
}
