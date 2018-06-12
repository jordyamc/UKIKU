package knf.kuma.tv.sync;

import knf.kuma.R;

public class LogOutObject extends SyncObject {
    public LogOutObject() {
    }

    @Override
    public int getImage() {
        return R.drawable.banner_signout;
    }

    @Override
    public String getTitle() {
        return "Cerrar sesión";
    }
}
