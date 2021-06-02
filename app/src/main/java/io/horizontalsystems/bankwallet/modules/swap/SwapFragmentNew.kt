package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapViewModelNew.ActionState
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.SwapConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.info.SwapInfoFragment.Companion.dexKey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_swap.advancedSettings
import kotlinx.android.synthetic.main.fragment_swap.advancedSettingsViews
import kotlinx.android.synthetic.main.fragment_swap.allowanceView
import kotlinx.android.synthetic.main.fragment_swap.approveButton
import kotlinx.android.synthetic.main.fragment_swap.commonError
import kotlinx.android.synthetic.main.fragment_swap.fromCoinCard
import kotlinx.android.synthetic.main.fragment_swap.poweredBy
import kotlinx.android.synthetic.main.fragment_swap.poweredByLine
import kotlinx.android.synthetic.main.fragment_swap.proceedButton
import kotlinx.android.synthetic.main.fragment_swap.progressBar
import kotlinx.android.synthetic.main.fragment_swap.switchButton
import kotlinx.android.synthetic.main.fragment_swap.toCoinCard
import kotlinx.android.synthetic.main.fragment_swap.toolbar
import kotlinx.android.synthetic.main.fragment_swap_new.*

class SwapFragmentNew : BaseFragment() {

    private val vmFactory by lazy { SwapModule.Factory(this, requireArguments().getParcelable(fromCoinKey)!!) }
    private val viewModel by navGraphViewModels<SwapViewModelNew>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModel by navGraphViewModels<SwapAllowanceViewModel>(R.id.swapFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                R.id.menuInfo -> {
                    findNavController().navigate(R.id.swapFragment_to_swapInfoFragment, bundleOf(dexKey to viewModel.service.dex), navOptions())
                    true
                }
                else -> false
            }
        }

        val fromCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeFrom, SwapCoinCardViewModel::class.java)
        val fromCoinCardTitle = getString(R.string.Swap_FromAmountTitle)
        fromCoinCard.initialize(fromCoinCardTitle, fromCoinCardViewModel, this, viewLifecycleOwner)

        val toCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeTo, SwapCoinCardViewModel::class.java)
        val toCoinCardTile = getString(R.string.Swap_ToAmountTitle)
        toCoinCard.initialize(toCoinCardTile, toCoinCardViewModel, this, viewLifecycleOwner)

        allowanceView.initialize(allowanceViewModel, viewLifecycleOwner)

        observeViewModel()

        getNavigationResult(SwapApproveModule.requestKey)?.let {
            if (it.getBoolean(SwapApproveModule.resultKey)) {
                viewModel.didApprove()
            }
        }

        switchButton.setOnClickListener {
            viewModel.onTapSwitch()
        }

        advancedSettings.setOnSingleClickListener {
            findNavController().navigate(R.id.swapFragment_to_swapTradeOptionsFragment)
        }

        approveButton.setOnSingleClickListener {
            viewModel.onTapApprove()
        }

        proceedButton.setOnSingleClickListener {
            viewModel.onTapProceed()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoadingLiveData().observe(viewLifecycleOwner, { isLoading ->
            progressBar.isVisible = isLoading
        })

        viewModel.swapErrorLiveData().observe(viewLifecycleOwner, { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        viewModel.additionalTradeInfoLiveData().observe(viewLifecycleOwner, {
            handleAdditionalTradeInfo(it)
        })

        viewModel.proceedActionLiveData().observe(viewLifecycleOwner, { action ->
            handleButtonAction(proceedButton, action)
        })

        viewModel.approveActionLiveData().observe(viewLifecycleOwner, { approveActionState ->
            handleButtonAction(approveButton, approveActionState)
        })

        viewModel.openApproveLiveEvent().observe(viewLifecycleOwner, { approveData ->
            SwapApproveModule.start(this, R.id.swapFragment_to_swapApproveFragment, navOptions(), approveData)
        })

        viewModel.advancedSettingsVisibleLiveData().observe(viewLifecycleOwner, { visible ->
            advancedSettingsViews.isVisible = visible
        })

        viewModel.openConfirmationLiveEvent().observe(viewLifecycleOwner, { sendEvmData ->
            SwapConfirmationModule.start(this, R.id.swapFragment_to_swapConfirmationFragment, navOptions(), sendEvmData)
        })

        val dexName = when (viewModel.service.dex) {
            SwapModule.Dex.Uniswap -> "Uniswap"
            SwapModule.Dex.PancakeSwap -> "PancakeSwap"
        }
        poweredBy.text = "Powered by $dexName"
    }

    private fun handleButtonAction(button: Button, action: ActionState?) {
        when (action) {
            ActionState.Hidden -> {
                button.isVisible = false
            }
            is ActionState.Enabled -> {
                button.isVisible = true
                button.isEnabled = true
                button.text = action.title
            }
            is ActionState.Disabled -> {
                button.isVisible = true
                button.isEnabled = false
                button.text = action.title
            }
        }
    }

    private fun handleAdditionalTradeInfo(additionalTradeInfo: List<SwapViewModelNew.AdditionalTradeInfoViewItem>?) {
        additionalInfoItems.removeAllViewsInLayout()

        additionalTradeInfo?.forEach { info ->
            val view = SwapAdditionalInfoView(requireContext())

            when (info) {
                is SwapViewModelNew.AdditionalTradeInfoViewItem.TextWithTitle -> {
                    view.set(info.title, info.text)
                }
                is SwapViewModelNew.AdditionalTradeInfoViewItem.TextWithTitleAndColor -> {
                    view.set(info.title, info.text)
                    view.setTextColor(info.color)
                }
            }
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.setMargins(0, LayoutHelper.dp(16f, context), 0, 0)

            additionalInfoItems.addView(view, layoutParams)
        }

        poweredBy.isVisible = additionalTradeInfo == null
        poweredByLine.isVisible = additionalTradeInfo == null
    }

    companion object {
        const val fromCoinKey = "fromCoinKey"
    }

}
