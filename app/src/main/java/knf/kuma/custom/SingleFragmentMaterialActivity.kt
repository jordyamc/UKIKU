package knf.kuma.custom

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.setSurfaceBars
import org.jetbrains.anko.find

abstract class SingleFragmentMaterialActivity : AppCompatActivity() {

    private val layoutResId: Int
        @LayoutRes
        get() = R.layout.activity_fragment_material

    protected abstract fun createFragment(): Fragment

    abstract fun getActivityTitle(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(layoutResId)

        with(find<Toolbar>(R.id.toolbar)) {
            setSupportActionBar(this)
            setNavigationOnClickListener { onBackPressed() }
        }
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getActivityTitle()

        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            fragment = createFragment()
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
        }
    }
}