package dji.v5.ux.core.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import dji.v5.utils.common.AndUtil
import dji.v5.ux.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val DEFAULT_MIN = 0f
private const val DEFAULT_MAX = 100f
private const val DEFAULT_STEP = 1f
private const val DEFAULT_GAP = 0f

private const val INVALID_POINTER_ID = 255

class BasicRangeSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /* Attrs values */
    private val leftThumbImg: Bitmap
    private val rightThumbImg: Bitmap
    private var rightThumbOffset = 14
    private var leftThumbOffset = 14


    var minValue = DEFAULT_MIN
        private set
    private var maxValue = DEFAULT_MAX
    private var stepValue = DEFAULT_STEP
    private var gapValue = DEFAULT_GAP

    private var lowerBoundaryValue = DEFAULT_MAX
    private var largerBoundaryValue = DEFAULT_MIN

    @ColorInt
    var leftSideBarColor = AndUtil.getResColor(R.color.uxsdk_checklist_seekbar_red_color)
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    var centerBarColor = AndUtil.getResColor(R.color.uxsdk_checklist_warning_bg_color)
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    var rightSideBarColor = AndUtil.getResColor(R.color.uxsdk_checklist_seekbar_gray_color)
        set(value) {
            field = value
            invalidate()
        }

    var listener: OnRangeSeekBarListener? = null

    var isShowLeftThumb = true
        set(value) {
            field = value
            invalidate()
        }

    private var barHeight = 0
    private var thumbLeftPointerPadding = 0
    private var thumbRightPointerPadding = 24
    private var thumbBackPadding = 0

    /* System values */
    private var isDragging = false

    private var stepsCount = 0
    private val scaledTouchSlop by lazy { ViewConfiguration.get(getContext()).scaledTouchSlop }

    private var activePointerId = INVALID_POINTER_ID
    private var downMotionX = 0f

    private var leftPadding = 0f
    private var rightPadding = 0f

    private var normalizedMinValue = 0.0
    private var normalizedMaxValue = 1.0
    private var normalizedGapValue = 0.0
    private var normalizedLowerBoundaryValue = 1.0
    private var normalizedLargerBoundaryValue = 0.0

    private var pressedThumb: Thumb? = null

    companion object {
        private val srcThumbRect: Rect = Rect()
        private val dstThumbRectF: RectF = RectF()
        private val center: Point = Point(0, 0)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        context.resources.apply {
            leftThumbImg = toBitmap(ContextCompat.getDrawable(context,R.drawable.uxsdk_ic_slider_block_red)!!)
            rightThumbImg = toBitmap(ContextCompat.getDrawable(context,R.drawable.uxsdk_ic_slider_block_orange)!!)
        }
        thumbBackPadding = AndUtil.getDimension(R.dimen.uxsdk_5_dp).toInt()
        thumbLeftPointerPadding = AndUtil.getDimension(R.dimen.uxsdk_4_dp).toInt()

        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.BasicRangeSeekBar).apply {
                centerBarColor = getColor(R.styleable.BasicRangeSeekBar_uxsdk_bar_center_color, centerBarColor)
                leftSideBarColor = getColor(R.styleable.BasicRangeSeekBar_uxsdk_bar_left_side_color, leftSideBarColor)
                rightSideBarColor = getColor(R.styleable.BasicRangeSeekBar_uxsdk_bar_right_side_color, rightSideBarColor)

                minValue = getFloat(R.styleable.BasicRangeSeekBar_uxsdk_bar_min_value, minValue)
                maxValue = getFloat(R.styleable.BasicRangeSeekBar_uxsdk_bar_max_value, maxValue)
                stepValue = getFloat(R.styleable.BasicRangeSeekBar_uxsdk_bar_step_value, stepValue)
                if (maxValue < minValue) throw Exception("Min value can't be higher than max value")
                if (!stepValueValidation(minValue, maxValue, stepValue)) throw Exception("Incorrect min/max/step, it must be: (maxValue - minValue) % stepValue == 0f")
                stepsCount = ((maxValue - minValue) / stepValue).toInt()
                barHeight = getDimensionPixelSize(R.styleable.BasicRangeSeekBar_uxsdk_bar_height, barHeight)
                recycle()
            }
        }

        isFocusable = true
        isFocusableInTouchMode = true
    }

    @Suppress("NAME_SHADOWING")
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) return false
        event?.let { event ->
            val pointerIndex: Int
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    activePointerId = event.getPointerId(event.pointerCount - 1)
                    pointerIndex = event.findPointerIndex(activePointerId)
                    downMotionX = event.getX(pointerIndex)

                    pressedThumb = evalPressedThumb(downMotionX)
                    pressedThumb ?: return super.onTouchEvent(event)

                    isPressed = true
                    invalidate()
                    isDragging = true
                    trackTouchEvent(event)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    println()
                }

                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event)
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        trackTouchEvent(event)
                        isDragging = false
                        isPressed = false
                    } else {
                        isDragging = true
                        trackTouchEvent(event)
                        isDragging = false
                    }

                    listener?.onValuesChanged(getSelectedMinValue().toFloat(), getSelectedMaxValue().toFloat())

                    pressedThumb = null
                    invalidate()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val index = event.pointerCount - 1
                    downMotionX = event.getX(index)
                    activePointerId = event.getPointerId(index)
                    invalidate()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    onSecondaryPointerUp(event)
                    invalidate()
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) isDragging = false; isPressed = false
                    invalidate()
                }
            }
        } ?: run { return false }
        return true
    }

    private fun onActionMove(event: MotionEvent) {
        pressedThumb?.let {
            val pointerIndex: Int
            listener?.onValuesChanging(getSelectedMinValue().toFloat(), getSelectedMaxValue().toFloat())

            if (isDragging) {
                trackTouchEvent(event)
            } else {
                pointerIndex = event.findPointerIndex(activePointerId)
                val x = event.getX(pointerIndex)

                if (abs(x - downMotionX) > scaledTouchSlop) {
                    isPressed = true
                    invalidate()
                    isDragging = true
                    trackTouchEvent(event)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    println()
                }
            }
        }
        println()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        leftPadding = (leftThumbImg.width + thumbLeftPointerPadding).toFloat()
        rightPadding = (width - rightThumbImg.width - thumbRightPointerPadding).toFloat()

        center.set(width / 2, height / 2)

        val top = center.y - barHeight / 2f
        val bottom = center.y + barHeight / 2f

        val minScreenPos = normalizedToScreen(normalizedMinValue)
        val maxScreenPos = normalizedToScreen(normalizedMaxValue)

        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        canvas?.apply {
            /* left rect */
            val rectUnusedArea = RectF()
            rectUnusedArea.top = top
            rectUnusedArea.bottom = bottom
            rectUnusedArea.left = leftPadding
            rectUnusedArea.right = minScreenPos

            paint.color = if (isShowLeftThumb) leftSideBarColor else centerBarColor

            val bufRect = RectF(rectUnusedArea)
            bufRect.left += 10
            drawRect(bufRect, paint)
            drawRoundRect(rectUnusedArea, 10f, 10f, paint)

            /* right rect */
            rectUnusedArea.right = rightPadding
            rectUnusedArea.left = maxScreenPos

            paint.color = rightSideBarColor

            val bufRect1 = RectF(rectUnusedArea)
            bufRect1.right -= 10
            drawRect(bufRect1, paint)
            drawRoundRect(rectUnusedArea, 10f, 10f, paint)

            rectUnusedArea.left = minScreenPos
            rectUnusedArea.right = maxScreenPos

            paint.color = centerBarColor

            drawRect(rectUnusedArea, paint)

            /* draw thumb */
            if (isShowLeftThumb) {
                drawLeftThumb(minScreenPos, canvas)
            }
            drawRightThumb(maxScreenPos, canvas)
        }
    }

    private fun stepValueValidation(minValue: Float, maxValue: Float, stepValue: Float) =
        (maxValue.toBigDecimal() - minValue.toBigDecimal()) % stepValue.toBigDecimal() == 0f.toBigDecimal()

    private fun drawLeftThumb(screenCoord: Float, canvas: Canvas) {
        srcThumbRect.set(0, 0, leftThumbImg.width, leftThumbImg.height)
        dstThumbRectF.set(
            screenCoord - leftThumbImg.width + leftThumbOffset,
            center.y.toFloat() - leftThumbImg.height * 0.5f,
            screenCoord + leftThumbOffset,
            center.y.toFloat() + leftThumbImg.height * 0.5f
        )
        canvas.drawBitmap(leftThumbImg, srcThumbRect, dstThumbRectF, paint)
    }

    private fun drawRightThumb(screenCoord: Float, canvas: Canvas) {
        srcThumbRect.set(0, 0, rightThumbImg.width, rightThumbImg.height)
        dstThumbRectF.set(
            screenCoord - rightThumbOffset,
            center.y.toFloat() - rightThumbImg.height * 0.5f,
            screenCoord + rightThumbImg.width - rightThumbOffset,
            center.y.toFloat() + rightThumbImg.height * 0.5f
        )
        canvas.drawBitmap(rightThumbImg, srcThumbRect, dstThumbRectF, paint)
    }

    private fun evalPressedThumb(touchX: Float): Thumb? {
        val minThumbPressed = isInLeftThumbRange(touchX)
        val maxThumbPressed = isInRightThumbRange(touchX)
        return if (minThumbPressed && maxThumbPressed) {
            if (isShowLeftThumb && touchX / width > 0.5f) Thumb.MIN else Thumb.MAX
        } else if (isShowLeftThumb && minThumbPressed) {
            Thumb.MIN
        } else if (maxThumbPressed) {
            Thumb.MAX
        } else null
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        (ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT).apply {
            if (ev.getPointerId(this) == activePointerId) {
                val newPointerIndex = if (this == 0) 1 else 0
                downMotionX = ev.getX(newPointerIndex)
                activePointerId = ev.getPointerId(newPointerIndex)
            }
        }
    }

    private fun isInLeftThumbRange(touchX: Float) = normalizedToScreen(normalizedMinValue) - touchX in -4f..(leftThumbImg.width + 40f)
    private fun isInRightThumbRange(touchX: Float) = touchX - normalizedToScreen(normalizedMaxValue) in -4f..(rightThumbImg.width + 40f)

    private fun normalizedToScreen(normalizedPos: Double) = (leftPadding + normalizedPos * (width - leftPadding * 2)).toFloat()


    private fun trackTouchEvent(event: MotionEvent) {
        val pointerIndex = event.findPointerIndex(activePointerId)
        val x = event.getX(pointerIndex)

        if (Thumb.MIN == pressedThumb) {
            setNormalizedMinValue(screenToNormalized(x))
        } else if (Thumb.MAX == pressedThumb) {
            setNormalizedMaxValue(screenToNormalized(x))
        }
    }

    private fun setNormalizedMinValue(value: Double) {
        normalizedMinValue = max(0.0, min(1.0, min(value, min(normalizedMaxValue + normalizedGapValue, normalizedLowerBoundaryValue))))
        invalidate()
    }

    private fun setNormalizedMaxValue(value: Double) {
        normalizedMaxValue = max(0.0, min(1.0, max(value, max(normalizedMinValue - normalizedGapValue, normalizedLargerBoundaryValue))))
        invalidate()
    }

    private fun screenToNormalized(screenPos: Float): Double {
        val width = width
        return if (width <= leftPadding * 2) {
            0.0 //divide by zero safe
        } else {
            val result = ((screenPos - leftPadding) / (width - leftPadding * 2)).toDouble()
            min(1.0, max(0.0, result))
        }
    }

    private fun valueToNormalize(value: Double) = ((maxValue - minValue) * (value * 100)) / 100 + minValue
    private fun normalizeToValue(value: Float) = (((value - minValue) * 100) / (maxValue - minValue)) / 100

    private fun getSelectedMinValue() = getValueAccordingToStep(valueToNormalize(normalizedMinValue))
    private fun getSelectedMaxValue() = getValueAccordingToStep(valueToNormalize(normalizedMaxValue))

    private fun getValueAccordingToStep(value: Double) = ((value.toBigDecimal() / stepValue.toBigDecimal()).toDouble().roundToInt()).toBigDecimal() * stepValue.toBigDecimal()

    private fun resetRange() {
        if (maxValue < minValue) throw Exception("Min value can't be higher than max value")
        if (!stepValueValidation(minValue, maxValue, stepValue)) throw Exception("Incorrect min/max/step, it must be: (maxValue - minValue) % stepValue == 0f")

        normalizedMinValue = 0.0
        normalizedMaxValue = 1.0
        normalizedGapValue = normalizeToValue(gapValue).toDouble()
        normalizedLowerBoundaryValue = normalizeToValue(lowerBoundaryValue).toDouble()
        normalizedLargerBoundaryValue = normalizeToValue(largerBoundaryValue).toDouble()


        invalidate()
    }

    fun setRange(minValue: Float, maxValue: Float, stepValue: Float) {
        setRange(minValue, maxValue, stepValue, DEFAULT_GAP, DEFAULT_MAX, DEFAULT_MIN)
    }

    fun setRange(minValue: Float, maxValue: Float, stepValue: Float, gapValue: Float) {
        setRange(minValue, maxValue, stepValue, gapValue, DEFAULT_MAX, DEFAULT_MIN)
    }

    fun setRange(
        minValue: Float, maxValue: Float, stepValue: Float,
        gapValue: Float, lowerBoundaryValue: Float, largerBoundaryValue: Float
    ) {
        val notChange = this.minValue.compareTo(minValue) == 0
                && this.maxValue.compareTo(maxValue) == 0
                && this.stepValue.compareTo(stepValue) == 0
                && this.gapValue.compareTo(gapValue) == 0
                && this.lowerBoundaryValue.compareTo(lowerBoundaryValue) == 0
                && this.largerBoundaryValue.compareTo(largerBoundaryValue) == 0
        if (notChange) {
            return
        }
        this.minValue = minValue
        this.maxValue = maxValue
        this.stepValue = stepValue
        this.gapValue = gapValue
        this.lowerBoundaryValue = lowerBoundaryValue
        this.largerBoundaryValue = largerBoundaryValue

        resetRange()
    }

    fun setCurrentValues(leftValue: Float, rightValue: Float) {
        if (leftValue > rightValue) throw Exception("LeftValue $leftValue can't be higher than rightValue $rightValue")
        if (leftValue < minValue || rightValue > maxValue) throw Exception("$leftValue or $rightValue Out of range[$minValue, $maxValue]")
        if (!stepValueValidation(leftValue, maxValue, stepValue) || !stepValueValidation(minValue, rightValue, stepValue)) throw Exception("You can't set these values according to your step")

        val newNormalizedMinValue = normalizeToValue(leftValue).toDouble()
        val newNormalizedMaxValue = normalizeToValue(rightValue).toDouble()
        val valueNotChanged = this.normalizedMinValue.compareTo(newNormalizedMinValue) == 0
                && this.normalizedMaxValue.compareTo(newNormalizedMaxValue) == 0
        if (valueNotChanged) {
            return
        }
        normalizedMinValue = newNormalizedMinValue
        normalizedMaxValue = newNormalizedMaxValue

        invalidate()
    }

    fun getRangeInfo() = RangeInfo(minValue, maxValue, stepValue, gapValue)

    fun getCurrentValues() = Range(getSelectedMinValue().toFloat(), getSelectedMaxValue().toFloat())

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 200
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec)
        }

        var height = leftThumbImg.height

        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = max(height, MeasureSpec.getSize(heightMeasureSpec))
        }
        setMeasuredDimension(width, height)
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("SUPER", super.onSaveInstanceState())
        bundle.putDouble("LEFT_VALUE", normalizedMinValue)
        bundle.putDouble("RIGHT_VALUE", normalizedMaxValue)

        bundle.putFloat("MIN_VALUE", minValue)
        bundle.putFloat("MAX_VALUE", maxValue)

        bundle.putFloat("STEP_VALUE", stepValue)

        bundle.putInt("SIDE_BAR_COLOR", leftSideBarColor)
        bundle.putInt("CENTER_BAR_COLOR", centerBarColor)
        bundle.putInt("TRANSITION_BAR_COLOR", rightSideBarColor)
        return bundle
    }

    override fun onRestoreInstanceState(parcel: Parcelable) {
        val bundle = parcel as Bundle
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"))
        normalizedMinValue = bundle.getDouble("LEFT_VALUE")
        normalizedMaxValue = bundle.getDouble("RIGHT_VALUE")

        minValue = bundle.getFloat("MIN_VALUE")
        maxValue = bundle.getFloat("MAX_VALUE")

        stepValue = bundle.getFloat("STEP_VALUE")

        leftSideBarColor = bundle.getInt("SIDE_BAR_COLOR")
        centerBarColor = bundle.getInt("CENTER_BAR_COLOR")
        rightSideBarColor = bundle.getInt("TRANSITION_BAR_COLOR")
        invalidate()
    }

    interface OnRangeSeekBarListener {
        fun onValuesChanging(minValue: Float, maxValue: Float)
        fun onValuesChanged(minValue: Float, maxValue: Float)
    }

    data class Range(val leftValue: Float, val rightValue: Float)
    data class RangeInfo(val minValue: Float, val maxValue: Float, val stepValue: Float, val gapValue: Float)

    private enum class Thumb {
        MIN, MAX
    }

    private fun toBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        drawable.apply {
            val width = if (!bounds.isEmpty) bounds.width() else intrinsicWidth
            val height = if (!bounds.isEmpty) bounds.height() else intrinsicHeight

            val bitmap = Bitmap.createBitmap(
                if (width <= 0) 1 else width, if (height <= 0) 1 else height,
                Bitmap.Config.ARGB_8888
            )
            Canvas(bitmap).apply {
                setBounds(0, 0, width, height)
                draw(this)
            }
            return bitmap
        }
    }
}