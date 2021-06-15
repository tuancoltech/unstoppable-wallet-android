package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class SwapAdapterManager(
        private val localStorage: ILocalStorage,
        private val swapAdapterFactory: SwapAdapterFactory,
        private val initialFromCoin: Coin? = null
) {

    private val providerUpdatedSubject = PublishSubject.create<Unit>()
    val providerUpdatedObservable: Flowable<Unit> = providerUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    var swapAdapter: SwapModuleNew.ISwapAdapter
        private set
    var dex: SwapModuleNew.Dex
        private set

    init {
        val blockchain = when (initialFromCoin?.type) {
            CoinType.Ethereum, is CoinType.Erc20 -> SwapModuleNew.Dex.Blockchain.Ethereum
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> SwapModuleNew.Dex.Blockchain.BinanceSmartChain
            null -> SwapModuleNew.Dex.Blockchain.Ethereum
            else -> throw IllegalStateException("Swap not supported for coin: ${initialFromCoin.type.ID}")
        }

        val currentProvider = localStorage.getSwapProvider(blockchain)
                ?: blockchain.supportedProviders.first()

        dex = SwapModuleNew.Dex(blockchain, currentProvider)

        swapAdapter = swapAdapterFactory.adapter(dex, initialFromCoin)
                ?: throw IllegalStateException("Could not create swap adapter for blockchain: ${dex.blockchain.id}, provider: ${dex.provider.id}, coin: ${initialFromCoin?.id}")
    }

    private fun updateProvider(provider: SwapModuleNew.Dex.Provider) {
        dex.provider = provider

        localStorage.setSwapProvider(dex.blockchain, provider)

        // TODO need to configure adapters
        swapAdapter = swapAdapterFactory.adapter(dex, null)
                ?: throw IllegalStateException("Could not create swap adapter for blockchain: ${dex.blockchain.id}, provider: ${dex.provider.id}")

        providerUpdatedSubject.onNext(Unit)
    }

    val routerAddress: Address
        get() = swapAdapter.routerAddress

    fun setProvider(provider: SwapModuleNew.Dex.Provider) {
        if (provider != dex.provider) {
            updateProvider(provider)
        }
    }

}
