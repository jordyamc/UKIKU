package knf.kuma.animeinfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import knf.kuma.database.CacheDB

class AnimeBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val animeObject = CacheDB.INSTANCE.animeDAO().getByAid(intent.getStringExtra("aid"))
        if (animeObject != null)
            ActivityAnime.open(context, animeObject)
    }
}
