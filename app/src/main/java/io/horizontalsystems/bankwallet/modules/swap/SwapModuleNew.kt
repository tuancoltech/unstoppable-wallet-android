package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

object SwapModuleNew {

    interface ISwapAdapter {
        val routerAddress: Address

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

    class Dex(blockchain: Blockchain, provider: Provider) {
        var blockchain: Blockchain = blockchain
            set(blockchain) {
                field = blockchain
                if (!blockchain.supportedProviders.contains(provider)) {
                    provider = blockchain.supportedProviders.first()
                }
            }

        var provider: Provider = provider
            set(value) {
                field = value
                if (!provider.supportedBlockchains.contains(blockchain)) {
                    blockchain = provider.supportedBlockchains.first()
                }
            }

        val evmKit: EthereumKit?
            get() = when (blockchain) {
                Blockchain.Ethereum -> App.ethereumKitManager.evmKit
                Blockchain.BinanceSmartChain -> App.binanceSmartChainKitManager.evmKit
            }

        val coin: Coin?
            get() = when (blockchain) {
                Blockchain.Ethereum -> App.coinKit.getCoin(CoinType.Ethereum)
                Blockchain.BinanceSmartChain -> App.coinKit.getCoin(CoinType.BinanceSmartChain)
            }

        enum class Blockchain(val id: String) {
            Ethereum("ethereum"), BinanceSmartChain("binanceSmartChain");

            val supportedProviders: List<Provider>
                get() = when (this) {
                    Ethereum -> listOf(Provider.Uniswap, Provider.OneInch)
                    BinanceSmartChain -> listOf(Provider.Pancake, Provider.OneInch)
                }
        }

        enum class Provider(val id: String) {
            Uniswap("uniswap"), OneInch("1inch"), Pancake("pancake");

            val supportedBlockchains: List<Blockchain>
                get() = when (this) {
                    Uniswap -> listOf(Blockchain.Ethereum)
                    Pancake -> listOf(Blockchain.BinanceSmartChain)
                    OneInch -> listOf(Blockchain.Ethereum, Blockchain.BinanceSmartChain)
                }

            companion object {
                private val map = values().associateBy(Provider::id)

                fun fromId(id: String?): Provider? = map[id]
            }
        }
    }

}
