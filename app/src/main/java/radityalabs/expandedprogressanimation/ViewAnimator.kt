package radityalabs.expandedprogressanimation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.support.annotation.IntDef
import android.support.annotation.IntRange
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Interpolator
import java.util.*

class ViewAnimator {
    companion object {
        private val DEFAULT_DURATION: Long = 3000
        fun animate(vararg view: View): AnimationBuilder {
            val viewAnimator = ViewAnimator()
            return viewAnimator.addAnimationBuilder(*view)
        }
    }

    private val animationList = ArrayList<AnimationBuilder>()
    private var duration = DEFAULT_DURATION
    private var startDelay: Long = 0
    private var interpolator: Interpolator? = null

    private var repeatCount = 0
    private var repeatMode = ValueAnimator.RESTART

    private var animatorSet: AnimatorSet? = null
    private var waitForThisViewHeight: View? = null

    private var startListener: AnimationListener.Start? = null
    private var stopListener: AnimationListener.Stop? = null

    private var prev: ViewAnimator? = null
    private var next: ViewAnimator? = null

    @IntDef(flag = false, value = *longArrayOf(ValueAnimator.RESTART.toLong(), ValueAnimator.REVERSE.toLong()))
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class RepeatMode

    fun thenAnimate(vararg views: View): AnimationBuilder {
        val nextViewAnimator = ViewAnimator()
        this.next = nextViewAnimator
        nextViewAnimator.prev = this
        return nextViewAnimator.addAnimationBuilder(*views)
    }

    fun addAnimationBuilder(vararg views: View): AnimationBuilder {
        val animationBuilder = AnimationBuilder(this, arrayOf(*views))
        animationList.add(animationBuilder)
        return animationBuilder
    }

    fun start(): ViewAnimator {
        if (prev != null) {
            prev!!.start()
        } else {
            animatorSet = createAnimatorSet()

            if (waitForThisViewHeight != null) {
                waitForThisViewHeight!!.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        animatorSet!!.start()
                        waitForThisViewHeight!!.viewTreeObserver.removeOnPreDrawListener(this)
                        return false
                    }
                })
            } else {
                animatorSet!!.start()
            }
        }
        return this
    }

    fun cancel() {
        if (animatorSet != null) {
            animatorSet!!.cancel()
        }
        if (next != null) {
            next!!.cancel()
            next = null
        }
    }

    fun duration(duration: Long): ViewAnimator {
        this.duration = duration
        return this
    }

    fun startDelay(startDelay: Long): ViewAnimator {
        this.startDelay = startDelay
        return this
    }

    fun repeatCount(@IntRange(from = -1) repeatCount: Int): ViewAnimator {
        this.repeatCount = repeatCount
        return this
    }

    fun repeatMode(@RepeatMode repeatMode: Int): ViewAnimator {
        this.repeatMode = repeatMode
        return this
    }

    fun onStart(startListener: AnimationListener.Start): ViewAnimator {
        this.startListener = startListener
        return this
    }

    fun onStop(stopListener: AnimationListener.Stop): ViewAnimator {
        this.stopListener = stopListener
        return this
    }

    fun interpolator(interpolator: Interpolator): ViewAnimator {
        this.interpolator = interpolator
        return this
    }

    private fun createAnimatorSet(): AnimatorSet {
        val animators = ArrayList<Animator>()
        for (animationBuilder in animationList) {
            val animatorList = animationBuilder.createAnimators()
            if (animationBuilder.singleInterpolator != null) {
                for (animator in animatorList) {
                    animator.interpolator = animationBuilder.singleInterpolator
                }
            }
            animators.addAll(animatorList)
        }

        for (i in 0 until animationList.size) {
            if (animationList[i].isWaitForHeight) {
                for (t in 0 until animationList[i].views.size) {
                    waitForThisViewHeight = animationList[i].views[t]
                    break
                }
            }
        }

        for (animator in animators) {
            if (animator is ValueAnimator) {
                animator.repeatCount = repeatCount
                animator.repeatMode = repeatMode
            }
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animators)

        animatorSet.duration = duration
        animatorSet.startDelay = startDelay
        if (interpolator != null)
            animatorSet.interpolator = interpolator

        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (startListener != null) startListener!!.onStart()
            }

            override fun onAnimationEnd(animation: Animator) {
                if (stopListener != null) stopListener!!.onStop()
                if (next != null) {
                    next!!.prev = null
                    next!!.start()
                }
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        return animatorSet
    }
}
