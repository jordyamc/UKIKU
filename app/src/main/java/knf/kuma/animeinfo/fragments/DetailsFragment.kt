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
import xdroid.toaster.Toaster

class DetailsFragment : Fragment() {
    private var holder: AnimeDetailsHolder? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ViewModelProviders.of(activity!!).get(AnimeViewModel::class.java).liveData!!.observe(this, Observer { animeObject ->
            if (animeObject != null)
                holder!!.populate(this@DetailsFragment, animeObject)
            else {
                Toaster.toast("No se pudo obtener la informacion")
                activity!!.finish()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_anime_details, container, false)
        holder = AnimeDetailsHolder(view)
        return view
    }

    companion object {

        fun get(): DetailsFragment {
            return DetailsFragment()
        }
    }
}
