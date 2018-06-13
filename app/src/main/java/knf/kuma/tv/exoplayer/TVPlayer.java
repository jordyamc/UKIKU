package knf.kuma.tv.exoplayer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import knf.kuma.tv.TVBaseActivity;

public class TVPlayer extends TVBaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        addFragment(PlaybackFragment.get(getIntent().getExtras()));
    }
}
