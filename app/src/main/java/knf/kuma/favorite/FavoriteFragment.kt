package knf.kuma.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.pojos.FavSection
import knf.kuma.pojos.FavoriteObject
import xdroid.toaster.Toaster
import java.util.*

class FavoriteFragment : BottomFragment(), FavsSectionAdapter.OnMoveListener {

    @BindView(R.id.recycler)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.error)
    lateinit var errorLayout: LinearLayout
    private var edited: FavoriteObject? = null
    private var manager: RecyclerView.LayoutManager? = null
    private var adapter: FavsSectionAdapter? = null
    private var isFirst = true

    private var count = 0

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_favs
        } else {
            R.layout.recycler_favs_grid
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        CacheDB.INSTANCE.favsDAO().all.observe(this, Observer { FavSectionHelper.reload() })
        ViewModelProviders.of(activity!!).get(FavoriteViewModel::class.java).getData().observe(this, Observer { favoriteObjects ->
            if (favoriteObjects == null || favoriteObjects.isEmpty()) {
                errorLayout.visibility = View.VISIBLE
                adapter!!.updateList(ArrayList())
            } else if (PrefsUtil.showFavSections()) {
                errorLayout.visibility = View.GONE
                val container = FavSectionHelper.getInfoContainer(edited)
                if (container.needReload) {
                    adapter!!.updateList(favoriteObjects)
                    if (isFirst) {
                        isFirst = false
                        recyclerView.scheduleLayoutAnimation()
                    }
                } else
                    adapter!!.updatePosition(container)
            } else {
                errorLayout.visibility = View.GONE
                adapter!!.updateList(favoriteObjects)
                if (isFirst) {
                    isFirst = false
                    recyclerView.scheduleLayoutAnimation()
                }
            }
            edited = null
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout, container, false)
        ButterKnife.bind(this, view)
        manager = recyclerView.layoutManager
        adapter = FavsSectionAdapter(this, recyclerView)
        if (PrefsUtil.layType == "1" && PrefsUtil.showFavSections()) {
            (manager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return try {
                        if (FavSectionHelper.currentList[position].isSection)
                            (manager as GridLayoutManager).spanCount
                        else
                            1
                    } catch (e: Exception) {
                        1
                    }
                }
            }
        }
        recyclerView.adapter = adapter
        EAHelper.enter1("F")
        return view
    }

    fun onChangeOrder() {
        if (activity != null)
            ViewModelProviders.of(activity!!).get(FavoriteViewModel::class.java).getData().observe(this, Observer { favoriteObjects ->
                if (favoriteObjects == null || favoriteObjects.isEmpty()) {
                    adapter!!.updateList(ArrayList())
                    errorLayout.post { errorLayout.visibility = View.VISIBLE }
                } else {
                    adapter!!.updateList(favoriteObjects)
                    if (isFirst) {
                        isFirst = false
                        recyclerView.scheduleLayoutAnimation()
                    }
                }
            })
    }

    fun showNewCategoryDialog(favoriteObject: FavoriteObject?) {
        edited = favoriteObject

        showNewCategoryDialog(favoriteObject == null, null)
    }

    private fun showNewCategoryDialog(isEmpty: Boolean, name: String?) {
        val categories = FavoriteObject.getCategories(CacheDB.INSTANCE.favsDAO().catagories)
        MaterialDialog(context!!).safeShow {
            title(text = "Nueva categoría")
            input(hint = "Nombre", prefill = name, waitForPositiveButton = false) { dialog, charSequence ->
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, charSequence.isNotEmpty())
            }
            positiveButton(text = "crear") {
                val input = it.getInputField()!!.text.toString()
                if (categories.contains(input)) {
                    Toaster.toast("Esta categoría ya existe")
                    showNewCategoryDialog(isEmpty, name)
                } else {
                    if (isEmpty)
                        showNewCategoryInit(false, input)
                    else {
                        edited!!.category = input
                        CacheDB.INSTANCE.favsDAO().addFav(edited!!)
                        edited = null
                    }
                }
            }
        }
    }

    private fun showNewCategoryInit(isEdit: Boolean, name: String) {
        val fName = if (name == "Sin categoría") FavoriteObject.CATEGORY_NONE else name
        val favoriteObjects = CacheDB.INSTANCE.favsDAO().getNotInCategory(fName)
        if (favoriteObjects.isEmpty()) {
            Toaster.toast("Necesitas favoritos para crear una categoría")
        } else {
            val isNotDefault = isEdit && fName != FavoriteObject.CATEGORY_NONE
            MaterialDialog(context!!).safeShow {
                title(text = name)
                listItemsMultiChoice(items = FavoriteObject.getNames(favoriteObjects)) { _, indices, _ ->
                    edited = null
                    val list = ArrayList<FavoriteObject>()
                    for (i in indices) {
                        val favoriteObject = favoriteObjects[i]
                        favoriteObject.category = fName
                        list.add(favoriteObject)
                    }
                    CacheDB.INSTANCE.favsDAO().addAll(list)
                }
                positiveButton(text = "agregar")
                if (isNotDefault || !isEdit)
                    negativeButton(text =
                    when {
                        isNotDefault -> "cancelar"
                        !isEdit -> "atras"
                        else -> ""
                    }) { _ ->
                        if (isNotDefault)
                            MaterialDialog(context).safeShow {
                                message(text = "¿Desea eliminar esta categoría?")
                                positiveButton(text = "continuar") {
                                    edited = null
                                    val objects = CacheDB.INSTANCE.favsDAO().getAllInCategory(fName)
                                    for (favoriteObject in objects) {
                                        favoriteObject.category = FavoriteObject.CATEGORY_NONE
                                    }
                                    CacheDB.INSTANCE.favsDAO().addAll(objects)
                                }
                            }
                        else if (!isEdit)
                            showNewCategoryDialog(true, name)
                    }
                if (!isEdit)
                    setOnCancelListener { showNewCategoryDialog(true, name) }
            }
        }
    }

    override fun onEdit(category: String) {
        showNewCategoryInit(true, category)
    }

    override fun onSelect(favoriteObject: FavoriteObject) {
        if (favoriteObject !is FavSection) {
            val categories = FavoriteObject.getCategories(CacheDB.INSTANCE.favsDAO().catagories)
            if (categories.size <= 1) {
                edited = favoriteObject
                showNewCategoryDialog(false, null)
            } else {
                MaterialDialog(context!!).safeShow {
                    title(text = "Mover a...")
                    listItemsSingleChoice(items = categories, initialSelection = categories.indexOf(favoriteObject.category)) { _, _, text ->
                        if (text != favoriteObject.category) {
                            edited = favoriteObject
                            edited!!.category = if (text == "Sin categoría") "_NONE_" else text
                            CacheDB.INSTANCE.favsDAO().addFav(edited!!)
                        } else
                            Toaster.toast("Error al mover")
                    }
                    positiveButton(text = "mover")
                    negativeButton(text = "nuevo") {
                        edited = favoriteObject
                        showNewCategoryDialog(false, null)
                    }
                }
            }
        }
    }

    override fun onReselect() {
        EAHelper.enter1("F")
        if (manager != null) {
            manager!!.smoothScrollToPosition(recyclerView, null, 0)
            count++
            if (count == 3) {
                if (adapter != null)
                    Toaster.toast("Tienes " + CacheDB.INSTANCE.favsDAO().count + " animes en favoritos")
                count = 0
            }

        }
    }

    companion object {

        fun get(): FavoriteFragment {
            return FavoriteFragment()
        }
    }
}
