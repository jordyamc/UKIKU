package knf.kuma.backup.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.Fragment
import knf.kuma.App
import knf.kuma.R
import org.jetbrains.anko.find

class MigrateVersionFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.lay_migrate_version, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.find<TextView>(R.id.tv_version_bad).text = installedCode.toString()
    }

    companion object {

        val installedCode: Long
            get() {
            return try {
                val info = App.context.packageManager.getPackageInfo("knf.animeflv", 0)
                PackageInfoCompat.getLongVersionCode(info)
            } catch (e: Exception) {
                -1
            }
        }
    }
}
