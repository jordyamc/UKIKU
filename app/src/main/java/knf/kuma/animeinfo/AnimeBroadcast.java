package knf.kuma.animeinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;

/**
 * Created by Jordy on 09/01/2018.
 */

public class AnimeBroadcast extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        AnimeObject object= CacheDB.INSTANCE.animeDAO().getByAid(intent.getStringExtra("aid"));
        if (object!=null)
            ActivityAnime.open(context,object);
    }
}
