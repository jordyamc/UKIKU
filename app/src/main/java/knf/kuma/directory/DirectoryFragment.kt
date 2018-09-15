package knf.kuma.directory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.directory.viewholders.DirMainFragmentHolder

class DirectoryFragment : BottomFragment() {
    private var fragmentHolder: DirMainFragmentHolder? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ViewModelProviders.of(this).get(DirectoryViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_directory, container, false)
        fragmentHolder = DirMainFragmentHolder(view, childFragmentManager)
        EAHelper.enter1("D")
        return view
    }

    fun onChangeOrder() {
        if (fragmentHolder != null)
            fragmentHolder!!.onChangeOrder()
    }

    override fun onReselect() {
        EAHelper.enter1("D")
        if (fragmentHolder != null)
            fragmentHolder!!.onReselect()
    }

    companion object {

        fun get(): DirectoryFragment {
            return DirectoryFragment()
        }
    }
}
