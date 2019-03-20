package knf.kuma.queue

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import xdroid.toaster.Toaster

class QueueActivity : GenericActivity(), QueueAnimesAdapter.OnAnimeSelectedListener, QueueAllAdapter.OnStartDragListener {
    val toolbar: Toolbar by bind(R.id.toolbar)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    private val listToolbar: Toolbar by bind(R.id.list_toolbar)
    private val listRecyclerView: RecyclerView by bind(R.id.list_recycler)
    val cardView: MaterialCardView by bind(R.id.bottom_card)
    val errorView: View by bind(R.id.error)
    internal var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>? = null
    private var listAdapter: QueueListAdapter? = null

    private var mItemTouchHelper: ItemTouchHelper? = null

    private var current: QueueObject? = null

    private var currentData: LiveData<MutableList<QueueObject>> = MutableLiveData()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.activity_queue
        } else {
            R.layout.activity_queue_grid
        }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(layout)
        toolbar.title = "Pendientes"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        menuInflater.inflate(R.menu.menu_play_queue, listToolbar.menu)
        listToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.play -> {
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    QueueManager.startQueue(applicationContext, listAdapter?.list ?: listOf())
                }
                R.id.clear ->
                    MaterialDialog(this@QueueActivity).safeShow {
                        message(text = "¿Remover los episodios pendientes?")
                        positiveButton(text = "remover") {
                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                            QueueManager.remove(listAdapter?.list ?: mutableListOf())
                        }
                        negativeButton(text = "cancelar")
                    }
            }
            true
        }
        bottomSheetBehavior = BottomSheetBehavior.from(cardView)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    current = null
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })
        setLayoutManager(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("queue_is_grouped", true))
        listRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
        listAdapter = QueueListAdapter { closeSheet() }
        listRecyclerView.adapter = listAdapter
        /*Aesthetic.get().colorAccent().take(1).subscribe {
            listToolbar.backgroundColor = it
        }*/
        reload()
        if (savedInstanceState != null && savedInstanceState.getBoolean("isOpen", false))
            onSelect(savedInstanceState.getSerializable("current") as QueueObject)
    }

    private fun reload() {
        currentData.removeObservers(this)
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("queue_is_grouped", true)) {
            currentData = CacheDB.INSTANCE.queueDAO().all
            currentData.observe(this, Observer { list ->
                errorView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                val animesAdapter = QueueAnimesAdapter(this@QueueActivity)
                recyclerView.adapter = animesAdapter
                dettachHelper()
                mItemTouchHelper = ItemTouchHelper(NoTouchHelperCallback())
                mItemTouchHelper?.attachToRecyclerView(recyclerView)
                animesAdapter.update(QueueObject.getOne(list))
            })
        } else {
            currentData = CacheDB.INSTANCE.queueDAO().allAsort
            currentData.observe(this, object : Observer<MutableList<QueueObject>> {
                override fun onChanged(list: MutableList<QueueObject>?) {
                    clearInterfaces()
                    errorView.visibility = if (list?.isEmpty() == true) View.VISIBLE else View.GONE
                    val allAdapter = QueueAllAdapter(this@QueueActivity)
                    recyclerView.adapter = allAdapter
                    dettachHelper()
                    mItemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(allAdapter))
                    mItemTouchHelper?.attachToRecyclerView(recyclerView)
                    allAdapter.update(list ?: mutableListOf())
                    currentData.removeObserver(this)
                }
            })
        }
    }

    private fun dettachHelper() {
        if (mItemTouchHelper != null)
            mItemTouchHelper?.attachToRecyclerView(null)
    }

    private fun clearInterfaces() {
        if (recyclerView.adapter is QueueAnimesAdapter)
            (recyclerView.adapter as QueueAnimesAdapter).clear()
    }

    private fun setLayoutManager(isFull: Boolean) {
        if (isFull || PrefsUtil.layType == "0") {
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down)
        } else {
            recyclerView.layoutManager = GridLayoutManager(this, gridColumns())
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.grid_fall_down)
        }
    }

    override fun onSelect(queueObject: QueueObject) {
        if (queueObject.equalsAnime(current)) {
            closeSheet()
        } else {
            listToolbar.title = queueObject.chapter.name
            val liveData = CacheDB.INSTANCE.queueDAO().getByAid(queueObject.chapter.aid)
            liveData.observe(this, object : Observer<MutableList<QueueObject>> {
                override fun onChanged(list: MutableList<QueueObject>?) {
                    if (list?.isEmpty() == true)
                        bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
                    else {
                        listAdapter?.update(queueObject.chapter.aid, list ?: mutableListOf())
                        bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_EXPANDED)
                    }
                    current = queueObject
                    liveData.removeObserver(this)
                }
            })
        }
    }

    private fun closeSheet() {
        current = null
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onStartDrag(holder: RecyclerView.ViewHolder) {
        mItemTouchHelper?.startDrag(holder)
    }

    override fun onListCleared() {
        errorView.post { errorView.visibility = View.VISIBLE }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("queue_is_grouped", true))
            menuInflater.inflate(R.menu.menu_queue_group, menu)
        else
            menuInflater.inflate(R.menu.menu_queue_list, menu)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preferences.getBoolean("is_queue_info_shown", false)) {
            preferences.edit().putBoolean("is_queue_info_shown", true).apply()
            onOptionsItemSelected(menu.findItem(R.id.info))
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        when (item.itemId) {
            R.id.info ->
                MaterialDialog(this).safeShow {
                    message(text = "Los episodios añadidos desde servidor podrían dejar de funcionar después de días sin reproducir")
                    positiveButton(text = "OK")
                }
            R.id.queue_group -> {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("queue_is_grouped", true).apply()
                setLayoutManager(false)
                reload()
                invalidateOptionsMenu()
            }
            R.id.queue_list -> {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("queue_is_grouped", false).apply()
                setLayoutManager(true)
                reload()
                invalidateOptionsMenu()
            }
            R.id.play -> {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                val list = (recyclerView.adapter as? QueueAllAdapter)?.list ?: mutableListOf()
                if (list.size > 0)
                    QueueManager.startQueue(applicationContext, list)
                else
                    Toaster.toast("La lista esta vacia")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (current != null) {
            outState.putSerializable("current", current)
            outState.putBoolean("isOpen", true)
        } else
            outState.putBoolean("isOpen", false)
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
        else
            super.onBackPressed()
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, QueueActivity::class.java))
        }
    }
}
