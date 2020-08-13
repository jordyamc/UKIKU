package knf.kuma.explorer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter

class ExplorerPagerAdapterMaterial(context: Context, private val fragmentManager: FragmentManager) : PagerAdapter() {
    private val fragments: Array<Fragment?> = arrayOfNulls(2)
    private val stateChange: OnFileStateChange? = context as? OnFileStateChange

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = getItem(position)
        try {
            fragment?.let {
                val trans = fragmentManager.beginTransaction()
                trans.add(container.id, fragment, "fragment:$position")
                trans.commit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return fragment ?: Any()
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        val fragment = fragments[position]
        fragment?.let {
            val trans = fragmentManager.beginTransaction()
            trans.remove(fragment)
            trans.commit()
        }
        fragments[position] = null
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return (any as? Fragment)?.view === view
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Archivos"
            1 -> "Descargas"
            else -> "Archivos"
        }
    }

    fun getItem(position: Int): Fragment? {
        if (fragments[position] == null) {
            fragments[position] = createFragment(position)
            if (position == 0)
                (fragments[position] as? FragmentFilesRootMaterial)?.setStateChange(stateChange)
        }
        return fragments[position]
    }

    private fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> FragmentDownloadsMaterial.get()
            else -> FragmentFilesRootMaterial.get()
        }
    }

    internal fun onRemoveAllClicked() {
        try {
            (fragments[0] as? FragmentFilesRootMaterial)?.onRemoveAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
