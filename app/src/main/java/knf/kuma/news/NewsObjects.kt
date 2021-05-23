package knf.kuma.news

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import knf.kuma.commons.NoSSLOkHttpClient
import knf.kuma.commons.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pl.droidsonroids.jspoon.annotation.Selector
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class NewsItem {
    @Selector("h2 a")
    lateinit var title: String

    @Selector("a.cate-link")
    lateinit var type: String

    @Selector("[data-ttico=date_range]")
    lateinit var date: String

    @Selector("img", attr = "src", defValue = "")
    lateinit var image: String

    @Selector("figure a", attr = "href")
    lateinit var link: String

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<NewsItem>() {
            override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean =
                    oldItem.link == newItem.link

            override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean =
                    oldItem.title == newItem.title && oldItem.date == newItem.date
        }
    }
}

class NewsPage {
    @Selector("article.News.b")
    var newsList: List<NewsItem> = emptyList()
}

class NewsDataSource(private val newsFactory: NewsFactory, val category: String, val onInit: (isEmpty: Boolean, cause: String?) -> Unit) : PagingSource<Int, NewsItem>() {

    override fun getRefreshKey(state: PagingState<Int, NewsItem>): Int? = state.anchorPosition?.let {
        state.closestPageToPosition(it)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NewsItem> {
        val page = params.key?:1
        try {
            val response = withContext(Dispatchers.IO) { newsFactory.getNewsPage(category, page).execute() }
            if (response.isSuccessful){
                response.body()?.let {
                    if (page == 1)
                        onInit(false, null)
                    return LoadResult.Page(it.newsList, null, if (it.newsList.size < 12) null else page + 1)
                } ?: run{
                    if (page == 1)
                        onInit(true, "Empty body")
                }
            }
            val errorString = withContext(Dispatchers.IO) { response.errorBody()?.toString() }
            if (page == 1)
                onInit(true, "$errorString")
            return LoadResult.Error(IllegalStateException())
        }catch (e:Exception){
            if (page == 1)
                onInit(true, "${e.message}")
            return LoadResult.Error(e)
        }
    }
}

interface NewsFactory {
    @GET("{category}/page/{page}/")
    fun getNewsPage(@Path("category") category: String, @Path("page") page: Int): Call<NewsPage>
}

object NewsRepository {
    fun getNews(category: String, onInit: (isEmpty: Boolean, cause: String?) -> Unit): Flow<PagingData<NewsItem>> {
        return Pager(
            config = PagingConfig(12),
            pagingSourceFactory = { NewsDataSource(getFactory(), category, onInit) }
        ).flow
    }

    private fun getFactory(): NewsFactory {
        val retrofit = Retrofit.Builder()
                .baseUrl("https://somoskudasai.com/")
                .client(NoSSLOkHttpClient.get())
                .addConverterFactory(JspoonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        return retrofit.create(NewsFactory::class.java)
    }
}

fun openNews(activity: AppCompatActivity, newsItem: NewsItem) {
    try {
        NewsDialog.show(activity, newsItem.link)
    } catch (e: ActivityNotFoundException) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(newsItem.link)))
        } catch (anfe: ActivityNotFoundException) {
            "No se encontró ningun navegador para abrir noticia".toast()
        }
    } catch (ex: Exception) {
        "Error al abrir noticia".toast()
    }
}