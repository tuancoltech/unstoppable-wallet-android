package io.horizontalsystems.bankwallet.modules.swap

import android.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.AdditionalTradeInfo
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.AdditionalTradeInfoValue
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.AmountType
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.PercentageLevel
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.SwapAdapterState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.*
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

class UniswapAdapter(
        private val uniswapKit: UniswapKit,
        evmKit: EthereumKit,
        private val settingsAdapterFactory: SwapSettingsAdapterFactory,
        fromCoin: Coin?
) : SwapModuleNew.ISwapAdapter {
    private var swapData: SwapData? = null
    private var swapDataDisposable: Disposable? = null
    private var lastBlockDisposable: Disposable? = null

    init {
        lastBlockDisposable = evmKit.lastBlockHeightFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncSwapData()
                }
    }

    override val routerAddress: Address
        get() = uniswapKit.routerAddress

    override var state: SwapAdapterState = SwapAdapterState.NotReady
        private set(value) {
            field = value
            stateObservable.onNext(value)
        }
    override val stateObservable = PublishSubject.create<SwapAdapterState>()

    override var errors: List<Throwable> = listOf()
        private set(value) {
            field = value
            errorsObservable.onNext(value)
        }
    override val errorsObservable = PublishSubject.create<List<Throwable>>()

    override var fromCoin: Coin? = fromCoin
        private set(value) {
            field = value
            fromCoinObservable.onNext(Optional.ofNullable(value))
        }
    override val fromCoinObservable = PublishSubject.create<Optional<Coin>>()

    override var fromAmount: BigDecimal? = null
        private set(value) {
            field = value
            fromAmountObservable.onNext(Optional.ofNullable(value))
        }
    override val fromAmountObservable = PublishSubject.create<Optional<BigDecimal>>()

    override var toCoin: Coin? = null
        private set(value) {
            field = value
            toCoinObservable.onNext(Optional.ofNullable(value))
        }
    override val toCoinObservable = PublishSubject.create<Optional<Coin>>()

    override var toAmount: BigDecimal? = null
        private set(value) {
            field = value
            toAmountObservable.onNext(Optional.ofNullable(value))
        }
    override val toAmountObservable = PublishSubject.create<Optional<BigDecimal>>()

    override var amountType: AmountType = AmountType.ExactFrom
        private set(value) {
            field = value
            amountTypeObservable.onNext(value)
        }
    override val amountTypeObservable = PublishSubject.create<AmountType>()

    override fun enterFromCoin(coin: Coin?) {
        if (fromCoin == coin) return

        fromCoin = coin

        if (amountType == AmountType.ExactTo) {
            fromAmount = null
        }

        if (toCoin == fromCoin) {
            toCoin = null
            toAmount = null
        }

        swapData = null
        syncSwapData()
    }

    override fun enterFromAmount(amount: BigDecimal?) {
        amountType = AmountType.ExactFrom

        if (amountsEqual(fromAmount, amount)) return

        fromAmount = amount
        toAmount = null
        syncTradeData()
    }

    override fun enterToCoin(coin: Coin?) {
        if (toCoin == coin) return

        toCoin = coin

        if (amountType == AmountType.ExactFrom) {
            toAmount = null
        }

        if (fromCoin == toCoin) {
            fromCoin = null
            fromAmount = null
        }

        swapData = null
        syncSwapData()
    }

    override fun enterToAmount(amount: BigDecimal?) {
        amountType = AmountType.ExactTo

        if (amountsEqual(toAmount, amount)) return

        toAmount = amount
        fromAmount = null
        syncTradeData()
    }

    override fun switchCoins() {
        val swapCoin = toCoin
        toCoin = fromCoin

        enterFromCoin(swapCoin)
    }

    override fun onCleared() {
        lastBlockDisposable?.dispose()
        swapDataDisposable?.dispose()
    }

    private fun syncSwapData() {
        val fromCoin = fromCoin
        val toCoin = toCoin

        if (fromCoin == null || toCoin == null) {
            state = SwapAdapterState.NotReady
            return
        }

        if (swapData == null) {
            state = SwapAdapterState.Loading
        }

        swapDataDisposable?.dispose()
        swapDataDisposable = null

        swapDataDisposable = getSwapDataAsync(fromCoin, toCoin)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    swapData = it
                    syncTradeData()
                }, { error ->
                    errors = listOf(error)
                    state = SwapAdapterState.NotReady
                })
    }

    private fun getSwapDataAsync(fromCoin: Coin, toCoin: Coin): Single<SwapData> {
        return try {
            val tokenIn = uniswapToken(fromCoin)
            val tokenOut = uniswapToken(toCoin)

            uniswapKit.swapData(tokenIn, tokenOut)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    private fun getTradeData(swapData: SwapData, amount: BigDecimal, amountType: AmountType): TradeData {
        val tradeOptions = TradeOptions() //TODO take tradeOptions from settings page

        return when (amountType) {
            AmountType.ExactFrom -> {
                uniswapKit.bestTradeExactIn(swapData, amount, tradeOptions)
            }
            AmountType.ExactTo -> {
                uniswapKit.bestTradeExactOut(swapData, amount, tradeOptions)
            }
        }
    }

    @Throws
    private fun uniswapToken(coin: Coin): Token {
        return when (val coinType = coin.type) {
            is CoinType.Erc20 -> {
                uniswapKit.token(Address(coinType.address), coin.decimal)
            }
            is CoinType.Bep20 -> {
                uniswapKit.token(Address(coinType.address), coin.decimal)
            }
            CoinType.Ethereum, CoinType.BinanceSmartChain -> {
                uniswapKit.etherToken()
            }
            else -> throw Exception("Invalid coin for swap: $coin")
        }
    }

    private fun syncTradeData() {
        Log.e("AAA", "syncTradeData")
        val swapData = swapData ?: return

        errors = listOf()

        val amount = if (amountType == AmountType.ExactFrom) fromAmount else toAmount

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            state = SwapAdapterState.NotReady
            return
        }

        try {
            val tradeData = getTradeData(swapData, amount, amountType)
            handle(tradeData)
        } catch (e: Throwable) {
            errors = listOf(e)
            state = SwapAdapterState.NotReady

        }
    }

    private fun handle(tradeData: TradeData) {
        when (tradeData.type) {
            TradeType.ExactIn -> toAmount = tradeData.amountOut
            TradeType.ExactOut -> fromAmount = tradeData.amountIn
        }

        //TODO handle additionalInfo and nullable values
        val additionalTradeInfoList = mutableListOf<AdditionalTradeInfo>()

        tradeData.trade.executionPrice.decimalValue?.let {
            val title = Translator.getString(R.string.Swap_Price)
            val value = AdditionalTradeInfoValue.Price(it, fromCoin!!, toCoin!!)
            additionalTradeInfoList.add(AdditionalTradeInfo(title, value))
        }

        tradeData.priceImpact?.let {
            val title = Translator.getString(R.string.Swap_PriceImpact)
            val level = when {
                it >= BigDecimal.ZERO && it < warningPriceImpact -> PercentageLevel.Normal
                it >= warningPriceImpact && it < forbiddenPriceImpact -> PercentageLevel.Warning
                else -> {
                    errors = listOf(SwapModuleNew.SwapProviderError.ForbiddenPriceImpactLevel)
                    PercentageLevel.Forbidden
                }
            }
            val value = AdditionalTradeInfoValue.Percentage(it, level)
            additionalTradeInfoList.add(AdditionalTradeInfo(title, value))
        }

        when (tradeData.type) {
            TradeType.ExactIn -> {
                if (tradeData.amountOutMin != null) {
                    val title = Translator.getString(R.string.Swap_MinimumGot)
                    val value = AdditionalTradeInfoValue.CoinAmount(tradeData.amountOutMin!!, toCoin!!)
                    additionalTradeInfoList.add(AdditionalTradeInfo(title, value))
                }
            }
            TradeType.ExactOut -> {
                if (tradeData.amountInMax != null) {
                    val title = Translator.getString(R.string.Swap_MaximumPaid)
                    val value = AdditionalTradeInfoValue.CoinAmount(tradeData.amountInMax!!, fromCoin!!)
                    additionalTradeInfoList.add(AdditionalTradeInfo(title, value))
                }
            }
        }

        val trade = SwapModuleNew.Trade(fromCoin!!, fromAmount!!, toCoin!!, toAmount!!, additionalTradeInfoList)
        val transactionData = uniswapKit.transactionData(tradeData)

        Log.e("AAA", "uniswapprovider handle tradeData")
        state = SwapAdapterState.Ready(trade, transactionData)
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    companion object {
        private val warningPriceImpact = BigDecimal(1)
        private val forbiddenPriceImpact = BigDecimal(5)
    }

}
