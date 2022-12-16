package dji.v5.ux.core.util;


/**
 * <p>Description:</p>
 *
 * @author create at 2019/7/29 9:05 PM by ron.liu for dji-pilot
 * @version v1.0
 */
public class MatrixUtils {

    public static float[] createRotationMatrix(float yaw, float pitch, float roll) {
        float[] result = new float[9];
        float radianYaw = (float) Math.toRadians(yaw);
        float radianPitch = (float) Math.toRadians(pitch);
        float radianRoll = (float) Math.toRadians(roll);
        float cy = (float) Math.cos(radianYaw);
        float sy = (float) Math.sin(radianYaw);

        float cp = (float) Math.cos(radianPitch);
        float sp = (float) Math.sin(radianPitch);

        float cr = (float) Math.cos(radianRoll);
        float sr = (float) Math.sin(radianRoll);

        result[0] = cy * cp;
        result[1] = -sy * cr + cy * sp * sr;
        result[2] = sy * sr + cy * sp * cr;
        result[3] = sy * cp;
        result[4] = cy * cr +sy * sp *sr;
        result[5] = -cy * sr + sy * sp * cr;
        result[6] = -sp;
        result[7] = cp * sr;
        result[8] = cp * cr;
        return result;
    }

    public static float[] createIntrinsicMatrix(float fx, float fy, float u, float v) {
        float[] result = new float[9];
        result[0] = fx;
        result[1] = 0.0f;
        result[2] = u;
        result[3] = 0.0f;
        result[4] = fy;
        result[5] = v;
        result[6] = 0.0f;
        result[7] = 0.0f;
        result[8] = 1.0f;
        return result;
    }

    public static float[] transposeMatrix(float[] matrix) {
        float[] result = new float[9];

        result[0] = matrix[0];
        result[1] = matrix[3];
        result[2] = matrix[6];
        result[3] = matrix[1];
        result[4] = matrix[4];
        result[5] = matrix[7];
        result[6] = matrix[2];
        result[7] = matrix[5];
        result[8] = matrix[8];
        return result;
    }

    public static float[] productMatrix(float[] matrix1, float[] matrix2) {
        float[] result = new float[9];
        result[0] = matrix1[0] * matrix2[0] + matrix1[1] * matrix2[3] + matrix1[2] * matrix2[6];
        result[1] = matrix1[0] * matrix2[1] + matrix1[1] * matrix2[4] + matrix1[2] * matrix2[7];
        result[2] = matrix1[0] * matrix2[2] + matrix1[1] * matrix2[5] + matrix1[2] * matrix2[8];
        result[3] = matrix1[3] * matrix2[0] + matrix1[4] * matrix2[3] + matrix1[5] * matrix2[6];
        result[4] = matrix1[3] * matrix2[1] + matrix1[4] * matrix2[4] + matrix1[5] * matrix2[7];
        result[5] = matrix1[3] * matrix2[2] + matrix1[4] * matrix2[5] + matrix1[5] * matrix2[8];
        result[6] = matrix1[6] * matrix2[0] + matrix1[7] * matrix2[3] + matrix1[8] * matrix2[6];
        result[7] = matrix1[6] * matrix2[1] + matrix1[7] * matrix2[4] + matrix1[8] * matrix2[7];
        result[8] = matrix1[6] * matrix2[2] + matrix1[7] * matrix2[5] + matrix1[8] * matrix2[8];

        return result;
    }

    public static float[] rotateVector(float[] vector, float[] matrix) {
        float[] result = new float[3];
        result[0] = matrix[0] * vector[0] + matrix[1] * vector[1] + matrix[2] * vector[2];
        result[1] = matrix[3] * vector[0] + matrix[4] * vector[1] + matrix[5] * vector[2];
        result[2] = matrix[6] * vector[0] + matrix[7] * vector[1] + matrix[8] * vector[2];
        return result;
    }

    public static float[] multiple(float[] matrix, float vector) {
        for (int i = 0; i < matrix.length; i ++) {
            matrix[i] = matrix[i] * vector;
        }
        return matrix;
    }

    private MatrixUtils() {

    }
}
