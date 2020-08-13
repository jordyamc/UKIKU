package knf.kuma.custom

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.lifecycle.*
import knf.kuma.App
import knf.kuma.Diagnostic
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import kotlinx.android.synthetic.main.lay_status_bar.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.textColor
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.SocketTimeoutException

@SuppressLint("SetTextI18n")
class ConnectionState : LinearLayout {
    constructor(context: Context) : super(context) {
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(context,attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context,attrs)
    }

    private var bgColor = Color.TRANSPARENT

    private fun inflate(context: Context, attrs: AttributeSet? = null) {
        context.obtainStyledAttributes(attrs,R.styleable.ConnectionState).use {
            bgColor = it.getColor(R.styleable.ConnectionState_cs_bg_color,ContextCompat.getColor(context, R.color.colorPrimary))
        }
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.lay_status_bar, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        container.setBackgroundColor(bgColor)
    }

    private var isInitialized = false

    fun setUp(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        if (!isInitialized || visibility == View.VISIBLE)
            GlobalScope.launch(Dispatchers.Main + untilDestroyJob(owner)) {
                normalState()
                show()
                delay(1000)
                if (!Network.isConnected) {
                    noNetworkState()
                    while (!Network.isConnected) {
                        delay(3000)
                    }
                    normalState()
                }
                doNetworkTests(owner, onShowDialog)
                isInitialized = true
            }
    }

    private suspend fun doNetworkTests(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        when (withContext(Dispatchers.IO + untilDestroyJob(owner)) {
            doCookiesTest()
        }) {
            -2 -> networkTimeoutState(owner, onShowDialog)
            200 -> {
                if (!PrefsUtil.isDirectoryFinished)
                    directoryListenState()
                else {
                    okState()
                    dismiss()
                }
            }
            403 -> {
                errorBlockedState(onShowDialog)
                GenericActivity.bypassLive.observe(owner, Observer {
                    GlobalScope.launch(Dispatchers.Main + untilDestroyJob(owner)) {
                        if (it.first && it.second) {
                            warningCreatingState()
                        } else if (it.first && !it.second) {
                            okState()
                            dismiss()
                        }
                    }
                })
            }
            503 -> {
                warningState(onShowDialog)
                GenericActivity.bypassLive.observe(owner, Observer {
                    GlobalScope.launch(Dispatchers.Main + untilDestroyJob(owner)) {
                        if (it.first && it.second) {
                            warningCreatingState()
                        } else if (it.first && !it.second) {
                            okState()
                            dismiss()
                        }
                    }
                })
            }
            else -> networkErrorState(owner, onShowDialog)
        }
    }

