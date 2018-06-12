package knf.kuma.tv.exoplayer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import knf.kuma.tv.TVBaseActivity;

public class TVPlayer extends TVBaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addFragment(PlaybackFragment.get(getIntent().getExtras()));
    }
}
