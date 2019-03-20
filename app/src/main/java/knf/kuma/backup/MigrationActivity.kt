package knf.kuma.backup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import knf.kuma.R
import knf.kuma.backup.screens.MigrateDirectoryFragment
import knf.kuma.backup.screens.MigrateSuccessFragment
import knf.kuma.backup.screens.MigrateVersionFragment
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.GenericActivity
import knf.kuma.directory.DirectoryService

class MigrationActivity : GenericActivity(), DirectoryService.OnDirStatus {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_migrate)
    }

    override fun onResume() {
        super.onResume()
        if (MigrateVersionFragment.installedCode < 252)
            setFragment(MigrateVersionFragment())
        else if (!PrefsUtil.isDirectoryFinished) {
            DirectoryService.run(this)
            setFragment(MigrateDirectoryFragment[this])
        } else
            setFragment(MigrateSuccessFragment())
    }

    private fun setFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.root, fragment)
        transaction.commit()
    }

    override fun onFinished() {
        setFragment(MigrateSuccessFragment())
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, MigrationActivity::class.java))
        }
    }
}
