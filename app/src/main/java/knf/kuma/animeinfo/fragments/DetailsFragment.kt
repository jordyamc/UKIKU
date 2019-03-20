package knf.kuma.animeinfo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import knf.kuma.R
import knf.kuma.animeinfo.AnimeViewModel
import knf.kuma.animeinfo.viewholders.AnimeDetailsHolder

class DetailsFragment : Fragment() {
    private var holder: AnimeDetailsHolder? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            ViewModelProviders.of(it).get(AnimeViewModel::class.java).liveData.observe(this, Observer { animeObject ->
                if (animeObject != null)
                    holder?.populate(this@DetailsFragment, animeObject)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_anime_details, container, false)
            holder = AnimeDetailsHolder(view)
            view
        } catch (e: ExceptionInInitializerError) {
            null
        }
    }

    companion object {

        fun get(): DetailsFragment {
            return DetailsFragment()
        }
    }
}
