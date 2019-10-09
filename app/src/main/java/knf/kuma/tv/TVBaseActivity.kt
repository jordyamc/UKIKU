package knf.kuma.tv


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import knf.kuma.R
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.SSLSkipper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
open class TVBaseActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_main)
        SSLSkipper.skip()
        FirestoreManager.start()
    }

    fun addFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.tv_frame_content, fragment)
        fragmentTransaction.commit()
    }
}
