package knf.kuma.backup.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import knf.kuma.R
import knf.kuma.backup.objects.FavList
import knf.kuma.backup.objects.SeenList
import knf.kuma.commons.createIndeterminateSnackbar
import knf.kuma.commons.safeDismiss
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
        startActivityForResult(Intent().setAction("knf.kuma.MIGRATE").putExtra("type", 0), REQUEST_FAVS)
    }

    private fun onMigrateSeen() {
        startActivityForResult(Intent().setAction("knf.kuma.MIGRATE").putExtra("type", 1), REQUEST_SEEN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snackbar = root.createIndeterminateSnackbar("Migrando...")
        doAsync {
            try {
                when (requestCode) {
                    REQUEST_FAVS -> {
                        val list = FavList.decode(context!!.contentResolver.openInputStream(data!!.data!!)!!)
                        CacheDB.INSTANCE.favsDAO().addAll(list)
                    }
                    REQUEST_SEEN -> {
                        val chapters = SeenList.decode(context!!.contentResolver.openInputStream(data!!.data!!)!!)
                        CacheDB.INSTANCE.chaptersDAO().addAll(chapters)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Crashlytics.logException(e)
                Toaster.toast("Error al migrar datos")
            }
            snackbar.safeDismiss()
        }
    }
}
