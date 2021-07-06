package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_privacy_settings_info.*

class PrivacySettingsInfoFragment : BaseFragment(R.layout.fragment_privacy_settings_info) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.closeButton -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }
    }
}
