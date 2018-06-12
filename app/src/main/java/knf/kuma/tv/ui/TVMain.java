package knf.kuma.tv.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;

import knf.kuma.backup.BUUtils;
import knf.kuma.directory.DirectoryService;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.recents.RecentsNotReceiver;
import knf.kuma.tv.TVBaseActivity;
import knf.kuma.tv.TVServersFactory;

public class TVMain extends TVBaseActivity implements TVServersFactory.ServersInterface {

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
        if (requestCode == BUUtils.LOGIN_CODE) {
            Log.e("Result", "Code: " + resultCode);
            if (resultCode == RESULT_OK) {
                GoogleSignIn.getSignedInAccountFromIntent(data);
                BUUtils.setType(BUUtils.BUType.DRIVE);
                BUUtils.setDriveClient();
            } else if (fragment != null) {
                fragment.onLogin();
            }
        } else {
            if (resultCode == Activity.RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle.getBoolean("is_video_server", false))
                    serversFactory.analizeOption(bundle.getInt("position", 0));
                else
                    serversFactory.analizeServer(bundle.getInt("position", 0));
            } else if (resultCode == Activity.RESULT_CANCELED && data.getExtras().getBoolean("is_video_server", false))
                serversFactory.showServerList();
        }
    }
}
