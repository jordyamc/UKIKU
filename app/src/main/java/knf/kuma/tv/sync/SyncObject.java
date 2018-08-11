package knf.kuma.tv.sync;

import androidx.annotation.DrawableRes;
import knf.kuma.R;

public class SyncObject {
    public boolean isDropbox = false;

    SyncObject() {
    }

    public SyncObject(boolean isDropbox) {
        this.isDropbox = isDropbox;
    }

    @DrawableRes
    public int getImage() {
        return isDropbox ? R.drawable.banner_dropbox : R.drawable.banner_drive;
    }

    public String getTitle() {
        return isDropbox ? "DropBox" : "Google Drive";
    }
}
