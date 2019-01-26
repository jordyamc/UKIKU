package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.BUUtils
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.Network
import knf.kuma.commons.noCrash
import kotlinx.android.synthetic.main.sync_item_layout.view.*

class SyncItemView : RelativeLayout {

    private var cardTitle: String? = "Error"
    private var showDivider = true
    private var actionId: String = "neutral"

    var bakup: BackupObject<*>? = null
        private set

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
        inflater.inflate(R.layout.sync_item_layout, this)
    }

    private fun setDefaults(context: Context, attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.SyncItemView)
        cardTitle = array.getString(R.styleable.SyncItemView_si_title)
        showDivider = array.getBoolean(R.styleable.SyncItemView_si_showDivider, true)
        actionId = array.getString(R.styleable.SyncItemView_si_actionId) ?: "neutral"
        array.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        title.text = cardTitle
        if (!showDivider)
            separator?.visibility = View.GONE
    }

    fun enableBackup(backupObject: BackupObject<*>?, onClick: OnClick) {
        post {
            noCrash {
                if (Network.isConnected) {
                    backup?.isEnabled = true
                    if (backupObject == null)
                        date.text = "Sin respaldo"
                    else {
                        date.text = backupObject.date
                        restore?.isEnabled = true
                    }
                    backup?.setOnClickListener {
                        noCrash {
                            onClick.onAction(this@SyncItemView, actionId, true)
                            AchievementManager.onBackup()
                        }
                    }
                    restore?.setOnClickListener { noCrash { onClick.onAction(this@SyncItemView, actionId, false) } }
                } else {
                    date.text = "Sin internet"
                }
            }
        }

    }

    fun clear() {
        bakup = null
        post {
            backup?.isEnabled = false
            restore?.isEnabled = false
            date?.text = "Cargando..."
        }
    }

    fun init(onClick: OnClick) {
        BUUtils.search(actionId, object : BUUtils.SearchInterface {
            override fun onResponse(backupObject: BackupObject<*>?) {
                bakup = backupObject
                enableBackup(bakup, onClick)
            }
        })
    }

    interface OnClick {
        fun onAction(syncItemView: SyncItemView, id: String, isBackup: Boolean)
    }
}
