package knf.kuma.tv.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import androidx.annotation.Nullable;
import knf.kuma.backup.BUUtils;
import knf.kuma.directory.DirectoryService;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.recents.RecentsNotReceiver;
import knf.kuma.tv.TVBaseActivity;
import knf.kuma.tv.TVServersFactory;
import knf.kuma.updater.UpdateActivity;
import knf.kuma.updater.Updatechecker;

public class TVMain extends TVBaseActivity implements TVServersFactory.ServersInterface, Updatechecker.CheckListener {

    private TVMainFragment fragment;
    private TVServersFactory serversFactory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment = TVMainFragment.get();
        addFragment(fragment);
        DirectoryService.run(this);
        RecentsJob.schedule(this);
        DirUpdateJob.schedule(this);
        RecentsNotReceiver.removeAll(this);
        Updatechecker.check(this, this);
        Answers.getInstance().logCustom(new CustomEvent("TV UI"));
    }

    @Override
    public void onNeedUpdate(String o_code, String n_code) {
        runOnUiThread(() -> {
            try {
                new MaterialDialog.Builder(TVMain.this)
                        .title("Actualización")
                        .content("Parece que la versión " + n_code + " está disponible, ¿Quieres actualizar?")
                        .positiveText("si")
                        .negativeText("despues")
                        .onPositive((dialog, which) -> UpdateActivity.start(TVMain.this)).build().show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onReady(TVServersFactory serversFactory) {
        this.serversFactory = serversFactory;
    }

    @Override
    public void onFinish(boolean started, boolean success) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null)
            if (requestCode == BUUtils.LOGIN_CODE) {
                if (resultCode == RESULT_OK) {
                    GoogleSignIn.getSignedInAccountFromIntent(data);
                    BUUtils.setType(BUUtils.BUType.DRIVE);
                    BUUtils.setDriveClient();
                } else if (fragment != null) {
                    fragment.onLogin();
                }
            } else if (resultCode == Activity.RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle != null)
                    if (bundle.getBoolean("is_video_server", false))
                        serversFactory.analizeOption(bundle.getInt("position", 0));
                    else
                        serversFactory.analizeServer(bundle.getInt("position", 0));
            } else if (resultCode == Activity.RESULT_CANCELED && data.getExtras().getBoolean("is_video_server", false))
                serversFactory.showServerList();
    }

}
