package knf.kuma.explorer

import androidx.fragment.app.Fragment

abstract class FragmentBase : Fragment() {

    abstract fun onBackPressed(): Boolean
}