    private fun untilDestroyJob(owner: LifecycleOwner): Job {
        val job = Job()
        owner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                job.cancel()
            }
        })
        return job
    }

    private fun doCookiesTest(): Int {
        return try {
            val timeout = PrefsUtil.timeoutTime.toInt() * 1000
            val response = Jsoup.connect("https://animeflv.net")
                    .cookies(BypassUtil.getMapCookie(App.context))
                    .userAgent(BypassUtil.userAgent)
                    .timeout(if (timeout == 0) 30000 else timeout)
                    .method(Connection.Method.GET)
                    .execute()
            response.statusCode()
        } catch (e: HttpStatusException) {
            e.statusCode
        } catch (e: SocketTimeoutException) {
            -2
        } catch (e: Exception) {
            -1
        }
    }

    private fun tryTimeoutFix(): Boolean {
        val timeout = when (PrefsUtil.timeoutTime) {
            10L -> {
                PrefsUtil.timeoutTime = 20L
                20L
            }
            20L -> {
                PrefsUtil.timeoutTime = 30L
                30L
            }
            else -> return false
        } * 1000
        return try {
            Jsoup.connect("https://animeflv.net")
                    .cookies(BypassUtil.getMapCookie(App.context))
                    .userAgent(BypassUtil.userAgent)
                    .timeout(timeout.toInt())
                    .method(Connection.Method.GET)
                    .execute()
            true
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: Exception) {
            true
        }
    }

    private fun normalState() {
        container.setBackgroundColor(bgColor)
        progress.visibility = View.VISIBLE
        icon.visibility = View.GONE
        message.text = "Comprobando..."
        message.textColor = ContextCompat.getColor(context, R.color.textSecondary)
        container.setOnClickListener(null)
        container.setOnLongClickListener(null)
    }

    private fun noNetworkState() {
        container.setBackgroundColor(bgColor)
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_no_network)
        icon.visibility = View.VISIBLE
        message.text = "Sin internet!"
        message.textColor = ContextCompat.getColor(context, R.color.textSecondary)
        container.setOnClickListener(null)
        container.setOnLongClickListener(null)
    }

    private fun errorBlockedState(onShowDialog: (message: String) -> Unit) {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_error)
        icon.visibility = View.VISIBLE
        message.text = "Error HTTP 403!"
        message.textColor = Color.WHITE
        container.onClick { context.startActivity(Intent(context, Diagnostic.FullBypass::class.java)) }
        container.setOnLongClickListener(null)
    }

    private fun networkErrorState(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_error)
        icon.visibility = View.VISIBLE
        message.text = "Error desconocido!"
        message.textColor = Color.WHITE
        container.onClick { onShowDialog("Hubo un error haciendo las pruebas de conexion!") }
        container.onLongClick { setUp(owner, onShowDialog) }
    }

    private fun networkTimeoutState(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_error)
        icon.visibility = View.VISIBLE
        message.text = "Animeflv lento!"
        message.textColor = Color.WHITE
        container.onClick { onShowDialog("Se detectó un problema con la página de Animeflv, es posible que esté en mantenimiento, este problema se solucionará solo, no es necesario reportarlo!") }
        container.onLongClick { setUp(owner, onShowDialog) }
        owner.doAsync {
            var isFixed = false
            repeat(3) {
                if (!isFixed && tryTimeoutFix()) {
                    isFixed = true
                    setUp(owner, onShowDialog)
                }
            }
        }
    }

    private fun warningState(onShowDialog: (message: String) -> Unit) {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentAmber))
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_warning)
        icon.visibility = View.VISIBLE
        message.text = "Cloudflare activado!"
        message.textColor = Color.WHITE
        container.onClick { onShowDialog("Cloudflare activado, espera al bypass") }
        container.onLongClick { context.startActivity(Intent(context, Diagnostic.FullBypass::class.java)) }
    }

    private fun warningCreatingState() {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentAmber))
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_warning)
        icon.visibility = View.VISIBLE
        message.text = "Actualizando bypass!"
        message.textColor = Color.WHITE
        container.setOnClickListener(null)
        container.setOnLongClickListener(null)
    }

    private suspend fun directoryListenState() {
        (context.findActivity() as? AppCompatActivity)?.let { owner ->
            container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentAmber))
            progress.visibility = View.GONE
            icon.setImageResource(R.drawable.ic_warning)
            icon.visibility = View.VISIBLE
            message.textColor = Color.WHITE
            container.setOnClickListener(null)
            container.setOnLongClickListener(null)
            var maxPages = "3200~"
            CacheDB.INSTANCE.animeDAO().countLive.distinct.observe(owner, Observer {
                message.text = "Creando directorio: $it/$maxPages"
            })
            DirectoryService.getLiveStatus().observe(owner, Observer {
                if (it == DirectoryService.STATE_FINISHED) {
                    owner.lifecycleScope.launch(Dispatchers.Main) {
                        okState()
                        dismiss()
                    }
                }
            })
            maxPages = withContext(Dispatchers.IO) {
                noCrashLet("3200~") {
                    val main = okHttpDocument("https://animeflv.net/browse")
                    val lastPage = main.select("ul.pagination li:matches(\\d+)").last().text().trim().toInt()
                    val last = okHttpDocument("https://animeflv.net/browse?page=$lastPage")
                    ((24 * (lastPage - 1)) + last.select("article").size).toString()
                }
            }
        } ?: {
            okState()
            GlobalScope.launch(Dispatchers.Main) {
                dismiss()
            }
        }()
    }

    private fun okState() {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentGreen))
        progress.visibility = View.GONE
        icon.setImageResource(R.drawable.ic_check)
        icon.visibility = View.VISIBLE
        message.text = "Todo en orden!"
        message.textColor = Color.WHITE
        container.setOnClickListener(null)
        container.setOnLongClickListener(null)
    }

    private fun show() {
        val height = 24.asPx
        layoutParams = layoutParams.apply {
            this.height = 0
        }
        visibility = View.VISIBLE
        ValueAnimator.ofInt(0,height).apply {
            addUpdateListener {
                layoutParams = layoutParams.apply {
                    this.height = it.animatedValue as Int
                }
            }
            duration = 350
        }.start()
    }

    private suspend fun dismiss() {
        delay(1000)
        val height = 24.asPx
        ValueAnimator.ofInt(height,0).apply {
            addUpdateListener {
                layoutParams = layoutParams.apply {
                    this.height = it.animatedValue as Int
                }
            }
            addListener(object : Animator.AnimatorListener{
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    visibility = View.GONE
                    layoutParams = layoutParams.apply {
                        this.height = height
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }
            })
            duration = 350
        }.start()
    }

}
