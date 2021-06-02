package io.horizontalsystems.bankwallet.modules.swap

import androidx.annotation.ColorRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.AdditionalTradeInfoValue
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.PercentageLevel
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.SwapAdapterState
import io.horizontalsystems.bankwallet.modules.swap.SwapServiceNew.SwapError
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.SwapTradeOptions
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class SwapViewModelNew(
        val service: SwapServiceNew,
        val swapAdapter: SwapModuleNew.ISwapAdapter,
        private val pendingAllowanceService: SwapPendingAllowanceService,
        private val formatter: SwapViewItemHelper
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val isLoadingLiveData = MutableLiveData(false)
    private val swapErrorLiveData = MutableLiveData<String?>(null)
    private val additionalTradeInfoLiveData = MutableLiveData<List<AdditionalTradeInfoViewItem>?>(null)
    private val tradeOptionsViewItemLiveData = MutableLiveData<TradeOptionsViewItem?>(null)
    private val proceedActionLiveData = MutableLiveData<ActionState>(ActionState.Hidden)
    private val approveActionLiveData = MutableLiveData<ActionState>(ActionState.Hidden)
    private val openApproveLiveEvent = SingleLiveEvent<SwapAllowanceService.ApproveData>()
    private val advancedSettingsVisibleLiveData = MutableLiveData(false)
    private val openConfirmationLiveEvent = SingleLiveEvent<SendEvmData>()

    init {
        subscribeToServices()

        sync(service.state)
        sync(service.errors)
        sync(swapAdapter.state)
    }

    //region outputs
    fun isLoadingLiveData(): LiveData<Boolean> = isLoadingLiveData
    fun swapErrorLiveData(): LiveData<String?> = swapErrorLiveData
    fun additionalTradeInfoLiveData(): LiveData<List<AdditionalTradeInfoViewItem>?> = additionalTradeInfoLiveData
    fun tradeOptionsViewItemLiveData(): LiveData<TradeOptionsViewItem?> = tradeOptionsViewItemLiveData
    fun proceedActionLiveData(): LiveData<ActionState> = proceedActionLiveData
    fun approveActionLiveData(): LiveData<ActionState> = approveActionLiveData
    fun openApproveLiveEvent(): LiveData<SwapAllowanceService.ApproveData> = openApproveLiveEvent
    fun advancedSettingsVisibleLiveData(): LiveData<Boolean> = advancedSettingsVisibleLiveData
    fun openConfirmationLiveEvent(): LiveData<SendEvmData> = openConfirmationLiveEvent

    fun onTapSwitch() {
        swapAdapter.switchCoins()
    }

    fun onTapApprove() {
        service.approveData?.let { approveData ->
            openApproveLiveEvent.postValue(approveData)
        }
    }

    fun onTapProceed() {
        val serviceState = service.state
        if (serviceState is SwapServiceNew.State.Ready) {
            val trade = (swapAdapter.state as? SwapAdapterState.Ready)?.trade
            val swapInfo = SendEvmData.SwapInfo(
                    estimatedIn = swapAdapter.fromAmount ?: BigDecimal.ZERO,
                    estimatedOut = swapAdapter.toAmount ?: BigDecimal.ZERO,
                    slippage = null,//formatter.slippage(swapProvider.tradeOptions.allowedSlippage),
                    deadline = null, //formatter.deadline(swapProvider.tradeOptions.ttl),
                    recipientDomain = null, //swapProvider.tradeOptions.recipient?.domain,
                    price = null, //formatter.price(trade?.tradeData?.executionPrice, swapProvider.coinFrom, swapProvider.coinTo),
                    priceImpact = null, //trade?.let { formatter.priceImpactViewItem(it)?.value }
            )
            openConfirmationLiveEvent.postValue(SendEvmData(serviceState.transactionData, SendEvmData.AdditionalInfo.Swap(swapInfo)))
        }
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }
    //endregion

    override fun onCleared() {
        service.onCleared()
        disposables.clear()
    }

    private fun subscribeToServices() {
        service.stateObservable
                .subscribeIO { sync(it) }
                .let { disposables.add(it) }

        service.errorsObservable
                .subscribeIO { sync(it) }
                .let { disposables.add(it) }

        swapAdapter.stateObservable
                .subscribeIO { sync(it) }
                .let { disposables.add(it) }

//        swapProvider.tradeOptionsObservable
//                .subscribeOn(Schedulers.io())
//                .subscribe { sync(it) }
//                .let { disposables.add(it) }
//
        pendingAllowanceService.isPendingObservable
                .subscribeIO {
                    syncApproveAction()
                    syncProceedAction()
                }
                .let { disposables.add(it) }
    }

    private fun sync(serviceState: SwapServiceNew.State) {
        isLoadingLiveData.postValue(serviceState == SwapServiceNew.State.Loading)
        syncProceedAction()
    }

    private fun convert(error: Throwable): String = when (val convertedError = error.convertedError) {
        is JsonRpc.ResponseError.RpcError -> {
            convertedError.error.message
        }
        is TradeError.TradeNotFound -> {
            Translator.getString(R.string.Swap_ErrorNoLiquidity)
        }
        else -> {
            convertedError.message ?: convertedError.javaClass.simpleName
        }
    }

    private fun sync(errors: List<Throwable>) {
        val filtered = errors.filter { it !is EvmTransactionService.GasDataError && it !is SwapError && it !is SwapModuleNew.SwapProviderError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

        syncProceedAction()
        syncApproveAction()
    }

    private fun sync(adapterState: SwapAdapterState) {
        when (adapterState) {
            is SwapAdapterState.Ready -> {
                additionalTradeInfoLiveData.postValue(getAdditionalInfoViewItems(adapterState.trade))
                advancedSettingsVisibleLiveData.postValue(true)
            }
            else -> {
                additionalTradeInfoLiveData.postValue(null)
                advancedSettingsVisibleLiveData.postValue(false)
            }
        }
        syncProceedAction()
        syncApproveAction()
    }

    private fun sync(swapTradeOptions: SwapTradeOptions) {
        tradeOptionsViewItemLiveData.postValue(tradeOptionsViewItem(swapTradeOptions))
    }

    private fun syncProceedAction() {
        val proceedAction = when {
            service.state is SwapServiceNew.State.Ready -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
            }
            swapAdapter.state is SwapAdapterState.Ready -> {
                val swapProviderError = service.errors.firstOrNull { it is SwapModuleNew.SwapProviderError } as? SwapModuleNew.SwapProviderError

                when {
                    service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                    }
                    swapProviderError != null -> {
                        ActionState.Disabled(swapProviderError.text)
                    }
                    pendingAllowanceService.isPending -> {
                        ActionState.Hidden
                    }
                    else -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                }
            }
            else -> {
                ActionState.Hidden
            }
        }
        proceedActionLiveData.postValue(proceedAction)
    }

    private fun syncApproveAction() {
        val approveAction = when {
            swapAdapter.state !is SwapAdapterState.Ready || service.errors.any { it == SwapError.InsufficientBalanceFrom || it is SwapModuleNew.SwapProviderError } -> {
                ActionState.Hidden
            }
            pendingAllowanceService.isPending -> {
                ActionState.Disabled(Translator.getString(R.string.Swap_Approving))
            }
            service.errors.any { it == SwapError.InsufficientAllowance } -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Approve))
            }
            else -> {
                ActionState.Hidden
            }
        }
        approveActionLiveData.postValue(approveAction)
    }

    private fun getAdditionalInfoViewItems(trade: SwapModuleNew.Trade): List<AdditionalTradeInfoViewItem> {
        val additionalTradeInfoList = mutableListOf<AdditionalTradeInfoViewItem>()
        trade.additionalInfo
                .filter { it.value !is AdditionalTradeInfoValue.Percentage || it.value.level == PercentageLevel.Forbidden }
                .forEach { additionalInfo ->
                    val viewItem = when (additionalInfo.value) {
                        is AdditionalTradeInfoValue.Price -> {
                            val price = additionalInfo.value.let {
                                formatter.price(it.price, it.baseCoin, it.quoteCoin)
                            }
                            price?.let {
                                AdditionalTradeInfoViewItem.TextWithTitle(it, additionalInfo.title)
                            }
                        }
                        is AdditionalTradeInfoValue.Percentage -> {
                            val percentage = Translator.getString(R.string.Swap_Percent, additionalInfo.value.percentage)
                            val color = getPercentageColor(additionalInfo.value.level)
                            AdditionalTradeInfoViewItem.TextWithTitleAndColor(percentage, additionalInfo.title, color)
                        }
                        is AdditionalTradeInfoValue.CoinAmount -> {
                            val coinAmount = formatter.coinAmount(additionalInfo.value.amount, additionalInfo.value.coin)
                            AdditionalTradeInfoViewItem.TextWithTitle(coinAmount, additionalInfo.title)
                        }
                        is AdditionalTradeInfoValue.SwapRoute -> {
                            //skip for now
                            null
                        }
                    }
                    viewItem?.let { additionalTradeInfoList.add(it) }
                }

        return additionalTradeInfoList
    }

    private fun getPercentageColor(level: PercentageLevel): Int = when (level) {
        PercentageLevel.Normal -> R.color.remus
        PercentageLevel.Warning -> R.color.jacob
        PercentageLevel.Forbidden -> R.color.lucian
    }

    private fun tradeOptionsViewItem(tradeOptions: SwapTradeOptions): TradeOptionsViewItem {
        val defaultTradeOptions = TradeOptions()
        val slippage = if (tradeOptions.allowedSlippage.compareTo(defaultTradeOptions.allowedSlippagePercent) == 0) null else tradeOptions.allowedSlippage.stripTrailingZeros().toPlainString()
        val deadline = if (tradeOptions.ttl == defaultTradeOptions.ttl) null else tradeOptions.ttl.toString()
        val recipientAddress = tradeOptions.recipient?.hex

        return TradeOptionsViewItem(slippage, deadline, recipientAddress)
    }

    //region models

    sealed class AdditionalTradeInfoViewItem {
        class TextWithTitle(val text: String, val title: String) : AdditionalTradeInfoViewItem()
        class TextWithTitleAndColor(val text: String, val title: String, @ColorRes val color: Int) : AdditionalTradeInfoViewItem()
    }

    data class TradeOptionsViewItem(
            val slippage: String?,
            val deadline: String?,
            val recipient: String?
    )

    sealed class ActionState {
        object Hidden : ActionState()
        class Enabled(val title: String) : ActionState()
        class Disabled(val title: String) : ActionState()
    }
    //endregion
}
