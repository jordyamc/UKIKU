package knf.kuma.custom

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.marlonlom.utilities.timeago.TimeAgo
import knf.kuma.R
import knf.kuma.backup.firestore.FirestoreManager
import kotlinx.android.synthetic.main.view_sync_firestore.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.defaultSharedPreferences


class SyncStaticItemView : RelativeLayout {

    private var cardTitle: String? = "Error"
    private var showDivider = true
    private var prefId: String = "neutral"
    private lateinit var lastState: FirestoreManager.State
    private var rotateAnimation: RotateAnimation? = null
    private var isRotating = false
    private var stopRotating = false

    constructor(context: Context) : super(context) {
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        inflate(context)
        setDefaults(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context)
        setDefaults(context, attrs)
    }

    private fun inflate(context: Context) {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_sync_firestore, this)
    }

    private fun setDefaults(context: Context, attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.SyncStaticItemView)
        cardTitle = array.getString(R.styleable.SyncStaticItemView_ssi_title)
        showDivider = array.getBoolean(R.styleable.SyncStaticItemView_ssi_showDivider, true)
        prefId = array.getString(R.styleable.SyncStaticItemView_ssi_prefId) ?: "neutral"
        array.recycle()
    }

    fun suscribe(owner: LifecycleOwner, liveData: LiveData<FirestoreManager.State>) {
        liveData.observe(owner, Observer {
            when (it) {
                FirestoreManager.State.IDLE -> stateOk(it)
                FirestoreManager.State.UPLOAD, FirestoreManager.State.SYNC -> stateSync(it)
                else -> stateOk(FirestoreManager.State.IDLE)
            }
        })
    }

    private fun stateOk(state: FirestoreManager.State) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!::lastState.isInitialized) {
                lastState = state
                indicator.setImageResource(R.drawable.ic_check_bold)
                stateText.text = "Última sincronización: ${timeAgo()}"
                return@launch
            } else if (lastState != FirestoreManager.State.IDLE) {
                lastState = state
                stopRotating = true
                while (isRotating) {
                    delay(100)
                }
                val transform = ContextCompat.getDrawable(context, R.drawable.anim_sync_check) as? AnimatedVectorDrawable
                indicator.setImageDrawable(transform)
                transform?.start()
                delay(600)
                stateText.text = "Última sincronización: ${timeAgo()}"
                indicator.setImageResource(R.drawable.ic_check_bold)
            }
        }
    }

    private fun timeAgo() = context.defaultSharedPreferences.getLong(prefId, -1L).let {
        if (it == -1L)
            "Sin registros"
        else
            TimeAgo.using(it)
    }

    private fun stateSync(state: FirestoreManager.State) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!::lastState.isInitialized) {
                lastState = state
                stateText.text = "Sincronizando..."
                indicator.setImageResource(R.drawable.ic_sync_rotate)
            } else if (lastState == FirestoreManager.State.IDLE) {
                lastState = state
                val transform = ContextCompat.getDrawable(context, R.drawable.anim_check_sync) as? AnimatedVectorDrawable
                indicator.setImageDrawable(transform)
                transform?.start()
                delay(600)
                stateText.text = "Sincronizando..."
                indicator.setImageResource(R.drawable.ic_sync_rotate)
            }
            rotateAnimation = RotateAnimation(180f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                duration = 500
                interpolator = LinearInterpolator()
                repeatCount = Animation.INFINITE
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(p0: Animation?) {
                        if (stopRotating) {
                            p0?.repeatCount = 1
                            stopRotating = false
                        }
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        isRotating = false
                    }

                    override fun onAnimationStart(p0: Animation?) {
                    }
                })
            }.also {
                isRotating = true
                indicator.startAnimation(it)
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        title.text = cardTitle
        if (!showDivider)
            separator?.visibility = View.GONE
    }
}
