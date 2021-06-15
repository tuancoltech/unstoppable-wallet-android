package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.LiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.Observable

object SwapSettingsModule {

    interface ISwapSettingsItemViewModel {
        val header: String?
        val footer: String?
    }

    interface ISwapBooleanSettingsItemViewModel {
        val title: String?

        fun onChange(value: Boolean)
    }

    interface ISwapStringSettingsItemViewModel {
        val header: String?
        val footer: String?
        val placeholder: String?
        val initialValue: String?

        val shortcuts: List<InputFieldButtonItem>

        fun onChange(text: String?)
        fun isValid(text: String): Boolean

        val setTextLiveData: LiveData<String?>
        val cautionLiveData: LiveData<Caution?>
    }

    interface ISwapBigDecimalSettingsItemViewModel : ISwapStringSettingsItemViewModel

    interface ISwapAddressSettingsViewModel : ISwapStringSettingsItemViewModel {
        val isLoadingLiveData: LiveData<Boolean> get() = SingleLiveEvent<Boolean>()

        fun onFetch(text: String?)
        fun onChangeFocus(hasFocus: Boolean)
    }

    interface ISwapSettingsAdapter {
        val settingsItems: List<ISwapSettingsItemViewModel>
        val settingsItemsObservable: Observable<List<ISwapSettingsItemViewModel>>

        val state: SwapSettingsState
        val stateObservable: Observable<SwapSettingsState>
    }

    sealed class SwapSettingsState {
        class Valid(val tradeOptions: SwapTradeOptions) : SwapSettingsState()
        class Invalid(val error: String) : SwapSettingsState()
    }

}
