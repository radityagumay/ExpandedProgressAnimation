package radityalabs.expandedprogressanimation

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.support.annotation.IntRange
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import java.util.*

interface AnimationListener {
    interface Start {
        fun onStart()
    }

    interface Stop {
        fun onStop()
    }

    interface Update<in V : Any> {
        fun update(view: V, value: Float)
    }
}

class AnimationBuilder(val viewAnimator: ViewAnimator, val views: Array<View>) {
    private val animatorList = ArrayList<Animator>()
    private var nextValueWillBeDp = false

    var isWaitForHeight: Boolean = false
        private set

    var singleInterpolator: Interpolator? = null
        private set

    fun dp(): AnimationBuilder {
        nextValueWillBeDp = true
        return this
    }

    fun property(propertyName: String, vararg values: Float): AnimationBuilder {
        for (view in views) {
            this.animatorList.add(ObjectAnimator.ofFloat(view, propertyName, *getValues(*values)))
        }
        return this
    }

    fun translationY(vararg y: Float): AnimationBuilder = property("translationY", *y)

    fun translationX(vararg x: Float): AnimationBuilder = property("translationX", *x)

    fun alpha(vararg alpha: Float): AnimationBuilder = property("alpha", *alpha)

    fun scaleX(vararg scaleX: Float): AnimationBuilder = property("scaleX", *scaleX)

    fun scaleY(vararg scaleY: Float): AnimationBuilder = property("scaleY", *scaleY)

    fun backgroundColor(vararg colors: Int): AnimationBuilder {
        for (view in views) {
            val objectAnimator = ObjectAnimator.ofInt(view, "backgroundColor", *colors)
            objectAnimator.setEvaluator(ArgbEvaluator())
            this.animatorList.add(objectAnimator)
        }
        return this
    }

    fun custom(update: AnimationListener.Update<View>?, vararg values: Float): AnimationBuilder {
        for (view in views) {
            val valueAnimator = ValueAnimator.ofFloat(*getValues(*values))
            if (update != null)
                valueAnimator.addUpdateListener { animation -> update.update(view, animation.animatedValue as Float) }
            add(valueAnimator)
        }
        return this
    }

    fun height(vararg height: Float): AnimationBuilder {
        return custom(object : AnimationListener.Update<View> {
            override fun update(view: View, value: Float) {
                view.layoutParams.height = value.toInt()
                view.requestLayout()
            }
        }, *height)
    }

    fun width(vararg width: Float): AnimationBuilder {
        return custom(object : AnimationListener.Update<View> {
            override fun update(view: View, value: Float) {
                view.layoutParams.width = value.toInt()
                view.requestLayout()
            }
        }, *width)
    }

    fun waitForHeight(): AnimationBuilder {
        isWaitForHeight = true
        return this
    }

    fun createAnimators(): List<Animator> = animatorList

    fun andAnimate(vararg views: View): AnimationBuilder = viewAnimator.addAnimationBuilder(*views)

    fun thenAnimate(vararg views: View): AnimationBuilder = viewAnimator.thenAnimate(*views)

    fun duration(duration: Long): AnimationBuilder {
        viewAnimator.duration(duration)
        return this
    }

    fun startDelay(startDelay: Long): AnimationBuilder {
        viewAnimator.startDelay(startDelay)
        return this
    }

    fun repeatCount(@IntRange(from = -1) repeatCount: Int): AnimationBuilder {
        viewAnimator.repeatCount(repeatCount)
        return this
    }

    fun repeatMode(@ViewAnimator.RepeatMode repeatMode: Int): AnimationBuilder {
        viewAnimator.repeatMode(repeatMode)
        return this
    }

    fun onStart(startListener: AnimationListener.Start): AnimationBuilder {
        viewAnimator.onStart(startListener)
        return this
    }

    fun onStop(stopListener: AnimationListener.Stop): AnimationBuilder {
        viewAnimator.onStop(stopListener)
        return this
    }

    fun interpolator(interpolator: Interpolator): AnimationBuilder {
        viewAnimator.interpolator(interpolator)
        return this
    }

    fun singleInterpolator(interpolator: Interpolator): AnimationBuilder {
        singleInterpolator = interpolator
        return this
    }

    fun accelerate(): ViewAnimator = viewAnimator.interpolator(AccelerateInterpolator())

    fun decelerate(): ViewAnimator = viewAnimator.interpolator(DecelerateInterpolator())

    fun start(): ViewAnimator {
        viewAnimator.start()
        return viewAnimator
    }

    fun add(animator: Animator): AnimationBuilder {
        this.animatorList.add(animator)
        return this
    }

    private fun toDp(px: Float): Float = px / views[0].context.resources.displayMetrics.density

    private fun toPx(dp: Float): Float = dp * views[0].context.resources.displayMetrics.density

    private fun getValues(vararg values: Float): FloatArray {
        if (!nextValueWillBeDp) {
            return values
        }
        val pxValues = FloatArray(values.size)
        for (i in values.indices) {
            pxValues[i] = toPx(values[i])
        }
        return pxValues
    }
}
