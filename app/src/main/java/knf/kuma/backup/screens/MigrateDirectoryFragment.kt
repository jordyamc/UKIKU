package knf.kuma.backup.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import knf.kuma.directory.DirectoryService.Companion.STATE_FINISHED
import knf.kuma.directory.DirectoryService.Companion.STATE_FULL
import knf.kuma.directory.DirectoryService.Companion.STATE_INTERRUPTED
import knf.kuma.directory.DirectoryService.Companion.STATE_PARTIAL
import kotlinx.android.synthetic.main.lay_migrate_directory.*

class MigrateDirectoryFragment : Fragment() {

    private var onDirStatus: DirectoryService.OnDirStatus? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.lay_migrate_directory, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        CacheDB.INSTANCE.animeDAO().allList.observe(this, Observer { animeObjects -> tv_directory_count.text = animeObjects.size.toString() })
        DirectoryService.getLiveStatus().observe(this, Observer { integer ->
            if (integer != null)
                when (integer) {
                    STATE_PARTIAL -> Log.e("Dir", "Partial search")
                    STATE_FULL -> Log.e("Dir", "Full search")
                    STATE_INTERRUPTED -> {
                        Log.e("Dir", "Interrupted")
                        loading.visibility = View.GONE
                        tv_error.text = "Error: Creacion interrumpida"
                        tv_error.visibility = View.VISIBLE
                    }
                    STATE_FINISHED -> {
                        Log.e("Dir", "Finished")
                        onDirStatus!!.onFinished()
                    }
                }
        })
    }

    fun setOnDirStatus(onDirStatus: DirectoryService.OnDirStatus) {
        this.onDirStatus = onDirStatus
    }

    companion object {

        operator fun get(dirStatus: DirectoryService.OnDirStatus): MigrateDirectoryFragment {
            val fragment = MigrateDirectoryFragment()
            fragment.setOnDirStatus(dirStatus)
            return fragment
        }
    }
}
