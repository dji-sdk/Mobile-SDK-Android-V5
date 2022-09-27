package dji.v5.ux.core.popover

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView
import com.dji.industry.pandora.pilot2.uikit.popover.TintedBitmapDrawable
import dji.v5.ux.R


class PopoverView(context: Context,
                  private var popoverBackgroundColor: Int = Color.parseColor("#FFCC00"),
                  private var arrowColor: Int = popoverBackgroundColor,
                  private var borderRadius: Float = 0F,
                  private var showArrow: Boolean = true,
                  private var arrowOffset: Float = 0.5F,
                  private var arrowPosition: ArrowPosition = ArrowPosition.TOP
): FrameLayout(context) {

    // 箭头位置
    enum class ArrowPosition {
        TOP,
        RIGHT,
        BOTTOM,
        LEFT
    }
    // 箭头旋转的角度
    private var arrowAngle = 270F

    lateinit var arrowImageView: ImageView
    lateinit var contentLayout: CardView

    // 阴影
    var shadowPaint = Paint()
    var shadowDx: Float = 0F
    var shadowDy: Float = 0F
    var shadowRectF = RectF()
    var enableDropShadow = false


    init {
        arrowAngle = when(arrowPosition) {
            ArrowPosition.TOP ->270F
            ArrowPosition.RIGHT ->0F
            ArrowPosition.BOTTOM ->90F
            ArrowPosition.LEFT ->180F
        }

        initViews()
    }

    private fun getArrowDrawable(): Drawable {
        val arrowRes: Int = R.drawable.uxsdk_ic_themedark_popover_arrow_left
        val source = BitmapFactory.decodeResource(this.resources, arrowRes)

        val rotateBitmap = rotateBitmap(source, arrowAngle)

        return TintedBitmapDrawable(resources, rotateBitmap, arrowColor)
    }

    private fun initViews() {

        contentLayout = CardView(context)
        contentLayout.radius = borderRadius
        contentLayout.cardElevation = 0F
        contentLayout.isClickable = true

        contentLayout.id = View.generateViewId()
        contentLayout.setCardBackgroundColor(popoverBackgroundColor)

        val contentLayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        arrowImageView = ImageView(context)
        arrowImageView.id = View.generateViewId()
        arrowImageView.setImageDrawable(getArrowDrawable())

        val arrowLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        arrowImageView.measure(UNSPECIFIED, UNSPECIFIED)

        if (showArrow) {
            when (arrowPosition) {
                ArrowPosition.TOP -> {
                    arrowLayoutParams.gravity = Gravity.TOP
                    contentLayoutParams.topMargin = arrowImageView.measuredHeight
                }
                ArrowPosition.RIGHT -> {
                    arrowLayoutParams.gravity = Gravity.RIGHT
                    contentLayoutParams.rightMargin = arrowImageView.measuredWidth
                }
                ArrowPosition.BOTTOM -> {
                    arrowLayoutParams.gravity = Gravity.BOTTOM
                    contentLayoutParams.bottomMargin = arrowImageView.measuredHeight
                }
                ArrowPosition.LEFT -> {
                    arrowLayoutParams.gravity = Gravity.LEFT
                    contentLayoutParams.leftMargin = arrowImageView.measuredWidth
                }
            }
        }

        addView(arrowImageView, arrowLayoutParams)
        addView(contentLayout, contentLayoutParams)

        post(this::updateArrow)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post(this::updateArrow)
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }


    private fun updateArrow() {
        if (!showArrow) {
            arrowImageView.visibility = GONE
            return
        }
        val layoutParams = arrowImageView.layoutParams as LayoutParams
        when(arrowPosition) {
            ArrowPosition.TOP,
            ArrowPosition.BOTTOM
            -> {
                val x = contentLayout.width * arrowOffset - arrowImageView.width/2
                layoutParams.leftMargin = x.toInt()
            }
            ArrowPosition.LEFT,
            ArrowPosition.RIGHT
            -> {
                val y = contentLayout.height * arrowOffset - arrowImageView.height/2
                layoutParams.topMargin = y.toInt()
            }
        }
        arrowImageView.layoutParams = layoutParams
    }

    fun setArrowOffset(arrowOffset: Float): PopoverView {
        this.arrowOffset = arrowOffset
        updateArrow()
        return this
    }

    fun setContentView(view: View): PopoverView {
        if (view.parent != null && view.parent is ViewGroup) {
            (view.parent as ViewGroup).removeView(view)
        }
        this.contentLayout.addView(view)
        return this
    }

    fun setContentView(view: View, layoutParams: ViewGroup.LayoutParams): PopoverView {
        if (view.parent != null && view.parent is ViewGroup) {
            (view.parent as ViewGroup).removeView(view)
        }
        this.contentLayout.addView(view, layoutParams)
        return this
    }

    fun setDropShadow(blurRadius: Float, dx: Float, dy: Float, color: Int) {
        // Android 10以下系统不能用硬件加速，否则BlurMaskFilter不生效
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        enableDropShadow = true
        shadowDx = dx
        shadowDy = dy
        val blurMaskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        shadowPaint.run {
            style = Paint.Style.FILL
            isAntiAlias = true
            shadowPaint.color = color
            maskFilter = blurMaskFilter
        }

        val padding = (blurRadius * 2).toInt()
        setPadding(padding, padding, padding, padding)
        setWillNotDraw(false)
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (!enableDropShadow) {
            return
        }

        shadowRectF.set(contentLayout.left + shadowDx,
            contentLayout.top + shadowDy,
            contentLayout.right + shadowDx,
            contentLayout.bottom + shadowDy)

        canvas?.save()

        canvas?.drawRoundRect(shadowRectF, contentLayout.radius, contentLayout.radius, shadowPaint)

        canvas?.restore()

    }
}