package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import kotlinx.android.synthetic.main.fragment_fee_info.*

class SendFeeInfoFragment : BaseFragment(R.layout.fragment_fee_info) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    parentFragmentManager.popBackStack()
                    true
                }
                else -> false
            }
        }
    }
}
