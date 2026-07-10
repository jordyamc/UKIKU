package knf.kuma.custom

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import knf.kuma.App
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.asPx
import knf.kuma.commons.findActivity
import knf.kuma.commons.isFullMode
import knf.kuma.commons.noCrash
import knf.kuma.databinding.LayStatusBarBinding
import knf.tools.bypass.startBypass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.textColor
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.time.Duration.Companion.seconds

@SuppressLint("SetTextI18n")
class ConnectionState : LinearLayout {
    constructor(context: Context) : super(context) {
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context, attrs)
    }

    private var bgColor = Color.TRANSPARENT
    private lateinit var binding: LayStatusBarBinding

    private fun inflate(context: Context, attrs: AttributeSet? = null) {
        context.obtainStyledAttributes(attrs, R.styleable.ConnectionState).use {
            bgColor = it.getColor(R.styleable.ConnectionState_cs_bg_color, ContextCompat.getColor(context, R.color.colorPrimary))
        }
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.lay_status_bar, this)
        binding = LayStatusBarBinding.bind(this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.container.setBackgroundColor(bgColor)
    }

    private var isInitialized = false

    fun setUp(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        if (!isInitialized || isVisible)
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
                //doNetworkTests(owner, onShowDialog)
                delay(1.seconds)
                okState()
                dismiss()
                isInitialized = true
            }
    }

    private suspend fun doNetworkTests(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        when (val code = withContext(Dispatchers.IO + untilDestroyJob(owner)) {
            doCookiesTest()
        }) {
            -2 -> networkTimeoutState(owner, onShowDialog)
            200 -> {
                okState()
                dismiss()
            }
            403 -> {
                errorBlockedState(403, onShowDialog)
                var defResult = true
                val observer = Observer<Pair<Boolean, Boolean>> {
                    if (defResult) {
                        defResult = false
                        return@Observer
                    }
                    GlobalScope.launch(Dispatchers.Main + untilDestroyJob(owner)) {
                        if (BypassUtil.isLoading) {
                            warningCreatingState()
                        } else if (it.first && !it.second) {
                            okState()
                            delay(1000)
                            dismiss()
                            GenericActivity.removeBypassObserver("connectionState")
                        } else if (!it.first && !it.second) {
                            //dismiss()
                            GenericActivity.removeBypassObserver("connectionState")
                            normalState()
                            doNetworkTests(owner, onShowDialog)
                        }
                    }
                }
                GenericActivity.addBypassObserver("connectionState", owner, observer)
            }
            502 -> errorDownState(owner, onShowDialog)
            503 -> {
                errorBlockedState(503, onShowDialog)
                var defResult = true
                val observer = Observer<Pair<Boolean, Boolean>> {
                    if (defResult) {
                        defResult = false
                        return@Observer
                    }
                    GlobalScope.launch(Dispatchers.Main + untilDestroyJob(owner)) {
                        if (BypassUtil.isLoading) {
                            warningCreatingState()
                        } else if (it.first && !it.second) {
                            okState()
                            delay(1000)
                            dismiss()
                            GenericActivity.removeBypassObserver("connectionState")
                        } else if (!it.first && !it.second) {
                            //dismiss()
                            GenericActivity.removeBypassObserver("connectionState")
                            normalState()
                            doNetworkTests(owner, onShowDialog)
                        }
                    }
                }
                GenericActivity.addBypassObserver("connectionState", owner, observer)
            }
            else -> {
                try {
                    val json = JSONObject(withContext(Dispatchers.IO) { URL("https://ipinfo.io/json").readText() })
                    if (json.getString("country") == "PE") {
                        errorCountryState(owner, onShowDialog)
                    } else {
                        networkErrorState(owner = owner, onShowDialog = onShowDialog)
                    }
                } catch (e: Exception) {
                    when (e) {
                        is UnknownHostException, is NoRouteToHostException, is SSLException -> {
                            okState()
                            dismiss()
                        }

                        else -> {
                            networkErrorState(owner = owner, message = "HTTP $code Error", onShowDialog = onShowDialog)
                        }
                    }
                }
            }
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
            val response = Jsoup.connect(BypassUtil.testLink)
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
            Jsoup.connect("https://animeav1.com/")
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
        binding.container.setBackgroundColor(bgColor)
        binding.progress.visibility = VISIBLE
        binding.icon.visibility = GONE
        binding.message.text = "Comprobando..."
        binding.message.textColor = ContextCompat.getColor(context, R.color.textSecondary)
        binding.container.setOnClickListener(null)
        binding.container.setOnLongClickListener(null)
    }

    private fun noNetworkState() {
        binding.container.setBackgroundColor(bgColor)
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_no_network)
        binding.icon.visibility = VISIBLE
        binding.message.text = "Sin internet!"
        binding.message.textColor = ContextCompat.getColor(context, R.color.textSecondary)
        binding.container.setOnClickListener(null)
        binding.container.setOnLongClickListener(null)
    }

    private fun errorDownState(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_error)
        binding.icon.visibility = VISIBLE
        binding.message.text = "Animeav1 caido"
        binding.message.textColor = Color.WHITE
        binding.container.onClick { onShowDialog("Animeav1 parece estar caido por el momento, intenta de nuevo mas tarde") }
        binding.container.onLongClick { setUp(owner, onShowDialog) }
    }

    private fun errorBlockedState(errorCode: Int, onShowDialog: (message: String) -> Unit) {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_error)
        binding.icon.visibility = VISIBLE
        binding.message.text = "Error HTTP $errorCode!"
        binding.message.textColor = Color.WHITE
        binding.container.onClick {
            PrefsUtil.isForbiddenTipShown = true
            //context.startActivity(Intent(context, Diagnostic.FullBypass::class.java))
            (context.findActivity() as? AppCompatActivity)?.startBypass(
                4157,
                BypassUtil.createRequest()
            )
        }
        binding.container.setOnLongClickListener(null)
        if (isFullMode && !PrefsUtil.isForbiddenTipShown) {
            noCrash {
                Tooltip.Builder(context).apply {
                    arrow(true)
                    text("Haz click en la barra roja para resolver el captcha!")
                    overlay(true)
                    styleId(R.style.ToolTipAltStyle)
                    anchor(this@ConnectionState)
                    closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                }.create().show(this, Tooltip.Gravity.TOP)
            }
        }
    }

    private fun errorCountryState(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_error)
        binding.icon.visibility = VISIBLE
        binding.message.text = "VPN necesario"
        binding.message.textColor = Color.WHITE
        binding.container.onClick { onShowDialog("Tu pais ha bloqueado la conexion con Animeav1 por lo que es necesario usar una VPN para seguir usando la app") }
        binding.container.onLongClick { setUp(owner, onShowDialog) }
    }

    private fun networkErrorState(owner: LifecycleOwner, message: String = "Error desconocido!", onShowDialog: (message: String) -> Unit) {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_error)
        binding.icon.visibility = VISIBLE
        binding.message.text = message
        binding.message.textColor = Color.WHITE
        binding.container.onClick { onShowDialog("Hubo un error haciendo las pruebas de conexion!") }
        binding.container.onLongClick { setUp(owner, onShowDialog) }
    }

    private fun networkTimeoutState(owner: LifecycleOwner, onShowDialog: (message: String) -> Unit) {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_error)
        binding.icon.visibility = VISIBLE
        binding.message.text = "Animeav1 lento!"
        binding.message.textColor = Color.WHITE
        binding.container.onClick { onShowDialog("Se detectó un problema con la página de Animeav1, es posible que esté en mantenimiento, este problema se solucionará solo, no es necesario reportarlo!") }
        binding.container.onLongClick { setUp(owner, onShowDialog) }
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

    private fun warningCustomState(message: String, dialogText: String, onShowDialog: (message: String) -> Unit) {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentAmber))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_warning)
        binding.icon.visibility = VISIBLE
        binding.message.text = message
        binding.message.textColor = Color.WHITE
        binding.container.setOnClickListener { onShowDialog(dialogText) }
    }

    private fun warningCreatingState() {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentAmber))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_warning)
        binding.icon.visibility = VISIBLE
        binding.message.text = "Actualizando bypass!"
        binding.message.textColor = Color.WHITE
        binding.container.setOnClickListener(null)
        binding.container.setOnLongClickListener(null)
    }

    private fun okState() {
        binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentGreen))
        binding.progress.visibility = GONE
        binding.icon.setImageResource(R.drawable.ic_check)
        binding.icon.visibility = VISIBLE
        binding.message.text = "Todo en orden!"
        binding.message.textColor = Color.WHITE
        binding.container.setOnClickListener(null)
        binding.container.setOnLongClickListener(null)
    }

    private fun show() {
        val height = 24.asPx
        layoutParams = layoutParams.apply {
            this.height = 0
        }
        visibility = VISIBLE
        ValueAnimator.ofInt(0, height).apply {
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
        ValueAnimator.ofInt(height, 0).apply {
            addUpdateListener {
                layoutParams = layoutParams.apply {
                    this.height = it.animatedValue as Int
                }
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                    layoutParams = layoutParams.apply {
                        this.height = height
                    }
                }

                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationStart(animation: Animator) {

                }
            })
            duration = 350
        }.start()
    }

}
