package dji.sampleV5.aircraft.util.geometry

/**
 * 4×4 列优先（column-first）双精度矩阵
 * 完全参照 Pilot dji.lidar_map.utils.geometry.DoubleMatrix4
 *
 * 内存布局（列优先，与 OpenGL 一致）：
 *   m[0]  m[4]  m[8]  m[12]
 *   m[1]  m[5]  m[9]  m[13]
 *   m[2]  m[6]  m[10] m[14]
 *   m[3]  m[7]  m[11] m[15]
 */
class DoubleMatrix4(
    val m: DoubleArray
) {
    // 单位矩阵
    constructor() : this(
        doubleArrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
    )

    // 按行序传入，内部转为列优先存储
    constructor(
        m11: Double, m21: Double, m31: Double, m41: Double,
        m12: Double, m22: Double, m32: Double, m42: Double,
        m13: Double, m23: Double, m33: Double, m43: Double,
        m14: Double, m24: Double, m34: Double, m44: Double
    ) : this(
        doubleArrayOf(
            m11, m21, m31, m41,
            m12, m22, m32, m42,
            m13, m23, m33, m43,
            m14, m24, m34, m44
        )
    )

    fun identity(): DoubleMatrix4 = DoubleMatrix4(
        1.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0
    )

    /** 转置 */
    fun transpose(): DoubleMatrix4 = DoubleMatrix4(
        m[0], m[4], m[8],  m[12],
        m[1], m[5], m[9],  m[13],
        m[2], m[6], m[10], m[14],
        m[3], m[7], m[11], m[15]
    )

    /** 标量乘 */
    fun dot(value: Double): DoubleMatrix4 {
        val n = m.clone()
        for (i in 0..15) n[i] *= value
        return DoubleMatrix4(n)
    }

    /** 矩阵叉乘：result = this × matrix4 */
    fun multiply(matrix4: DoubleMatrix4): DoubleMatrix4 {
        val n = matrix4.m
        val result = DoubleMatrix4()
        for (i in 0..3) {
            for (j in 0..3) {
                var product = 0.0
                for (k in 0..3) product += m[i + k * 4] * n[k + j * 4]
                result.m[i + j * 4] = product
            }
        }
        return result
    }

    /** 矩阵乘向量（w = 1.0） */
    fun multiply(vector3: DoubleVector3): DoubleVector3 = multiply(vector3, 1.0)

    /** 矩阵乘向量（指定 w） */
    fun multiply(vector3: DoubleVector3, w: Double): DoubleVector3 {
        val old = doubleArrayOf(vector3.x, vector3.y, vector3.z, w)
        val new = DoubleArray(4)
        for (i in 0..3) {
            var product = 0.0
            for (j in 0..3) product += m[i + j * 4] * old[j]
            new[i] = product
        }
        return DoubleVector3(new[0], new[1], new[2])
    }

    /**
     * 求逆（Cramer's Rule）
     * 若行列式为 0（不可逆），返回单位矩阵
     */
    fun invert(): DoubleMatrix4 {
        val src = this.transpose().m

        val atmp = doubleArrayOf(
            src[10] * src[15], src[11] * src[14], src[9] * src[15], src[11] * src[13],
            src[9]  * src[14], src[10] * src[13], src[8] * src[15], src[11] * src[12],
            src[8]  * src[14], src[10] * src[12], src[8] * src[13], src[9]  * src[12],
        )
        val btmp = doubleArrayOf(
            src[2] * src[7], src[3] * src[6], src[1] * src[7], src[3] * src[5],
            src[1] * src[6], src[2] * src[5], src[0] * src[7], src[3] * src[4],
            src[0] * src[6], src[2] * src[4], src[0] * src[5], src[1] * src[4],
        )
        val dst = doubleArrayOf(
            (atmp[0]*src[5] + atmp[3]*src[6] + atmp[4]*src[7])  - (atmp[1]*src[5] + atmp[2]*src[6]  + atmp[5]*src[7]),
            (atmp[1]*src[4] + atmp[6]*src[6] + atmp[9]*src[7])  - (atmp[0]*src[4] + atmp[7]*src[6]  + atmp[8]*src[7]),
            (atmp[2]*src[4] + atmp[7]*src[5] + atmp[10]*src[7]) - (atmp[3]*src[4] + atmp[6]*src[5]  + atmp[11]*src[7]),
            (atmp[5]*src[4] + atmp[8]*src[5] + atmp[11]*src[6]) - (atmp[4]*src[4] + atmp[9]*src[5]  + atmp[10]*src[6]),
            (atmp[1]*src[1] + atmp[2]*src[2] + atmp[5]*src[3])  - (atmp[0]*src[1] + atmp[3]*src[2]  + atmp[4]*src[3]),
            (atmp[0]*src[0] + atmp[7]*src[2] + atmp[8]*src[3])  - (atmp[1]*src[0] + atmp[6]*src[2]  + atmp[9]*src[3]),
            (atmp[3]*src[0] + atmp[6]*src[1] + atmp[11]*src[3]) - (atmp[2]*src[0] + atmp[7]*src[1]  + atmp[10]*src[3]),
            (atmp[4]*src[0] + atmp[9]*src[1] + atmp[10]*src[2]) - (atmp[5]*src[0] + atmp[8]*src[1]  + atmp[11]*src[2]),
            (btmp[0]*src[13] + btmp[3]*src[14] + btmp[4]*src[15])  - (btmp[1]*src[13] + btmp[2]*src[14]  + btmp[5]*src[15]),
            (btmp[1]*src[12] + btmp[6]*src[14] + btmp[9]*src[15])  - (btmp[0]*src[12] + btmp[7]*src[14]  + btmp[8]*src[15]),
            (btmp[2]*src[12] + btmp[7]*src[13] + btmp[10]*src[15]) - (btmp[3]*src[12] + btmp[6]*src[13]  + btmp[11]*src[15]),
            (btmp[5]*src[12] + btmp[8]*src[13] + btmp[11]*src[14]) - (btmp[4]*src[12] + btmp[9]*src[13]  + btmp[10]*src[14]),
            (btmp[2]*src[10] + btmp[5]*src[11] + btmp[1]*src[9])   - (btmp[4]*src[11] + btmp[0]*src[9]   + btmp[3]*src[10]),
            (btmp[8]*src[11] + btmp[0]*src[8]  + btmp[7]*src[10])  - (btmp[6]*src[10] + btmp[9]*src[11]  + btmp[1]*src[8]),
            (btmp[6]*src[9]  + btmp[11]*src[11] + btmp[3]*src[8])  - (btmp[10]*src[11] + btmp[2]*src[8]  + btmp[7]*src[9]),
            (btmp[10]*src[10] + btmp[4]*src[8] + btmp[9]*src[9])   - (btmp[8]*src[9]  + btmp[11]*src[10] + btmp[5]*src[8])
        )

        val det = src[0] * dst[0] + src[1] * dst[1] + src[2] * dst[2] + src[3] * dst[3]
        if (det == 0.0) return identity()
        return DoubleMatrix4(dst).dot(1.0 / det)
    }
}
