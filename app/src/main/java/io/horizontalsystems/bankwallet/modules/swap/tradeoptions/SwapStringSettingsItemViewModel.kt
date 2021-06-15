package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.LiveData

class SwapStringSettingsItemViewModel(
        override val header: String?,
        override val footer: String?,
        override val placeholder: String?,
        override val initialValue: String?,
        override val shortcuts: List<InputFieldButtonItem>
) : SwapSettingsModule.ISwapStringSettingsItemViewModel {

    override val setTextLiveData: LiveData<String?>
        get() = TODO("Not yet implemented")


    override val cautionLiveData: LiveData<Caution?>
        get() = TODO("Not yet implemented")


    override fun onChange(text: String?) {
        TODO("not implemented")
    }

    override fun isValid(text: String): Boolean {
        TODO("not implemented")
    }

}
