package knf.kuma.custom

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import knf.kuma.R
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUI
import knf.kuma.commons.inflate
import knf.kuma.home.UpdateableAdapter
import org.jetbrains.anko.sdk27.coroutines.onClick

class HomeList : LinearLayout {

    private var showAll = false
    private var showAllText = "Ver Todos"
    private var isLarge = false
    private var startHidden = false
    private var showError = false
    private var subheaderText = "Subheader"
    private var errorText: String? = null

    private var isHidden = false

    private val subheader: TextView by bind(R.id.subheader)
    private val viewAll: MaterialButton by bind(R.id.viewAll)
    private val progress: ProgressBar by bind(R.id.progress)
    private val errorTV: TextView by bind(R.id.errorTV)
    private val recyclerView: RecyclerView by bind(R.id.recycler)

    private var adapter: UpdateableAdapter<*>? = null

    constructor(context: Context) : super(context) {
        inflate()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(attrs)
    }

    private fun loadVars(attrs: AttributeSet?) {
        attrs?.let {
            val array = context.obtainStyledAttributes(it, R.styleable.HomeList)
            showAll = array.getBoolean(R.styleable.HomeList_hm_showViewAll, false)
            showAllText = array.getString(R.styleable.HomeList_hm_viewAllText) ?: "Ver Todos"
            isLarge = array.getBoolean(R.styleable.HomeList_hm_isLarge, false)
            startHidden = array.getBoolean(R.styleable.HomeList_hm_startHidden, false)
            subheaderText = array.getString(R.styleable.HomeList_hm_subheader) ?: "Subheader"
            errorText = array.getString(R.styleable.HomeList_hm_errorText)
            showError = !errorText.isNullOrEmpty()
            array.recycle()
        }
    }

    private fun inflate(attrs: AttributeSet? = null) {
        loadVars(attrs)
        this.inflate(if (isLarge) R.layout.view_home_list_large else R.layout.view_home_list, true)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (startHidden) visibility = View.GONE
        viewAll.text = showAllText
        subheader.text = subheaderText
        errorTV.text = errorText
        if (showAll) viewAll.visibility = View.VISIBLE
    }

    fun hide() {
        doOnUI {
            isHidden = true
            visibility = View.GONE
        }
    }

    fun show() {
        doOnUI {
            isHidden = false
            visibility = View.VISIBLE
        }
    }

    fun setSubheader(text: String) {
        doOnUI { subheader.text = text.also { subheaderText = it } }
    }

    fun <T> setViewAllClass(clazz: Class<T>) {
        viewAll.onClick { context.startActivity(Intent(context, clazz)) }
    }

    fun setViewAllOnClick(func: () -> Unit) {
        viewAll.onClick { func() }
    }

    fun setAdapter(adapter: UpdateableAdapter<*>) {
        doOnUI {
            recyclerView.adapter = adapter.also { this.adapter = it }
        }
    }

    fun updateList(list: List<Any>) {
        doOnUI {
            if (showError)
                errorTV.visibility = if (list.isEmpty())
                    View.VISIBLE
                else
                    View.GONE
            else
                visibility = if (list.isNotEmpty() && !isHidden)
                    View.VISIBLE
                else
                    View.GONE
            progress.visibility = View.GONE
            adapter?.updateList(list)
        }
    }
}
