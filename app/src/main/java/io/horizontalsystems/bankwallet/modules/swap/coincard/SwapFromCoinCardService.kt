package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapAdapterManager
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew
import io.horizontalsystems.bankwallet.modules.swap.SwapServiceNew
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class SwapFromCoinCardService(
        private val service: SwapServiceNew,
        private val swapAdapterManager: SwapAdapterManager,
        private val coinProvider: SwapCoinProvider
) : ISwapCoinCardService {
    private val amountType: SwapModuleNew.AmountType = SwapModuleNew.AmountType.ExactFrom

    private val swapAdapter: SwapModuleNew.ISwapAdapter
        get() = swapAdapterManager.swapAdapter

    override val isEstimated: Boolean
        get() = swapAdapter.amountType != amountType

    override val amount: BigDecimal?
        get() = swapAdapter.fromAmount

    override val coin: Coin?
        get() = swapAdapter.fromCoin

    override val balance: BigDecimal?
        get() = service.balanceFrom

    override val tokensForSelection: List<SwapModule.CoinBalanceItem>
        get() = coinProvider.coins(enabledCoins = false)

    override val isEstimatedObservable: Observable<Boolean>
        get() = swapAdapter.amountTypeObservable.map { it != amountType }

    override val amountObservable: Observable<Optional<BigDecimal>>
        get() = swapAdapter.fromAmountObservable

    override val coinObservable: Observable<Optional<Coin>>
        get() = swapAdapter.fromCoinObservable

    override val balanceObservable: Observable<Optional<BigDecimal>>
        get() = service.balanceFromObservable

    override val errorObservable: Observable<Optional<Throwable>>
        get() = service.errorsObservable.map { errors -> errors.firstOrNull { it is SwapServiceNew.SwapError.InsufficientBalanceFrom }.let { Optional.ofNullable(it) } }

    override fun onChangeAmount(amount: BigDecimal?) {
        swapAdapter.enterFromAmount(amount)
    }

    override fun onSelectCoin(coin: Coin) {
        swapAdapter.enterFromCoin(coin)
    }

}
