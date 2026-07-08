package knf.kuma.seeing

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.verifyManager
import knf.kuma.database.CacheDB
import knf.kuma.databinding.FragmentSeeingBinding
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.av1.Organizer
import knf.kuma.pojos.av1.OrganizerWRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xdroid.toaster.Toaster

class SeeingFragment : Fragment() {

    var clickCount = 0

    private lateinit var binding: FragmentSeeingBinding
    private val adapter: SeeingAdapter? by lazy { activity?.let { SeeingAdapter(it, arguments?.getInt("state", 0) == 0) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_seeing, container, false).also {
            binding = FragmentSeeingBinding.bind(it)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            binding.adContainer.implBanner(AdsType.SEEING_BANNER, true)
        }
        when (arguments?.getInt("state", 0)) {
            1 -> {
                binding.errorText.text = "No estas viendo ningún anime"
                binding.errorImg.setImageResource(R.drawable.ic_watching)
            }
            2 -> {
                binding.errorText.text = "No consideras ningún anime"
                binding.errorImg.setImageResource(R.drawable.ic_considering)
            }
            3 -> {
                binding.errorText.text = "No has terminado ningún anime"
                binding.errorImg.setImageResource(R.drawable.ic_completed)
            }
            4 -> {
                binding.errorText.text = "No has dropeado ningún anime"
                binding.errorImg.setImageResource(R.drawable.ic_droped)
            }
            5 -> {
                binding.errorText.text = "No tienes pausado ningún anime"
                binding.errorImg.setImageResource(R.drawable.ic_paused)
            }
            else -> binding.errorText.text = "No has marcado ningún anime"
        }
        binding.recycler.verifyManager()
        binding.recycler.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            liveData.collectLatest {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                }
                adapter?.submitData(it)
            }
        }
        adapter?.addLoadStateListener {
            binding.error.isVisible = it.append.endOfPaginationReached && adapter?.itemCount == 0
        }
    }

    val liveData: Flow<PagingData<OrganizerWRecord>>
        get() {
            val state = arguments?.getInt("state", 0) ?: 0
            return Pager(
                PagingConfig(20, enablePlaceholders = false),
                pagingSourceFactory = {
                    if (state == 0){
                        CacheDB.INSTANCE.organizerDAO().allPaging
                    } else {
                        CacheDB.INSTANCE.organizerDAO().getByStatePaging(state)
                    }
                }
            ).flow
        }

    val title: String
        get() {
            return when (arguments?.getInt("state", 0)) {
                1 -> "Viendo"
                2 -> "Considerando"
                3 -> "Completado"
                4 -> "Dropeado"
                5 -> "Pausado"
                else -> "Todos"
            }
        }

    fun onSelected() {
        clickCount++
        if (clickCount == 3) {
            lifecycleScope.launch {
                val state = arguments?.getInt("state", -1) ?: -1
                if (state == -1) return@launch
                val num = if (state == 0)
                    CacheDB.INSTANCE.organizerDAO().countAll
                else
                    CacheDB.INSTANCE.organizerDAO().countByState(state)
                if (num > 0)
                    Toaster.toast("$num anime" + adapter?.let { if (num > 1) "s" else "" })
            }
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