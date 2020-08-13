package knf.kuma.explorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.snackbar.Snackbar
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.doOnUI
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.safeShow
import knf.kuma.commons.showSnackbar
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.pojos.DownloadObject
import kotlinx.android.synthetic.main.recycler_downloading.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick

class FragmentDownloadsMaterial : FragmentBase() {
    private var isFirst = true
    private var adapter: DownloadingAdapterMaterial? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adContainer.implBanner(AdsType.EXPLORER_BANNER, true)
        CacheDB.INSTANCE.downloadsDAO().active.observe(viewLifecycleOwner, Observer { downloadObjects ->
            progress?.visibility = View.GONE
            error?.visibility = if (downloadObjects.isEmpty()) View.VISIBLE else View.GONE
            clear?.visibility = if (downloadObjects.isEmpty()) View.GONE else View.VISIBLE
            if (isFirst || downloadObjects.isEmpty() || recycler.adapter != null && downloadObjects.size > recycler.adapter?.itemCount ?: 0) {
                isFirst = false
                recycler.adapter = DownloadingAdapterMaterial(this@FragmentDownloadsMaterial, downloadObjects as MutableList<DownloadObject>).also { adapter = it }
            }
        })
        clear.onClick {
            activity?.let {
                MaterialDialog(it).safeShow {
                    lifecycleOwner()
                    message(text = "¿Desea limpiar todas las descargas en la lista?")
                    positiveButton(text = "limpiar") {
                        onRemoveAll()
                    }
                    negativeButton(text = "Cancelar")
                }
            }
        }
    }

    private fun onRemoveAll() {
        clear.visibility = View.GONE
        val snackbar = recycler.showSnackbar("Limpiando lista...", Snackbar.LENGTH_INDEFINITE)
        doAsync {
            DownloadManager.cancelAll()
            doOnUI { snackbar.safeDismiss() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recycler_downloading, container, false)
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    companion object {

        fun get(): FragmentDownloadsMaterial {
            return FragmentDownloadsMaterial()
        }
    }
}
