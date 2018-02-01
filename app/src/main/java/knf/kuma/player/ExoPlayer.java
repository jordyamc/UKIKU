package knf.kuma.player;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import knf.kuma.R;

/**
 * Created by Jordy on 10/01/2018.
 */

public class ExoPlayer extends AppCompatActivity {
    @BindView(R.id.player)
    SimpleExoPlayerView exoPlayerView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.pip)
    ImageButton pip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exo_player);
        ButterKnife.bind(this);
        title.setText(getIntent().getStringExtra("title"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
            pip.setVisibility(View.VISIBLE);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);
        MediaSource source;
        if (!getIntent().getBooleanExtra("isFile", false)) {
            source = new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "UKIKU"))).createMediaSource(getIntent().getData());
        } else {
            source = new ExtractorMediaSource.Factory(new FileDataSourceFactory()).createMediaSource(getIntent().getData());
        }
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        exoPlayerView.setPlayer(player);
        player.setPlayWhenReady(true);
        player.prepare(source);
    }

    @OnClick(R.id.pip)
    @TargetApi(Build.VERSION_CODES.N)
    void onPip(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInPictureInPictureMode())
                    enterPictureInPictureMode();
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode)
            getApplication().startActivity(new Intent(this, getClass())
                    .putExtra("isReorder", true)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (exoPlayerView != null && !intent.getBooleanExtra("isReorder", false)) {
            exoPlayerView.getPlayer().stop();
            title.setText(intent.getStringExtra("title"));
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector =
                    new DefaultTrackSelector(videoTrackSelectionFactory);
            MediaSource source;
            if (!intent.getBooleanExtra("isFile", false)) {
                source = new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "UKIKU"))).createMediaSource(intent.getData());
            } else {
                source = new ExtractorMediaSource.Factory(new FileDataSourceFactory()).createMediaSource(intent.getData());
            }
            SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            exoPlayerView.setPlayer(player);
            player.setPlayWhenReady(true);
            player.prepare(source);
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (exoPlayerView != null) {
            exoPlayerView.getPlayer().stop();
            exoPlayerView.getPlayer().release();
        }
        super.onDestroy();
    }


}
