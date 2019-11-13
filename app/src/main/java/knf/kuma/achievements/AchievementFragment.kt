package knf.kuma.achievements

import android.graphics.Color
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
import kotlinx.android.synthetic.main.fragment_achievements.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import org.jetbrains.anko.find


class AchievementFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var error: View
    private lateinit var errorText: TextView
    private lateinit var onClick: OnClick
    private val adapter = AchievementAdapter {
        onClick.invoke(it)
    }
    private val isLockedScreen: Boolean by lazy { arguments?.getInt(isUnlockedKey, 0) ?: 0 == 0 }
    private var isListEmpty: Boolean = false
    private var isFirst = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        CacheDB.INSTANCE.achievementsDAO().achievementList(arguments?.getInt(isUnlockedKey, 0) ?: 0)
                .observe(viewLifecycleOwner, Observer {
                    isListEmpty = it.isEmpty()
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

    override fun onResume() {
        super.onResume()
        if (isLockedScreen && isListEmpty)
            konfetti.build().apply {
                addColors(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN, Color.MAGENTA)
                setDirection(0.0, 359.0)
                setSpeed(4f, 7f)
                setFadeOutEnabled(true)
                setTimeToLive(2000)
                addShapes(Shape.RECT, Shape.CIRCLE)
                addSizes(Size(12, 6f), Size(16, 6f))
                setPosition(-50f, konfetti.width + 50f, -50f, -50f)
            }.streamFor(200, 10000L)
    }

    override fun onPause() {
        super.onPause()
        if (isLockedScreen && isListEmpty)
            konfetti.reset()
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