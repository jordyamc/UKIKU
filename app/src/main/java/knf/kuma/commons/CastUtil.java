package knf.kuma.commons;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.afollestad.materialdialogs.MaterialDialog;

import es.munix.multidisplaycast.CastControlsActivity;
import es.munix.multidisplaycast.CastManager;
import es.munix.multidisplaycast.interfaces.CastListener;
import es.munix.multidisplaycast.interfaces.PlayStatusListener;
import xdroid.toaster.Toaster;

/**
 * Created by Jordy on 25/01/2018.
 */

public class CastUtil implements CastListener, PlayStatusListener {
    public static String NO_PLAYING = "no_play";
    private static CastUtil ourInstance;
    private Context context;
    private MutableLiveData<String> castingEid = new MutableLiveData<>();
    private boolean isConnected = false;

    private MaterialDialog loading;

    private CastUtil(Context context) {
        this.context=context;
        CastManager.getInstance().setDiscoveryManager();
        CastManager.getInstance().setPlayStatusListener(getClass().getSimpleName(),this);
        CastManager.getInstance().setCastListener(getClass().getSimpleName(),this);
        castingEid.setValue(NO_PLAYING);
    }

    public static void init(Context context) {
        ourInstance = new CastUtil(context);
    }

    public static CastUtil get() {
        return ourInstance;
    }

    public boolean connected() {
        return isConnected;
    }

    public MutableLiveData<String> getCasting(){
        return castingEid;
    }

    public void play(Activity activity,String eid, String url, String title, String chapter, String preview, boolean isAid){
        if (connected()){
            startLoading(activity);
            setEid(eid);
            if (isAid)
                preview="https://animeflv.net/uploads/animes/thumbs/"+preview+".jpg";
            CastManager.getInstance().playMedia(url,"video/mp4",title,chapter,preview);
        }else {
            Toaster.toast("No hay dispositivo seleccionado");
        }
    }

    public void onDestroy() {
        CastManager.getInstance().unsetCastListener(getClass().getSimpleName());
        CastManager.getInstance().unsetPlayStatusListener(getClass().getSimpleName());
        CastManager.getInstance().onDestroy();
    }

    @Override
    public void isConnected() {
        isConnected = true;
    }

    @Override
    public void isDisconnected() {
        isConnected = false;
    }

    private MaterialDialog getLoading(Activity activity) {
        loading = new MaterialDialog.Builder(activity)
                .content("Cargando...")
                .progress(true, 0)
                .cancelable(false)
                .build();
        return loading;
    }

    private void startLoading(final Activity activity){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                getLoading(activity).show();
            }
        });
    }

    private void stopLoading(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (loading!=null)
                    loading.dismiss();
            }
        });
    }

    private void setEid(final String eid){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                castingEid.setValue(eid);
            }
        });
    }

    public void openControls(){
        context.startActivity(new Intent(context, CastControlsActivity.class));
    }

    @Override
    public void onPlayStatusChanged(int playStatus) {
        switch (playStatus) {
            case STATUS_START_PLAYING:
                stopLoading();
                openControls();
                break;
            case STATUS_FINISHED:
            case STATUS_STOPPED:
                setEid(NO_PLAYING);
                break;
            case STATUS_NOT_SUPPORT_LISTENER:
                stopLoading();
                setEid(NO_PLAYING);
                Toaster.toast("Video no soportado por diapositivo");
                break;
        }
    }

    @Override
    public void onPositionChanged(long currentPosition) {

    }

    @Override
    public void onTotalDurationObtained(long totalDuration) {

    }

    @Override
    public void onSuccessSeek() {

    }
}
