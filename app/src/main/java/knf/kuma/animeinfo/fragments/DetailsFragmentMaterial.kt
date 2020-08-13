package knf.kuma.animeinfo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.animeinfo.AnimeViewModel
import knf.kuma.animeinfo.viewholders.AnimeDetailsMaterialHolder

class DetailsFragmentMaterial : Fragment() {
    private var holder: AnimeDetailsMaterialHolder? = null
    private val viewModel: AnimeViewModel by activityViewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.liveData.observe(viewLifecycleOwner, Observer { animeObject ->
            if (animeObject != null)
                holder?.populate(this@DetailsFragmentMaterial, animeObject)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_anime_details_material, container, false)
            holder = AnimeDetailsMaterialHolder(view)
            view
        } catch (e: ExceptionInInitializerError) {
            null
        }
    }

    companion object {

        fun get(): DetailsFragmentMaterial {
            return DetailsFragmentMaterial()
        }
    }
}
