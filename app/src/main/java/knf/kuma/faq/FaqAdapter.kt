package knf.kuma.faq

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.florent37.expansionpanel.ExpansionLayout
import com.github.florent37.expansionpanel.viewgroup.ExpansionLayoutCollection
import knf.kuma.R
import knf.kuma.commons.inflate
import kotlinx.android.synthetic.main.item_faq.view.*

class FaqAdapter(private val list: List<FaqItem>) : RecyclerView.Adapter<FaqAdapter.ItemHolder>() {

    private val expansionCollection = ExpansionLayoutCollection().apply {
        openOnlyOne(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(parent.inflate(R.layout.item_faq))

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val item = list[position]
        holder.apply {
            question.text = item.question
            answer.text = item.answer
            expansionLayout.let {
                it.collapse(false)
                expansionCollection.add(it)
            }
        }
    }

    class ItemHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val question: TextView by lazy { view.question }
        val answer: TextView by lazy { view.answer }
        val expansionLayout: ExpansionLayout by lazy { view.expansionLayout }
    }
}