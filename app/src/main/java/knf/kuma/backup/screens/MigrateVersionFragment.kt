package knf.kuma.backup.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import knf.kuma.R
import kotlinx.android.synthetic.main.lay_migrate_version.*

class MigrateVersionFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.lay_migrate_version, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        tv_version_bad.text = getInstalledCode(context).toString()
    }

    companion object {

        fun getInstalledCode(context: Context?): Int {
            return try {
                val info = context!!.packageManager.getPackageInfo("knf.animeflv", 0)
                info.versionCode
            } catch (e: Exception) {
                -1
            }

        }
    }
}
