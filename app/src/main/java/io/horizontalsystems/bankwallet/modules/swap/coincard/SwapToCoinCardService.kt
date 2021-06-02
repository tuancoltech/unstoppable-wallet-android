package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModuleNew
import io.horizontalsystems.bankwallet.modules.swap.SwapServiceNew
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class SwapToCoinCardService(
        private val service: SwapServiceNew,
        private val swapAdapter: SwapModuleNew.ISwapAdapter,
        private val coinProvider: SwapCoinProvider
) : ISwapCoinCardService {
    private val amountType: SwapModuleNew.AmountType = SwapModuleNew.AmountType.ExactTo

    override val isEstimated: Boolean
        get() = swapAdapter.amountType != amountType

    override val amount: BigDecimal?
        get() = swapAdapter.toAmount

    override val coin: Coin?
        get() = swapAdapter.toCoin

    override val balance: BigDecimal?
        get() = service.balanceTo

    override val tokensForSelection: List<SwapModule.CoinBalanceItem>
        get() = coinProvider.coins(enabledCoins = false)

    override val isEstimatedObservable: Observable<Boolean>
        get() = swapAdapter.amountTypeObservable.map { it != amountType }

    override val amountObservable: Observable<Optional<BigDecimal>>
        get() = swapAdapter.toAmountObservable

    override val coinObservable: Observable<Optional<Coin>>
        get() = swapAdapter.toCoinObservable

    override val balanceObservable: Observable<Optional<BigDecimal>>
        get() = service.balanceToObservable

    override val errorObservable: Observable<Optional<Throwable>>
        get() = Observable.just(Optional.empty())

    override fun onChangeAmount(amount: BigDecimal?) {
        swapAdapter.enterToAmount(amount)
    }

    override fun onSelectCoin(coin: Coin) {
        swapAdapter.enterToCoin(coin)
    }

}
