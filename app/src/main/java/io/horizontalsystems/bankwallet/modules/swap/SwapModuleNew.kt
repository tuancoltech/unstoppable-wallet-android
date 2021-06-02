package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

object SwapModuleNew {

    interface ISwapAdapter {
        val state: SwapAdapterState
        val stateObservable: Observable<SwapAdapterState>

        val errors: List<Throwable>
        val errorsObservable: Observable<List<Throwable>>

        val fromCoin: Coin?
        val fromCoinObservable: Observable<Optional<Coin>>
        val fromAmount: BigDecimal?
        val fromAmountObservable: Observable<Optional<BigDecimal>>

        val toCoin: Coin?
        val toCoinObservable: Observable<Optional<Coin>>
        val toAmount: BigDecimal?
        val toAmountObservable: Observable<Optional<BigDecimal>>

        val amountType: AmountType
        val amountTypeObservable: Observable<AmountType>

        fun enterFromCoin(coin: Coin?)
        fun enterFromAmount(amount: BigDecimal?)

        fun enterToCoin(coin: Coin?)
        fun enterToAmount(amount: BigDecimal?)

        fun switchCoins()

        fun onCleared()
    }

    sealed class SwapAdapterState {
        object Loading : SwapAdapterState()
        class Ready(val trade: Trade, val transactionData: TransactionData) : SwapAdapterState()
        object NotReady : SwapAdapterState()
    }

    enum class AmountType {
        ExactFrom, ExactTo
    }

    data class Trade(
            val fromCoin: Coin,
            val fromAmount: BigDecimal,
            val toCoin: Coin,
            val toAmount: BigDecimal,
            val additionalInfo: List<AdditionalTradeInfo>
    )

    data class AdditionalTradeInfo(
            val title: String,
            val value: AdditionalTradeInfoValue
    )

    sealed class AdditionalTradeInfoValue {
        class Price(val price: BigDecimal, val baseCoin: Coin, val quoteCoin: Coin) : AdditionalTradeInfoValue()
        class Percentage(val percentage: BigDecimal, val level: PercentageLevel = PercentageLevel.Normal) : AdditionalTradeInfoValue()
        class CoinAmount(val amount: BigDecimal, val coin: Coin) : AdditionalTradeInfoValue()
        class SwapRoute(val route: List<Coin>) : AdditionalTradeInfoValue()
    }

    enum class PercentageLevel {
        Normal, Warning, Forbidden
    }

    sealed class SwapProviderError(val text: String) : Throwable(text) {
        object ForbiddenPriceImpactLevel : SwapProviderError(Translator.getString(R.string.Swap_ErrorHighPriceImpact))
    }

    data class SwapSettings(val title: String?, val description: String?, val hint: String?) {

    }

}
