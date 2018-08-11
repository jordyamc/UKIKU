package knf.kuma.pojos;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import knf.kuma.recents.RecentsNotReceiver;

@Entity
public class NotificationObj {
    @Ignore
    public static final int RECENT=0;
    @PrimaryKey
    public int key;
    public int type;

    public NotificationObj(int key, int type) {
        this.key = key;
        this.type = type;
    }

    @NonNull
    public static NotificationObj fromIntent(Intent intent){
        return new NotificationObj(intent.getIntExtra("key",-1),intent.getIntExtra("type",-1));
    }

    public Intent getBroadcast(Context context) {
        return new Intent(context, RecentsNotReceiver.class).putExtra("key", key).putExtra("type", type);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NotificationObj && key==((NotificationObj)obj).key;
    }
}
