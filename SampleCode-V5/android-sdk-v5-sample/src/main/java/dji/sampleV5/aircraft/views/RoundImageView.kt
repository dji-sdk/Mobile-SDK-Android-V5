package dji.sampleV5.aircraft.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import dji.sampleV5.aircraft.R

class RoundImageView(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context!!, attrs) {
    private var mPaint: Paint? = null
    private var mPath: Path? = null
    private var mRound = 0
    private var mbCircle = false

    fun setBeCircle(circle: Boolean) {
        mbCircle = circle
    }

    fun setRound(round: Int) {
        mRound = round
    }

    private fun init() {
        mRound = this.resources.getDimensionPixelSize(R.dimen.gen_corner_radius)
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isFilterBitmap = true
        mPaint!!.color = 0xfeffffff.toInt()
    }

    private fun initPath() {
        if (null == mPath) {
            mPath = Path()
            val width = this.width
            val height = this.height
            if (mbCircle) {
                mRound = width / 2
            }
            mPath!!.addRoundRect(RectF(0F, 0F, width.toFloat(), height.toFloat()), mRound.toFloat(), mRound.toFloat(), Path.Direction.CW)
        }
    }

    override fun draw(canvas: Canvas) {
        initPath()
        canvas.clipPath(mPath!!)
        super.draw(canvas)
    }

    init {
        if (!isInEditMode) {
            init()
        }
    }
}