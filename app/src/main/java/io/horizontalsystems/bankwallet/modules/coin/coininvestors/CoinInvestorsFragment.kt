package io.horizontalsystems.bankwallet.modules.coin.coininvestors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_recyclerview.*

class CoinInvestorsFragment : BaseFragment(R.layout.fragment_recyclerview), CoinInvestorCategoryAdapter.Listener {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.CoinPage_FundsInvested)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        coinViewModel.coinInvestors.observe(viewLifecycleOwner, {
            val investorsAdapter = CoinInvestorCategoryAdapter(it, this)
            recyclerView.adapter = investorsAdapter
        })
    }

    override fun onItemClick(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
