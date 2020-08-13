package knf.kuma.news

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import knf.kuma.commons.NoSSLOkHttpClient
import knf.kuma.commons.toast
import knf.kuma.custom.BackgroundExecutor
import knf.kuma.custom.MainExecutor
import pl.droidsonroids.jspoon.annotation.Selector
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    @Selector("img", attr = "src")
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

class NewsDataSource(private val newsFactory: NewsFactory, val category: String, val onInit: (isEmpty: Boolean) -> Unit) : PageKeyedDataSource<Int, NewsItem>() {
    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, NewsItem>) {
        newsFactory.getNewsPage(category, 1).enqueue(object : Callback<NewsPage> {
            override fun onFailure(call: Call<NewsPage>, t: Throwable) {
                onInit(true)
            }

            override fun onResponse(call: Call<NewsPage>, response: Response<NewsPage>) {
                response.body()?.let {
                    callback.onResult(it.newsList, null, 2)
                    onInit(false)
                } ?: onInit(true)
            }
        })
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, NewsItem>) {
        newsFactory.getNewsPage(category, params.key).enqueue(object : Callback<NewsPage> {
            override fun onFailure(call: Call<NewsPage>, t: Throwable) {
            }

            override fun onResponse(call: Call<NewsPage>, response: Response<NewsPage>) {
                response.body()?.let {
                    callback.onResult(it.newsList, if (it.newsList.size < 12) null else params.key + 1)
                }
            }
        })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, NewsItem>) {

    }
}

interface NewsFactory {
    @GET("{category}/page/{page}/")
    fun getNewsPage(@Path("category") category: String, @Path("page") page: Int): Call<NewsPage>
}

object NewsRepository {
    fun getNews(category: String, onInit: (isEmpty: Boolean) -> Unit): PagedList<NewsItem> {
        return PagedList.Builder<Int, NewsItem>(NewsDataSource(getFactory(), category, onInit), 12).apply {
            setFetchExecutor(BackgroundExecutor())
            setNotifyExecutor(MainExecutor())
        }.build()
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