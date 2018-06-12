package knf.kuma.tv.exoplayer;

import android.net.Uri;
import android.os.Bundle;

public class Video {
    Uri uri;
    String title;
    String chapter;

    public Video(Bundle bundle) {
        this.uri = Uri.parse(bundle.getString("url"));
        this.title = bundle.getString("title");
        this.chapter = bundle.getString("chapter");
    }
}
