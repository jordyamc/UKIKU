package knf.kuma.tv.streaming;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v4.app.FragmentActivity;

public class TVServerSelection extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && savedInstanceState == null)
            if (bundle.containsKey(TVServerSelectionFragment.SERVERS_DATA))
                GuidedStepSupportFragment.addAsRoot(this, TVServerSelectionFragment.get(bundle.getStringArrayList(TVServerSelectionFragment.SERVERS_DATA), bundle.getString("name"), false), android.R.id.content);
            else if (bundle.containsKey(TVServerSelectionFragment.VIDEO_DATA))
                GuidedStepSupportFragment.addAsRoot(this, TVServerSelectionFragment.get(bundle.getStringArrayList(TVServerSelectionFragment.VIDEO_DATA), bundle.getString("name"), true), android.R.id.content);
    }
}
