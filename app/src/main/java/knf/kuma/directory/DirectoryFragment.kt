package knf.kuma.directory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.BannerContainerView
import knf.kuma.database.CacheDB
import knf.kuma.directory.viewholders.DirMainFragmentHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.support.v4.find

class DirectoryFragment : BottomFragment() {
    private var fragmentHolder: DirMainFragmentHolder? = null
    private var snackbar: Snackbar? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!PrefsUtil.isDirectoryFinished) {
            snackbar = activity?.findViewById<View>(R.id.root)?.showSnackbar("Creando directorio...", Snackbar.LENGTH_INDEFINITE)
            CacheDB.INSTANCE.animeDAO().countLive.observe(viewLifecycleOwner, Observer {
                try {
                    snackbar?.setText("Agregados... $it")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
            DirectoryService.getLiveStatus().observe(viewLifecycleOwner, Observer {
                when (it) {
                    DirectoryService.STATE_VERIFYING ->
                        snackbar?.setText("Verificando directorio...")
                    DirectoryService.STATE_INTERRUPTED,
                    DirectoryService.STATE_FINISHED ->
                        snackbar?.safeDismiss()
                }
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_directory, container, false)
        fragmentHolder = DirMainFragmentHolder(view, childFragmentManager)
        EAHelper.enter1("D")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PrefsUtil.isDirectoryFinished)
            lifecycleScope.launch(Dispatchers.IO) {
                delay(1000)
                find<BannerContainerView>(R.id.adContainer).implBanner(AdsType.DIRECTORY_BANNER, true)
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

        fun get(): DirectoryFragment {
            return DirectoryFragment()
        }
    }
}
