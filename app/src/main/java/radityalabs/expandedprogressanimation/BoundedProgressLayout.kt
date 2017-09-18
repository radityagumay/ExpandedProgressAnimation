package radityalabs.expandedprogressanimation

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout

interface Action {
    fun onLoading()

    fun onCompleted()

    fun onProgress()

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

    private lateinit var container: RelativeLayout

    override fun onCompleted() {

    }

    override fun onLoading() {

    }

    override fun onProgress() {
    }

    override fun setAddress(address: String) {
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        container = findViewById(R.id.container)


        initView()
    }

    private fun initView() {
        val iconBackground = GradientDrawable()
        iconBackground.cornerRadius = 100f
        iconBackground.setColor(resources.getColor(R.color.colorAccent))
        container.background = iconBackground
    }
}