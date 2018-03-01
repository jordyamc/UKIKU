package knf.kuma;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import knf.kuma.downloadservice.FileAccessHelper;
import xdroid.toaster.Toaster;

public abstract class BottomFragment extends Fragment {
    public abstract void onReselect();

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode== FileAccessHelper.SD_REQUEST&&resultCode== Activity.RESULT_OK){
            if (!FileAccessHelper.INSTANCE.isUriValid(data.getData())) {
                Toaster.toast("Directorio invalido");
                FileAccessHelper.openTreeChooser(this);
            }
        }
    }
}
