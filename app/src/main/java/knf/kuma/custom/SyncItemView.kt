package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.Backups
import knf.kuma.backup.framework.BackupService
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.Network
import knf.kuma.commons.noCrash
import knf.kuma.databinding.SyncItemLayoutBinding
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import xdroid.toaster.Toaster

class SyncItemView : RelativeLayout {

    private var cardTitle: String? = "Error"
    private var showDivider = true
    private var hideBackup = false
    private var actionId: String = "neutral"

    var backupObj: BackupObject<*>? = null
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

    private lateinit var binding: SyncItemLayoutBinding
    private fun inflate(context: Context) {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.sync_item_layout, this)
        binding = SyncItemLayoutBinding.bind(this)
    }

    private fun setDefaults(context: Context, attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.SyncItemView)
        cardTitle = array.getString(R.styleable.SyncItemView_si_title)
        showDivider = array.getBoolean(R.styleable.SyncItemView_si_showDivider, true)
        hideBackup = array.getBoolean(R.styleable.SyncItemView_si_hideBackup, false)
        actionId = array.getString(R.styleable.SyncItemView_si_actionId) ?: "neutral"
        array.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.title.text = cardTitle
        if (!showDivider)
            binding.separator.visibility = View.GONE
        if (hideBackup)
            binding.backup.isEnabled = false
    }

    fun enableBackup(backupObject: BackupObject<*>?, onClick: OnClick) {
        post {
            noCrash {
                if (Network.isConnected) {
                    if (!hideBackup)
                        binding.backup.isEnabled = true
                    if (backupObject == null)
                        binding.date.text = "Sin respaldo"
                    else {
                        binding.date.text = backupObject.date
                        binding.restore.isEnabled = true
                    }
                    binding.backup.onLongClick(returnValue = true) { Toaster.toast("Respaldar a la nube") }
                    binding.backup.setOnClickListener {
                        noCrash {
                            onClick.onAction(this@SyncItemView, actionId, true)
                            AchievementManager.onBackup()
                        }
                    }
                    binding.restore.onLongClick(returnValue = true) { Toaster.toast("Restaurar desde la nube") }
                    binding.restore.setOnClickListener { noCrash { onClick.onAction(this@SyncItemView, actionId, false) } }
                } else {
                    binding.date.text = "Sin internet"
                }
            }
        }
    }

    fun clear() {
        backupObj = null
        post {
            binding.backup.isEnabled = false
            binding.restore.isEnabled = false
            binding.date.text = "Cargando..."
        }
    }

    fun init(service: BackupService?, onClick: OnClick) {
        Backups.search(service, actionId) {
            backupObj = it
            enableBackup(backupObj, onClick)
        }
    }

    interface OnClick {
        fun onAction(syncItemView: SyncItemView, id: String, isBackup: Boolean)
    }
}
