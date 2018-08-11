package knf.kuma.tv.details;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import knf.kuma.R;
import knf.kuma.tv.TVBaseActivity;
import knf.kuma.tv.TVServersFactory;

public class TVAnimesDetails extends TVBaseActivity implements TVServersFactory.ServersInterface {
    private TVAnimesDetailsFragment fragment;
    private TVServersFactory serversFactory;

    public static void start(Context context, String url) {
        context.startActivity(new Intent(context, TVAnimesDetails.class).putExtra("url", url));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment = TVAnimesDetailsFragment.get(getIntent().getStringExtra("url"));
        addFragment(fragment);
    }

    @Override
    public void onReady(TVServersFactory serversFactory) {
        this.serversFactory = serversFactory;
    }

    @Override
    public void onFinish(boolean started, boolean success) {
        if (fragment != null && success) {
            fragment.onStartStreaming();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (serversFactory.getViewHolder() != null) {
                    serversFactory.getViewHolder().view.findViewById(R.id.indicator).setVisibility(View.VISIBLE);
                    serversFactory.getViewHolder().view.invalidate();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
