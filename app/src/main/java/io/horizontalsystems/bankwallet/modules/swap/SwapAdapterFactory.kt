package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew.Dex
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.uniswapkit.UniswapKit

class SwapAdapterFactory(
        private val swapSettingsAdapterFactory: SwapSettingsAdapterFactory
) {
     fun adapter(dex: Dex, fromCoin: Coin?): SwapModuleNew.ISwapAdapter? {
        val evmKit = dex.evmKit ?: return null

        when (dex.provider) {
            Dex.Provider.Uniswap, Dex.Provider.Pancake -> {
                return UniswapAdapter(
                        UniswapKit.getInstance(evmKit),
                        evmKit,
                        swapSettingsAdapterFactory,
                        fromCoin
                )
            }
            Dex.Provider.OneInch -> TODO()
        }
    }
}