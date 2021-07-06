package io.horizontalsystems.bankwallet.modules.send.submodules

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class SendSubmoduleFragment(@LayoutRes layoutResId: Int = 0) : Fragment(layoutResId) {
    abstract fun init()
}
