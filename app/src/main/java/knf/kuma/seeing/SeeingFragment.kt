package knf.kuma.seeing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject
import kotlinx.android.synthetic.main.fragment_seeing.*
import xdroid.toaster.Toaster

class SeeingFragment : Fragment() {

    var clickCount = 0

    private val adapter: SeeingAdapter by lazy { SeeingAdapter(activity!!, arguments?.getInt("state", 0) == 0) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        liveData.observe(this, Observer {
            error.visibility = if (it?.isEmpty() == true) View.VISIBLE else View.GONE
            adapter.update(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_seeing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        error_text.text = when (arguments?.getInt("state", 0)) {
            1 -> "No estas viendo ningun anime"
            2 -> "No consideras ningun anime"
            3 -> "No has terminado ningun anime"
            4 -> "No has dropeado ningun anime"
            else -> "No has marcado ningun anime"
        }
        recycler.adapter = adapter
    }

    val liveData: LiveData<List<SeeingObject>>
        get() {
            return if (arguments?.getInt("state", 0) ?: 0 == 0)
                CacheDB.INSTANCE.seeingDAO().all
            else
                CacheDB.INSTANCE.seeingDAO().getLiveByState(arguments?.getInt("state", 0) ?: 0)
        }

    val title: String
        get() {
            return when (arguments?.getInt("state", 0)) {
                1 -> "Viendo"
                2 -> "Considerando"
                3 -> "Completado"
                4 -> "Dropeado"
                else -> "Todos"
            }
        }

    fun onSelected() {
        clickCount++
        if (clickCount == 3 && adapter.list.isNotEmpty()) {
            Toaster.toast("${adapter.list.size} anime" + if (adapter.list.size > 1) "s" else "")
            clickCount = 0
        }
    }

    companion object {
        operator fun get(state: Int): SeeingFragment {
            val emissionFragment = SeeingFragment()
            val bundle = Bundle()
            bundle.putInt("state", state)
            emissionFragment.arguments = bundle
            return emissionFragment
        }
    }
}