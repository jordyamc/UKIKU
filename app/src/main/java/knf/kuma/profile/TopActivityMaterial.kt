package knf.kuma.profile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ListenerRegistration
import knf.kuma.R
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.FullscreenAdLoader
import knf.kuma.ads.getFAdLoaderInterstitial
import knf.kuma.ads.getFAdLoaderRewarded
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.backup.firestore.data.TopData
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.recycler_loader_material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import kotlin.contracts.ExperimentalContracts

class TopActivityMaterial : GenericActivity() {

    private lateinit var listener: ListenerRegistration

    private var snackbar: Snackbar? = null
    private var isEditing = false

    private val topAdapter: TopAdapter by lazy { TopAdapter() }

    private val rewardedAd: FullscreenAdLoader by lazy { getFAdLoaderRewarded(this) }
    private var interstitial: FullscreenAdLoader = getFAdLoaderInterstitial(this)

    private var topList: List<TopData> = emptyList()

    private val sync: String? by lazy { FirestoreManager.updateTopSync() }

    private fun showAd() {
        diceOf<() -> Unit> {
            put({ rewardedAd.show() }, AdsUtils.remoteConfigs.getDouble("rewarded_percent"))
            put({ interstitial.show() }, AdsUtils.remoteConfigs.getDouble("interstitial_percent"))
        }()
    }

    @OptIn(ExperimentalContracts::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(R.layout.recycler_loader_material)
        setSupportActionBar(toolbar)
        title = "Videos vistos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        recycler.apply {
            addItemDecoration(DividerItemDecoration(this@TopActivityMaterial, DividerItemDecoration.VERTICAL))
            adapter = topAdapter
        }
        loading.show()
        listen()
        rewardedAd.load()
        interstitial.load()
    }

    @ExperimentalContracts
    private fun listen() {
        if (::listener.isInitialized)
            listener.remove()
        listener = FirestoreManager.listenTop { topList ->
            this.topList = topList
            reload()
        }
    }

    private fun reload() {
        lifecycleScope.launch(Dispatchers.IO) {
            sync
            launch(Dispatchers.Main) { loading.show() }
            val sorted = topList.sortedByDescending { it.number }
            val list = sorted.take(PrefsUtil.topCount).mapIndexed { index, topData -> TopItem(index + 1, topData) }.toMutableList()
            val current = FirestoreManager.uid?.let { uid -> sorted.find { it.uid == uid } }
            val currentPosition = current?.let { sorted.indexOf(it) } ?: 999
            if (current != null && currentPosition > PrefsUtil.topCount - 1)
                list.add(TopItem(currentPosition + 1, current))
            launch(Dispatchers.Main) {
                loading.hide()
                topAdapter.submitList(list)
            }
        }
    }

    private fun showSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
        snackbar?.dismiss()
        snackbar = Snackbar.make(recycler, text, duration).also { it.show() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top, menu)
        when (PrefsUtil.topCount) {
            25 -> menu.findItem(R.id.top25)?.isChecked = true
            50 -> menu.findItem(R.id.top50)?.isChecked = true
            75 -> menu.findItem(R.id.top75)?.isChecked = true
            100 -> menu.findItem(R.id.top100)?.isChecked = true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.coins -> {
                Economy.showWallet(this) { showAd() }
            }
            R.id.editName -> {
                if (!isEditing)
                    MaterialDialog(this).safeShow {
                        title(text = "Editar nombre")
                        input(prefill = FirestoreManager.user?.displayName
                                ?: PrefsUtil.instanceName, maxLength = 30) { _, text ->
                            val checked = text.replace("[^\\wA-zÀ-ú &\\-()\\[\\]\"#\$!¿?¡%{}@_/]*".toRegex(), "").trim()
                            if (checked.isEmpty() || checked.length <= 3) {
                                if (checked.isEmpty())
                                    toast("El nombre no puede estar vacío o con caracteres invalidos")
                                else if (checked.length <= 3)
                                    toast("El nombre debe tener mas de 3 caracteres")
                                return@input
                            }
                            if (FirestoreManager.isLoggedIn) {
                                isEditing = true
                                FirestoreManager.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(checked).build())?.apply {
                                    addOnSuccessListener {
                                        isEditing = false
                                        showSnackbar("Nombre editado exitosamente")
                                        FirestoreManager.updateTop()
                                    }
                                    addOnFailureListener {
                                        isEditing = false
                                        showSnackbar("Error al editar nombre\n${it.message}", Snackbar.LENGTH_LONG)
                                    }
                                } ?: showSnackbar("Error al editar nombre")
                                showSnackbar("Editando nombre...", Snackbar.LENGTH_INDEFINITE)
                            } else {
                                PrefsUtil.instanceName = checked
                                showSnackbar("Nombre editado exitosamente")
                                FirestoreManager.updateTop()
                            }
                        }
                        getInputLayout().boxBackgroundColor = Color.TRANSPARENT
                        getInputField().setBackgroundColor(Color.TRANSPARENT)
                    }
            }
            R.id.top25 -> {
                PrefsUtil.topCount = 25
                reload()
                invalidateOptionsMenu()
            }
            R.id.top50 -> {
                PrefsUtil.topCount = 50
                reload()
                invalidateOptionsMenu()
            }
            R.id.top75 -> {
                PrefsUtil.topCount = 75
                reload()
                invalidateOptionsMenu()
            }
            R.id.top100 -> {
                PrefsUtil.topCount = 100
                reload()
                invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::listener.isInitialized)
            listener.remove()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, TopActivityMaterial::class.java))
        }
    }
}