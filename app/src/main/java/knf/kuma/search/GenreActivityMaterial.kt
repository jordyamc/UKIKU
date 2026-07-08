package knf.kuma.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.setSurfaceBars
import knf.kuma.commons.verifyManager
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.VariantLinearLayoutManager
import knf.kuma.databinding.RecyclerGenreMaterialBinding
import knf.kuma.directory.DirectoryAV1PageAdapter
import knf.kuma.pojos.av1.Genre
import knf.kuma.pojos.av1.SearchDataSource
import knf.kuma.pojos.av1.SearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GenreActivityMaterial : GenericActivity() {
    private val adapter: DirectoryAV1PageAdapter by lazy { DirectoryAV1PageAdapter(this) }
    private var isFirst = true
    private val binding by lazy { RecyclerGenreMaterialBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(binding.root)
        binding.toolbar.title = intent.getStringExtra("name")
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
        if (PrefsUtil.layType == "0") {
            binding.recycler.layoutManager = VariantLinearLayoutManager(this)
        }
        binding.recycler.verifyManager()
        binding.recycler.adapter = adapter
        lifecycleScope.launch {
            Pager(
                config = PagingConfig(20, enablePlaceholders = false),
                pagingSourceFactory = {
                    SearchDataSource(
                        SearchState(
                            query = null,
                            genres = listOf(intent.getStringExtra("slug")!!)
                        )
                    )
                }
            ).flow.collectLatest {
                adapter.submitData(it)
                withContext(Dispatchers.Main) {
                    if (isFirst) {
                        isFirst = false
                        binding.recycler.scheduleLayoutAnimation()
                    }
                }
            }
        }
    }

    companion object {

        fun open(context: Context, genre: Genre) {
            val intent = Intent(context, GenreActivityMaterial::class.java)
            intent.putExtra("name", genre.name)
            intent.putExtra("slug", genre.slug)
            context.startActivity(intent)
        }
    }
}
