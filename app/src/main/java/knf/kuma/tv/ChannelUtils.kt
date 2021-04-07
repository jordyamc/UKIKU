package knf.kuma.tv

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.urlFixed
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecentObject
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ChannelUtils {
    fun createIfNeeded(context: Context) {
        if (!context.resources.getBoolean(R.bool.isTv)) return
        if (!PrefsUtil.tvRecentsChannelCreated) {
            GlobalScope.launch(Dispatchers.IO) {
                val channelBuilder = PreviewChannel.Builder()
                    .setDisplayName("Episodios recientes")
                    .setAppLinkIntentUri(Uri.parse("ukiku://tv/home"))
                    .setLogo(
                        BitmapFactory.decodeResource(
                            context.resources,
                            R.drawable.ukiku_logo_plain
                        )
                    )
                val channelId =
                    PreviewChannelHelper(context).publishDefaultChannel(channelBuilder.build())
                PrefsUtil.tvRecentsChannelCreated = true
                PrefsUtil.tvRecentsChannelId = channelId
            }
        }
        initChannelIfNeeded(context)
    }

    fun initChannelIfNeeded(context: Context) {
        if (!context.resources.getBoolean(R.bool.isTv)) return
        if (!PrefsUtil.tvRecentsPreFilled)
            GlobalScope.launch(Dispatchers.IO) {
                delay(5000)
                val recents = CacheDB.INSTANCE.recentsDAO().allSimple
                if (recents.isNotEmpty()) {
                    PrefsUtil.tvRecentsPreFilled = true
                    val programIds = mutableSetOf<String>()
                    recents.forEach {
                        val chapUri = Uri.Builder().scheme("ukiku")
                            .authority("tv")
                            .appendPath("chapter")
                            .appendQueryParameter("aid", it.aid)
                            .appendQueryParameter("chapter", it.chapter)
                            .appendQueryParameter("eid", it.eid)
                            .appendQueryParameter("url", it.url.urlFixed)
                            .appendQueryParameter("name", it.name)
                            .build()
                        val program = PreviewProgram.Builder()
                            .setChannelId(PrefsUtil.tvRecentsChannelId)
                            .setType(TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE)
                            .setTitle(it.name)
                            .setEpisodeNumber(it.chapter.substringAfterLast(" ").toInt())
                            .setIntentUri(chapUri)
                            .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_3_2)
                            .setPosterArtUri(Uri.parse(PatternUtil.getThumb(it.aid)))
                        programIds.add(
                            PreviewChannelHelper(context).publishPreviewProgram(program.build())
                                .toString()
                        )
                    }
                    PrefsUtil.tvRecentsChannelLastEid = recents.first().eid
                    PrefsUtil.tvRecentsChannelIds = programIds
                } else
                    Repository().reloadAllRecents()
            }
    }

    fun addProgram(context: Context, recentObject: RecentObject): Long {
        val chapUri = Uri.Builder().scheme("ukiku")
            .authority("tv")
            .appendPath("chapter")
            .appendQueryParameter("aid", recentObject.aid)
            .appendQueryParameter("chapter", recentObject.chapter)
            .appendQueryParameter("eid", recentObject.eid)
            .appendQueryParameter("url", recentObject.url.urlFixed)
            .appendQueryParameter("name", recentObject.name)
            .build()
        val program = PreviewProgram.Builder()
            .setContentId(recentObject.eid)
            .setChannelId(PrefsUtil.tvRecentsChannelId)
            .setType(TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE)
            .setTitle(recentObject.name)
            .setEpisodeNumber(recentObject.chapter.substringAfterLast(" ").toInt())
            .setIntentUri(chapUri)
            .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_3_2)
            .setPosterArtUri(Uri.parse(PatternUtil.getThumb(recentObject.aid)))
            .setWeight(999)
        return PreviewChannelHelper(context).publishPreviewProgram(program.build())
    }
}