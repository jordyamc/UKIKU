package knf.kuma.backup.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import com.google.android.material.snackbar.Snackbar
import knf.kuma.App
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.backup.objects.FavList
import knf.kuma.backup.objects.SeenList
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.showSnackbar
import knf.kuma.commons.toast
import knf.kuma.database.CacheDB
import kotlinx.android.synthetic.main.lay_migrate_success.view.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

class MigrateSuccessFragment : Fragment() {

    private val REQUEST_FAVS = 5628
    private val REQUEST_SEEN = 9986
    private lateinit var root: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.lay_migrate_success, container, false)
        root = view.root
        view.migrate_favs.setOnClickListener { onMigrateFavs() }
        view.migrate_seen.setOnClickListener { onMigrateSeen() }
        return view
    }

    private fun onMigrateFavs() {
        try {
            startActivityForResult(Intent().setAction("knf.kuma.MIGRATE").putExtra("type", 0), REQUEST_FAVS)
        } catch (e: ActivityNotFoundException) {
            "No se encontró Animeflv App o la version es incorrecta!".toast()
        }
    }

    private fun onMigrateSeen() {
        try {
            startActivityForResult(Intent().setAction("knf.kuma.MIGRATE").putExtra("type", 1), REQUEST_SEEN)
        } catch (e: ActivityNotFoundException) {
            "No se encontró Animeflv App o la version es incorrecta!".toast()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snackbar = root.showSnackbar("Migrando...", Snackbar.LENGTH_INDEFINITE)
        doAsync {
            try {
                data?.data?.let {
                    when (requestCode) {
                        REQUEST_FAVS -> {
                            val list = FavList.decode(App.context.contentResolver.openInputStream(it))
                                    ?: return@let null
                            CacheDB.INSTANCE.favsDAO().addAll(list)
                            syncData { favs() }
                        }
                        REQUEST_SEEN -> {
                            val chapters = SeenList.decode(App.context.contentResolver.openInputStream(it))
                                    ?: return@let null
                            CacheDB.INSTANCE.seenDAO().addAll(chapters)
                            syncData { seen() }
                        }
                        else -> null
                    }
                } ?: throw IllegalStateException("Data or IS is null!")
            } catch (e: Exception) {
                e.printStackTrace()
                Crashlytics.logException(e)
                Toaster.toast("Error al migrar datos")
            }
            snackbar.safeDismiss()
        }
    }
}
