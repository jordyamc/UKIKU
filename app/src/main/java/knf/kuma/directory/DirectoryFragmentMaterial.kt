package knf.kuma.directory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.noCrash
import knf.kuma.commons.safeDismiss
import knf.kuma.directory.viewholders.DirMainFragmentMaterialHolder
import kotlinx.android.synthetic.main.fragment_directory_material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DirectoryFragmentMaterial : BottomFragment() {
    private var fragmentHolder: DirMainFragmentMaterialHolder? = null
    private var snackbar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_directory_material, container, false)
        fragmentHolder = DirMainFragmentMaterialHolder(view, childFragmentManager)
        EAHelper.enter1("D")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PrefsUtil.isDirectoryFinished)
            lifecycleScope.launch(Dispatchers.IO) {
                delay(1000)
                noCrash {
                    adContainer.implBanner(AdsType.DIRECTORY_BANNER, true)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackbar?.safeDismiss()
    }

    fun onChangeOrder() {
        fragmentHolder?.onChangeOrder()
    }

    override fun onReselect() {
        EAHelper.enter1("D")
        fragmentHolder?.onReselect()
    }

    companion object {

        fun get(): DirectoryFragmentMaterial {
            return DirectoryFragmentMaterial()
        }
    }
}
