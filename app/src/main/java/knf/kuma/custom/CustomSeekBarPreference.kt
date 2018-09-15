package knf.kuma.custom

import android.content.Context
import android.preference.Preference
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R

class CustomSeekBarPreference : Preference, SeekBar.OnSeekBarChangeListener {

    @BindView(R.id.custom_title)
    lateinit var title: TextView
    @BindView(R.id.seek)
    lateinit var seekBar: SeekBar
    @BindView(R.id.value)
    lateinit var value: TextView
    private var MAX_VALUE = 100
    private var MIN_VALUE = 0
    private var INIT_VALUE = 0
    private val STEP_VALUE = 1
    private var TEXT_MASK: String? = "%d"
    private var TEXT_ZERO: String? = "-"


    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context) : super(context) {
        init(null)
    }

    private fun init(attrs: AttributeSet?) {
        layoutResource = R.layout.custom_seekbar_preference
        this.setDefValues(attrs!!)
    }

    private fun setDefValues(defValues: AttributeSet) {
        val array = context!!.obtainStyledAttributes(defValues, R.styleable.CustomSeekBarPreference)
        MAX_VALUE = array.getInt(R.styleable.CustomSeekBarPreference_cs_max, 100)
        MIN_VALUE = array.getInt(R.styleable.CustomSeekBarPreference_cs_min, 0)
        TEXT_MASK = array.getString(R.styleable.CustomSeekBarPreference_cs_mask)
        TEXT_ZERO = array.getString(R.styleable.CustomSeekBarPreference_cs_zero_value)
        array.recycle()
        INIT_VALUE = getPersistedInt(PreferenceManager.getDefaultSharedPreferences(context).getInt(key, 0))
    }

    override fun onCreateView(parent: ViewGroup): View {
        super.onCreateView(parent)
        val view = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.custom_seekbar_preference, parent, false)
        ButterKnife.bind(this, view)
        title.text = getTitle()
        seekBar.max = MAX_VALUE
        seekBar.progress = INIT_VALUE
        seekBar.incrementProgressBy(STEP_VALUE)
        if (INIT_VALUE == 0)
            value.text = TEXT_ZERO
        else
            value.text = String.format(TEXT_MASK!!, INIT_VALUE)
        seekBar.setOnSeekBarChangeListener(this)
        return view
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (progress == 0) {
            value.text = TEXT_ZERO
        } else {
            value.text = String.format(TEXT_MASK!!, progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        persistInt(seekBar.progress)
        callChangeListener(seekBar.progress)
    }
}
