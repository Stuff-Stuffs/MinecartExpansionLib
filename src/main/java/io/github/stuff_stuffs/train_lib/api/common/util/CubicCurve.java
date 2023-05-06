package io.github.stuff_stuffs.train_lib.api.common.util;

import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

public class CubicCurve {
    private final Vec3d p0, p1, p2, p3;

    public CubicCurve(final Vec3d p0, final Vec3d p1, final Vec3d p2, final Vec3d p3) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public Vec3d eval(final double t) {
        final double t2_1 = (1 - t) * (1 - t);
        final double t3_1 = t2_1 * (1 - t);
        return p0.multiply(t3_1).add(p1.multiply(3 * t2_1 * t)).add(p2.multiply(3 * (1 - t) * t * t)).add(p3.multiply(t * t * t));
    }

    public Vec3d tangent(final double t) {
        final Vec3d p10 = p1.subtract(p0);
        final Vec3d p21 = p2.subtract(p1);
        final Vec3d p32 = p3.subtract(p2);
        final double t2_1 = (1 - t) * (1 - t);
        return p10.multiply(3 * t2_1).add(p21.multiply(6 * (1 - t) * t)).add(p32.multiply(3 * t * t));
    }

    public static final class Cached {
        private final double[] lengthPrefixSum;
        private final Vec3d[] points;
        private final Vec3d[] tangents;
        private final double[] slope;
        private final double length;
        private final double avgSlope;

        public Cached(final CubicCurve curve, final int segments) {
            lengthPrefixSum = new double[segments];
            points = new Vec3d[segments];
            tangents = new Vec3d[segments];
            slope = new double[segments];
            double lengthSum = 0;
            final double step = 1 / (double) segments;
            Vec3d prevPoint = curve.eval(0);
            Vec3d prevTangent = curve.tangent(0);
            points[0] = prevPoint;
            tangents[0] = prevTangent;
            double avgS = 0;
            for (int i = 1; i < segments; i++) {
                final double t = i * step;
                final Vec3d point = curve.eval(t);
                final Vec3d tangent = curve.tangent(t);
                points[i] = point;
                tangents[i] = tangent;
                slope[i] = Math.asin(tangent.y);
                final double distance = prevPoint.distanceTo(point);
                avgS = slope[i] * distance;
                lengthSum = lengthSum + distance;
                lengthPrefixSum[i] = lengthSum;
                prevPoint = point;
                prevTangent = tangent;
            }
            length = lengthSum;
            avgSlope = avgS / length;
        }

        public Vec3d eval(final double t) {
            if (t < 0 || t > length) {
                return Vec3d.ZERO;
            }
            final int search = Arrays.binarySearch(lengthPrefixSum, t);
            if (search >= 0) {
                return points[search];
            }
            final int real = -search - 1;
            final int other = real - 1;
            final double start = lengthPrefixSum[other];
            final double end = lengthPrefixSum[real];
            final double alpha = (t - start) / (end - start);
            return points[other].multiply(1 - alpha).add(points[real].multiply(alpha));
        }

        public Vec3d tangent(final double t) {
            if (t < 0 || t > length) {
                return Vec3d.ZERO;
            }
            final int search = Arrays.binarySearch(lengthPrefixSum, t);
            if (search >= 0) {
                return points[search];
            }
            final int real = -search - 1;
            final int other = real - 1;
            final double start = lengthPrefixSum[other];
            final double end = lengthPrefixSum[real];
            final double alpha = (t - start) / (end - start);
            return tangents[other].multiply(1 - alpha).add(tangents[real].multiply(alpha));
        }

        public double slope(final double t) {
            if (t < 0 || t > length) {
                return 0;
            }
            final int search = Arrays.binarySearch(lengthPrefixSum, t);
            if (search >= 0) {
                return slope[search];
            }
            final int real = -search - 1;
            final int other = real - 1;
            final double start = lengthPrefixSum[other];
            final double end = lengthPrefixSum[real];
            final double alpha = (t - start) / (end - start);
            return (slope[other] * (1 - alpha)) + (slope[real] * alpha);
        }

        public double averageSlope() {
            return avgSlope;
        }

        public double length() {
            return length;
        }

        public double snap(final Vec3d p) {
            double best = Double.POSITIVE_INFINITY;
            int bestSegment = -1;
            for (int i = 0; i < points.length - 1; i++) {
                final double project = MathUtil.unAppliedProject(points[i], points[i + 1], p, false);
                if (Math.abs(project) < Math.abs(best)) {
                    if (bestSegment == -1 || !(project < 0) || !(best >= 0)) {
                        best = project;
                        bestSegment = i;
                    }
                } else {
                    final double start = lengthPrefixSum[i];
                    final double end = lengthPrefixSum[i + 1];
                    final double length = end - start;
                    if (project >= 0 && project <= length) {
                        best = project;
                        bestSegment = i;
                    }
                }
            }
            final double start = lengthPrefixSum[bestSegment];
            final double end = lengthPrefixSum[bestSegment + 1];
            return best * (end - start) + start;
        }
    }
}
