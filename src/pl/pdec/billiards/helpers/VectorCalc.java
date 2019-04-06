package pl.pdec.billiards.helpers;

public class VectorCalc {
    public static double[] subtractVector(double[] v1, double[] v2) {
        return new double[]{v1[0] - v2[0], v1[1] - v2[1]};
    }

    public static double[] multiplyVector(double scalar, double[] v) {
        return new double[]{v[0] * scalar, v[1] * scalar};
    }

    public static double[] normalizeVector(double[] vector) {
        double magnitude = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
        return new double[]{vector[0] / magnitude, vector[1] / magnitude};
    }

    public static double[] reflectVector(double[] sourceVector, double[] surfaceVector) {
        return subtractVector(sourceVector, multiplyVector(2
                * dot(sourceVector, surfaceVector), surfaceVector));
    }

    public static double dot(double[] v1, double[] v2) {
        return v1[0] * v2[0] + v1[1] * v2[1];
    }

    public static double cross(double[] v1, double[] v2) {
        return v1[0] * v2[1] - v1[1] * v2[0];
    }

    public static double distanceBetweenPoints(double[] point1, double[] point2) {
        return Math.sqrt((point2[0] - point1[0]) * (point2[0] - point1[0])
                + (point2[1] - point1[1]) * (point2[1] - point1[1]));
    }
}
