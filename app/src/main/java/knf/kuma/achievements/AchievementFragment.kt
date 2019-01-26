package knf.kuma.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.database.CacheDB
import knf.kuma.pojos.Achievement
import org.jetbrains.anko.find

class AchievementFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var error: View
    private lateinit var errorText: TextView
    private lateinit var onClick: OnClick
    private val adapter = AchievementAdapter {
        onClick.invoke(it)
    }
    private var isFirst = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        CacheDB.INSTANCE.achievementsDAO().achievementList(arguments?.getInt(isUnlockedKey, 0) ?: 0)
                .observe(this, Observer {
                    adapter.setAchievements(it)
                    error.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    if (isFirst) {
                        recyclerView.scheduleLayoutAnimation()
                        isFirst = false
                    }
                })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_achievements, container, false).also {
            error = it.find(R.id.error)
            errorText = it.find(R.id.error_text)
            errorText.text = if (arguments?.getInt(isUnlockedKey, 0) == 0) "Has completado todos los logros" else "No has completado ningun logro"
            recyclerView = it.find(R.id.recycler)
            recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
            recyclerView.adapter = adapter
        }
    }

    fun setCallback(onClick: OnClick) {
        this.onClick = onClick
    }

    companion object {
        private const val isUnlockedKey = "isUnlocked"

        fun get(isUnlocked: Int, onClick: OnClick): AchievementFragment {
            val achievementFragment = AchievementFragment()
            val bundle = Bundle().apply {
                this.putInt(isUnlockedKey, isUnlocked)
            }
            achievementFragment.arguments = bundle
            achievementFragment.setCallback(onClick)
            return achievementFragment
        }
    }
}

typealias OnClick = (achievement: Achievement) -> Unit