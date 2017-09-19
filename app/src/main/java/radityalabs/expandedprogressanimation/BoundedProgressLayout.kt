package radityalabs.expandedprogressanimation

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import android.widget.TextView

interface Action {
    fun isLoading(): Boolean

    fun extended()

    fun collapsed()

    fun setAddress(address: String)
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

    override fun extended() {
        isLoading = true

        ViewAnimator.animate(container)
                .dp()
                .width(60f, 250f)
                .interpolator(DecelerateInterpolator())
                .duration(800)
                .onStop {
                    mAddress?.let {
                        address.visibility = View.VISIBLE
                        address.text = "Jakarta Pusat"
                        ViewAnimator.animate(address)
                                .dp().translationY(50f, 0f)
                                .alpha(0.1f, 1f)
                                .singleInterpolator(OvershootInterpolator())
                                .duration(800)
                                .start()
                    }
                }
                .start()
        ViewAnimator
                .animate(container).scaleY(0f, 1f).decelerate().duration(500)
    }

    override fun collapsed() {
        isLoading = false

        ViewAnimator.animate(address)
                .dp().translationY(0f, 50f)
                .alpha(1f, 0.1f)
                .singleInterpolator(OvershootInterpolator())
                .duration(800)
                .onStart {
                    ViewAnimator.animate(container)
                            .dp()
                            .width(250f, 60f)
                            .interpolator(AccelerateInterpolator())
                            .duration(1200)
                            .start()
                    ViewAnimator
                            .animate(container).scaleY(1f, 0f).accelerate().duration(800)
                }
                .onStop { address.visibility = View.GONE }
                .start()
    }

    override fun setAddress(address: String) {
        this.mAddress = address
    }

    override fun isLoading() = isLoading

    override fun onFinishInflate() {
        super.onFinishInflate()
        container = findViewById(R.id.container)
        address = findViewById(R.id.address)
        initView()
    }

    private fun initView() {
        val iconBackground = GradientDrawable()
        iconBackground.cornerRadius = 100f
        iconBackground.setColor(resources.getColor(R.color.colorAccent))
        container.background = iconBackground

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            container.elevation = 4f
        }
    }
}