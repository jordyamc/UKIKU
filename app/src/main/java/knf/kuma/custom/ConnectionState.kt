package knf.kuma.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import knf.kuma.App
import knf.kuma.Diagnostic
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import kotlinx.android.synthetic.main.lay_status_bar.view.*
import kotlinx.coroutines.*
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
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context)
    }

    private fun inflate(context: Context) {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.lay_status_bar, this)
    }

    private var isInitialized = false

    fun setUp(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        if (!isInitialized || visibility == View.VISIBLE)
            GlobalScope.launch(Dispatchers.Main + untilDestroyJob(owner)) {
                normalState()
                visibility = View.VISIBLE
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
                okState()
                dismiss()
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

    private fun normalState() {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
        progress.visibility = View.VISIBLE
        icon.visibility = View.GONE
        message.text = "Comprobando..."
        message.textColor = ContextCompat.getColor(context, R.color.textSecondary)
        container.setOnClickListener(null)
        container.setOnLongClickListener(null)
    }

    private fun noNetworkState() {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
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
        container.onClick { onShowDialog("Tu proveedor de internet bloquea la conexion entre UKIKU y Animeflv") }
        container.onLongClick { context.startActivity(Intent(context, Diagnostic.FullBypass::class.java)) }
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

    private suspend fun dismiss() {
        delay(1000)
        visibility = View.GONE
    }

}
