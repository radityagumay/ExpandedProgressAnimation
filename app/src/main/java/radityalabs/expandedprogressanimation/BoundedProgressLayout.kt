package radityalabs.expandedprogressanimation

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

interface Action {
    fun isLoading(): Boolean

    fun extended()

    fun collapsed()

    fun setAddress(address: String)

    fun onProgess()

    fun onCompleted()

    fun setIcon(drawable: Int)

    fun setBackground(color: Int)

    fun setBackgroundElevation(elevation: Int)

    fun setCornerRadius(radius: Float)
}

class BoundedProgressLayout @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyle: Int = 0
) : RelativeLayout(context, attributeSet, defStyle), Action {

    init {
        LayoutInflater.from(context).inflate(R.layout.component_bounded_progress_layout, this, true)
    }

    private var mAddress: String? = null

    private var isLoading = false

    private lateinit var container: RelativeLayout
    private lateinit var address: TextView
    private lateinit var circle: ImageView
    private lateinit var progressWheel: CircleProgressView

    private var backgroundDrawable: GradientDrawable? = null

    override fun onSaveInstanceState(): Parcelable {
        val savedState = super.onSaveInstanceState()
        val ourState = BoundedProgressSavedState(savedState)
        ourState.mAddress = this.mAddress
        return ourState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is BoundedProgressSavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        this.mAddress = state.mAddress
    }

    override fun extended() {
        isLoading = true
        ViewAnimator.animate(container)
                .dp()
                .width(60f, 250f)
                .interpolator(DecelerateInterpolator())
                .duration(800)
                .onStart(object : AnimationListener.Start {
                    override fun onStart() {
                        onProgess()
                    }
                })
                .onStop(object : AnimationListener.Stop {
                    override fun onStop() {
                        mAddress?.let {
                            address.visibility = View.VISIBLE
                            address.text = mAddress
                            ViewAnimator.animate(address)
                                    .dp().translationY(50f, 0f)
                                    .alpha(0.1f, 1f)
                                    .singleInterpolator(OvershootInterpolator())
                                    .duration(800)
                                    .start()
                        }
                    }
                }).start()
        ViewAnimator
                .animate(container).scaleY(0f, 1f).decelerate().duration(500)
    }

    override fun collapsed() {
        isLoading = false

        ViewAnimator.animate(address)
                .dp().translationY(0f, 50f)
                .alpha(1f, 0.1f)
                .singleInterpolator(OvershootInterpolator())
                .duration(600)
                .onStart(object : AnimationListener.Start {
                    override fun onStart() {
                        ViewAnimator.animate(container)
                                .dp()
                                .width(250f, 60f)
                                .interpolator(AccelerateInterpolator())
                                .duration(900)
                                .onStop(object : AnimationListener.Stop {
                                    override fun onStop() {
                                        onCompleted()
                                    }
                                })
                                .start()
                        ViewAnimator
                                .animate(container).scaleY(1f, 0f).accelerate().duration(600)
                    }
                })
                .onStop(object : AnimationListener.Stop {
                    override fun onStop() {
                        address.visibility = View.GONE
                    }
                })
                .start()
    }

    override fun setAddress(address: String) {
        this.mAddress = address
    }

    override fun setIcon(drawable: Int) {
        circle.setImageResource(drawable)
    }

    override fun setCornerRadius(radius: Float) {
        container.background.apply {
            with(backgroundDrawable!!) {
                cornerRadius = radius
            }
        }
    }

    override fun setBackground(color: Int) {
        backgroundDrawable?.let {
            container.background.apply {
                with(backgroundDrawable!!) {
                    setColor(color)
                }
            }
        }
    }

    override fun onProgess() {
        if (!progressWheel.isSpinning) {
            progressWheel.spin()
        }
    }

    override fun onCompleted() {
        if (progressWheel.isSpinning) {
            progressWheel.stopSpinning()
        }
    }

    override fun setBackgroundElevation(elevation: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            container.elevation = 4f
        }
    }

    override fun isLoading() = isLoading

    override fun onFinishInflate() {
        super.onFinishInflate()
        container = findViewById(R.id.container)
        address = findViewById(R.id.address)
        circle = findViewById(R.id.circle)
        progressWheel = findViewById(R.id.progressWheel)
        initView()
    }

    private fun initView() {
        backgroundDrawable = GradientDrawable()
        backgroundDrawable?.cornerRadius = 100f
        backgroundDrawable?.setColor(Color.WHITE)
        container.background = backgroundDrawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            container.elevation = 4f
        }

        circle.setImageResource(R.drawable.ic_location_on)
    }

    internal class BoundedProgressSavedState : View.BaseSavedState {

        var mAddress: String? = null

        constructor(savedState: Parcelable) : super(savedState)

        private constructor(inParcel: Parcel) : super(inParcel) {
            this.mAddress = inParcel.readString()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeString(mAddress)
        }


        companion object CREATOR : Parcelable.Creator<BoundedProgressSavedState> {
            override fun createFromParcel(parcel: Parcel): BoundedProgressSavedState =
                    BoundedProgressSavedState(parcel)

            override fun newArray(size: Int): Array<BoundedProgressSavedState?> = arrayOfNulls(size)
        }
    }
}