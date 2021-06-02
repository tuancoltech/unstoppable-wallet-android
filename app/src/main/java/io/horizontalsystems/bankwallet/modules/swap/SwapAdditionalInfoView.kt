package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_swap_additional_info_item.view.*

class SwapAdditionalInfoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        inflate(context, R.layout.view_swap_additional_info_item, this)
    }

    fun set(title: String, text: String) {
        titleTextView.text = title
        valueTextView.text = text
    }

    fun setTextColor(@ColorRes color: Int) {
        valueTextView.setTextColor(context.getColor(color))
    }

}
