package radityalabs.expandedprogressanimation

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.RectF
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.gojek.life.component.R

/**
 * This code base from Nico Hormazábal then modified by radityagumay
 **/
class CircleProgressView : View {
    companion object {
        private val TAG = CircleProgressView::class.java.simpleName
    }

    private val barLength = 16
    private val barMaxLength = 270
    private val pauseGrowingTime: Long = 200

    private var circleRadius = 28
    private var barWidth = 4
    private var rimWidth = 4
    private var fillRadius = false
    private var timeStartGrowing = 0.0
    private var barSpinCycleTime = 460.0
    private var barExtraLength = 0f
    private var barGrowingFromFront = true
    private var pausedTimeWithoutGrowing: Long = 0

    private var barColor = 0xAA000000.toInt()
    private var rimColor = 0x00FFFFFF

    private val barPaint = Paint()
    private val rimPaint = Paint()

    private var circleBounds = RectF()

    private var spinSpeed = 230.0f
    private var lastTimeAnimated: Long = 0

    private var linearProgress: Boolean = false

    private var mProgress = 0.0f
    private var mTargetProgress = 0.0f

    var isSpinning = false
        private set

    private var callback: ProgressCallback? = null

    private var shouldAnimate: Boolean = false

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView))
        setAnimationEnabled()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    constructor(context: Context) : super(context) {
        setAnimationEnabled()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun setAnimationEnabled() {
        val currentApiVersion = Build.VERSION.SDK_INT
        val animationValue: Float
        animationValue = if (currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getFloat(context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } else {
            Settings.System.getFloat(context.contentResolver,
                    Settings.System.ANIMATOR_DURATION_SCALE, 1f)
        }
        shouldAnimate = animationValue != 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val viewWidth = circleRadius + this.paddingLeft + this.paddingRight
        val viewHeight = circleRadius + this.paddingTop + this.paddingBottom

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        val width: Int
        val height: Int

        width = if (widthMode == View.MeasureSpec.EXACTLY) {
            widthSize
        } else if (widthMode == View.MeasureSpec.AT_MOST) {
            Math.min(viewWidth, widthSize)
        } else {
            viewWidth
        }

        height = if (heightMode == View.MeasureSpec.EXACTLY || widthMode == View.MeasureSpec.EXACTLY) {
            heightSize
        } else if (heightMode == View.MeasureSpec.AT_MOST) {
            Math.min(viewHeight, heightSize)
        } else {
            viewHeight
        }

        setMeasuredDimension(width, height)
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * got a bug when use onAttachedToWindow
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        setupBounds(w, h)
        setupPaints()
        invalidate()
    }

    private fun setupPaints() {
        barPaint.color = barColor
        barPaint.isAntiAlias = true
        barPaint.style = Style.STROKE
        barPaint.strokeWidth = barWidth.toFloat()

        rimPaint.color = rimColor
        rimPaint.isAntiAlias = true
        rimPaint.style = Style.STROKE
        rimPaint.strokeWidth = rimWidth.toFloat()
    }

    private fun setupBounds(layout_width: Int, layout_height: Int) {
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight

        circleBounds = if (!fillRadius) {
            val minValue = Math.min(layout_width - paddingLeft - paddingRight,
                    layout_height - paddingBottom - paddingTop)

            val circleDiameter = Math.min(minValue, circleRadius * 2 - barWidth * 2)
            val xOffset = (layout_width - paddingLeft - paddingRight - circleDiameter) / 2 + paddingLeft
            val yOffset = (layout_height - paddingTop - paddingBottom - circleDiameter) / 2 + paddingTop

            RectF((xOffset + barWidth).toFloat(), (yOffset + barWidth).toFloat(), (xOffset + circleDiameter - barWidth).toFloat(),
                    (yOffset + circleDiameter - barWidth).toFloat())
        } else {
            RectF((paddingLeft + barWidth).toFloat(), (paddingTop + barWidth).toFloat(),
                    (layout_width - paddingRight - barWidth).toFloat(), (layout_height - paddingBottom - barWidth).toFloat())
        }
    }

    private fun parseAttributes(a: TypedArray) {
        /* convert dp to pixel */
        val metrics = context.resources.displayMetrics
        barWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, barWidth.toFloat(), metrics).toInt()
        circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, circleRadius.toFloat(), metrics).toInt()
        circleRadius = a.getDimension(R.styleable.CircleProgressView_matProg_circleRadius, circleRadius.toFloat()).toInt()
        fillRadius = a.getBoolean(R.styleable.CircleProgressView_matProg_fillRadius, false)
        barWidth = a.getDimension(R.styleable.CircleProgressView_matProg_barWidth, barWidth.toFloat()).toInt()
        barColor = a.getColor(R.styleable.CircleProgressView_matProg_barColor, barColor)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(circleBounds, 360f, 360f, false, rimPaint)
        var mustInvalidate = false
        if (!shouldAnimate) {
            return
        }
        if (isSpinning) {
            mustInvalidate = true

            val deltaTime = SystemClock.uptimeMillis() - lastTimeAnimated
            val deltaNormalized = deltaTime * spinSpeed / 1000.0f

            updateBarLength(deltaTime)

            mProgress += deltaNormalized
            if (mProgress > 360) {
                mProgress -= 360f
                runCallback(-1.0f)
            }
            lastTimeAnimated = SystemClock.uptimeMillis()
            var from = mProgress - 90
            var length = barLength + barExtraLength
            if (isInEditMode) {
                from = 0f
                length = 135f
            }
            canvas.drawArc(circleBounds, from, length, false, barPaint)
        } else {
            val oldProgress = mProgress
            if (mProgress != mTargetProgress) {
                mustInvalidate = true
                val deltaTime = (SystemClock.uptimeMillis() - lastTimeAnimated).toFloat() / 1000
                val deltaNormalized = deltaTime * spinSpeed
                mProgress = Math.min(mProgress + deltaNormalized, mTargetProgress)
                lastTimeAnimated = SystemClock.uptimeMillis()
            }

            if (oldProgress != mProgress) {
                runCallback()
            }

            var offset = 0.0f
            var progress = mProgress
            if (!linearProgress) {
                val factor = 2.0f
                offset = (1.0f - Math.pow((1.0f - mProgress / 360.0f).toDouble(), (2.0f * factor).toDouble())).toFloat() * 360.0f
                progress = (1.0f - Math.pow((1.0f - mProgress / 360.0f).toDouble(), factor.toDouble())).toFloat() * 360.0f
            }

            if (isInEditMode) {
                progress = 360f
            }

            canvas.drawArc(circleBounds, offset - 90, progress, false, barPaint)
        }

        if (mustInvalidate) {
            invalidate()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility == View.VISIBLE) {
            lastTimeAnimated = SystemClock.uptimeMillis()
        }
    }

    private fun updateBarLength(deltaTimeInMilliSeconds: Long) {
        if (pausedTimeWithoutGrowing >= pauseGrowingTime) {
            timeStartGrowing += deltaTimeInMilliSeconds.toDouble()
            if (timeStartGrowing > barSpinCycleTime) {
                timeStartGrowing -= barSpinCycleTime
                pausedTimeWithoutGrowing = 0
                barGrowingFromFront = !barGrowingFromFront
            }

            val distance = Math.cos((timeStartGrowing / barSpinCycleTime + 1) * Math.PI).toFloat() / 2 + 0.5f
            val destLength = (barMaxLength - barLength).toFloat()

            if (barGrowingFromFront) {
                barExtraLength = distance * destLength
            } else {
                val newLength = destLength * (1 - distance)
                mProgress += barExtraLength - newLength
                barExtraLength = newLength
            }
        } else {
            pausedTimeWithoutGrowing += deltaTimeInMilliSeconds
        }
    }

    fun stopSpinning() {
        isSpinning = false
        mProgress = 0.0f
        mTargetProgress = 0.0f
        invalidate()
    }

    fun spin() {
        lastTimeAnimated = SystemClock.uptimeMillis()
        isSpinning = true
        invalidate()
    }

    private fun runCallback(value: Float) {
        if (callback != null) {
            callback!!.onProgressUpdate(value)
        }
    }

    private fun runCallback() {
        if (callback != null) {
            val normalizedProgress = Math.round(mProgress * 100 / 360.0f).toFloat() / 100
            callback!!.onProgressUpdate(normalizedProgress)
        }
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = WheelSavedState(superState)
        ss.mProgress = this.mProgress
        ss.mTargetProgress = this.mTargetProgress
        ss.isSpinning = this.isSpinning
        ss.spinSpeed = this.spinSpeed
        ss.barWidth = this.barWidth
        ss.barColor = this.barColor
        ss.rimWidth = this.rimWidth
        ss.rimColor = this.rimColor
        ss.circleRadius = this.circleRadius
        ss.linearProgress = this.linearProgress
        ss.fillRadius = this.fillRadius

        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is WheelSavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        this.mProgress = state.mProgress
        this.mTargetProgress = state.mTargetProgress
        this.isSpinning = state.isSpinning
        this.spinSpeed = state.spinSpeed
        this.barWidth = state.barWidth
        this.barColor = state.barColor
        this.rimWidth = state.rimWidth
        this.rimColor = state.rimColor
        this.circleRadius = state.circleRadius
        this.linearProgress = state.linearProgress
        this.fillRadius = state.fillRadius

        this.lastTimeAnimated = SystemClock.uptimeMillis()
    }

    var progress: Float
        get() = if (isSpinning) -1f else mProgress / 360.0f
        set(progress) {
            var progress = progress
            if (isSpinning) {
                mProgress = 0.0f
                isSpinning = false

                runCallback()
            }

            if (progress > 1.0f) {
                progress -= 1.0f
            } else if (progress < 0) {
                progress = 0f
            }

            if (progress == mTargetProgress) {
                return
            }
            if (mProgress == mTargetProgress) {
                lastTimeAnimated = SystemClock.uptimeMillis()
            }

            mTargetProgress = Math.min(progress * 360.0f, 360.0f)

            invalidate()
        }

    interface ProgressCallback {
        fun onProgressUpdate(progress: Float)
    }

    internal class WheelSavedState : View.BaseSavedState {
        companion object {
            val CREATOR: Parcelable.Creator<WheelSavedState> = object : Parcelable.Creator<WheelSavedState> {
                override fun createFromParcel(`in`: Parcel): WheelSavedState = WheelSavedState(`in`)

                override fun newArray(size: Int): Array<WheelSavedState?> = arrayOfNulls(size)
            }
        }

        var mProgress: Float = 0.toFloat()
        var mTargetProgress: Float = 0.toFloat()
        var isSpinning: Boolean = false
        var spinSpeed: Float = 0.toFloat()
        var barWidth: Int = 0
        var barColor: Int = 0
        var rimWidth: Int = 0
        var rimColor: Int = 0
        var circleRadius: Int = 0
        var linearProgress: Boolean = false
        var fillRadius: Boolean = false

        constructor(superState: Parcelable) : super(superState) {}

        private constructor(`in`: Parcel) : super(`in`) {
            this.mProgress = `in`.readFloat()
            this.mTargetProgress = `in`.readFloat()
            this.isSpinning = `in`.readByte().toInt() != 0
            this.spinSpeed = `in`.readFloat()
            this.barWidth = `in`.readInt()
            this.barColor = `in`.readInt()
            this.rimWidth = `in`.readInt()
            this.rimColor = `in`.readInt()
            this.circleRadius = `in`.readInt()
            this.linearProgress = `in`.readByte().toInt() != 0
            this.fillRadius = `in`.readByte().toInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(this.mProgress)
            out.writeFloat(this.mTargetProgress)
            out.writeByte((if (isSpinning) 1 else 0).toByte())
            out.writeFloat(this.spinSpeed)
            out.writeInt(this.barWidth)
            out.writeInt(this.barColor)
            out.writeInt(this.rimWidth)
            out.writeInt(this.rimColor)
            out.writeInt(this.circleRadius)
            out.writeByte((if (linearProgress) 1 else 0).toByte())
            out.writeByte((if (fillRadius) 1 else 0).toByte())
        }
    }
}
