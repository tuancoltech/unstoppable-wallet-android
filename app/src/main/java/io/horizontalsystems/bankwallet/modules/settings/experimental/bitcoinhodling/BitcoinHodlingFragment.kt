package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_bitcoin_hodling.*

class BitcoinHodlingFragment : BaseFragment(R.layout.fragment_bitcoin_hodling) {

    private val presenter by viewModels<BitcoinHodlingPresenter> { BitcoinHodlingModule.Factory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val hodlingView = presenter.view as BitcoinHodlingView

        hodlingView.lockTimeEnabledLiveEvent.observe(viewLifecycleOwner, Observer { enabled ->
            switchLockTime.setChecked(enabled)
        })

        switchLockTime.setOnClickListener {
            switchLockTime.switchToggle()
        }

        switchLockTime.setOnCheckedChangeListener {
            presenter.onSwitchLockTime(it)
        }

        presenter.onLoad()
    }
}
