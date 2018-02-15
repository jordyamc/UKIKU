package es.munix.multidisplaycast;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import es.munix.multidisplaycast.helpers.NotificationsHelper;
import es.munix.multidisplaycast.interfaces.CastListener;
import es.munix.multidisplaycast.interfaces.PlayStatusListener;
import es.munix.multidisplaycast.model.MediaObject;
import es.munix.multidisplaycast.services.AntiLeakActivityService;
import es.munix.multidisplaycast.utils.StorageUtils;


/**
 * Created by munix on 1/11/16.
 */

public class CastManager implements DiscoveryManagerListener, MenuItem.OnMenuItemClickListener, ConnectableDeviceListener, MediaControl.PlayStateListener {

    private static final boolean ENABLE_LOG = true;
    private static final String TAG = "CastInstance";

    private static CastManager instance;
    private Context context;

    //Multimedia
    private DiscoveryManager discoveryManager;
    private MenuItem castMenuItem;
    private ConnectableDevice connectableDevice;
    private HashMap<String, CastListener> castListeners = new HashMap<>();
    private HashMap<String, PlayStatusListener> playStatusListeners = new HashMap<>();
    private MediaControl mMediaControl;
    private Boolean isPaused = false;
    private Boolean statusStartPlayingFired = false;

    //Unset at destroy
    private WeakReference<Activity> activityWeakReference;

    //Dialogos
    private AlertDialog connectToCastDialog;
    private AlertDialog pairingAlertDialog;
    private AlertDialog pairingCodeDialog;
    private AlertDialog disconnectDialog;

    //Listeners no implemetables
    private MediaControl.DurationListener durationListener;
    private MediaControl.PositionListener positionListener;

    //Otros
    private Timer refreshTimer;
    private long totalDuration = -1;
    private long currentPosition = 0;
    private MediaObject mediaObject;


    public static CastManager getInstance() {
        if (instance == null) {
            instance = new CastManager();
        }
        return instance;
    }


    public static void register(Context context) {
        DiscoveryManager.init(context);
        getInstance().setContext(context);
    }

    private void setContext(Context context) {
        this.context = context;
    }

    public void setDiscoveryManager() {
        setDiscoveryManager(new CapabilityFilter[]{});
    }

    //Version con CapabilityFilters
    public void setDiscoveryManager(CapabilityFilter... filters) {
        if (discoveryManager == null) {
            discoveryManager = DiscoveryManager.getInstance();
            DiscoveryManager.getInstance().registerDefaultDeviceTypes();
            if (filters.length > 0)
                discoveryManager.setCapabilityFilters(filters);
            discoveryManager.setPairingLevel(DiscoveryManager.PairingLevel.ON);
            discoveryManager.addListener(this);
            discoveryManager.start();
        } else {
            discoveryManager.addListener(this);
            discoveryManager.start();
            calculateMenuVisibility();
        }
    }

    public void setCastListener(String tag, CastListener listener) {
        this.castListeners.put(tag, listener);
    }

    public void setPlayStatusListener(String tag, PlayStatusListener listener) {
        this.playStatusListeners.put(tag, listener);
    }

    public void unsetCastListener(String tag) {
        this.castListeners.remove(tag);
    }

    public void unsetPlayStatusListener(String tag) {
        this.playStatusListeners.remove(tag);
    }

    public void registerForActivity(Activity activity, Menu menu, int menuId) {
        log("registerForActivity");
        activityWeakReference = new WeakReference<Activity>(activity);
        castMenuItem = menu.findItem(menuId);
        castMenuItem.setIcon(R.drawable.cast_off);
        castMenuItem.setOnMenuItemClickListener(this);
        calculateMenuVisibility();
    }

    private Activity getActivity() {
        return activityWeakReference.get();
    }

    public void setCastMenuVisible(Boolean visible) {
        log("setCastMenuVisible(" + visible + ")");
        if (castMenuItem != null) {
            castMenuItem.setVisible(visible);
        }
    }

    private void log(String log) {
        if (ENABLE_LOG) {
            Log.i(TAG, log);
        }
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        calculateMenuVisibility();
        /*String mRecentDeviceId = StorageUtils.getRecentDeviceId( context );

        if ( mRecentDeviceId != null && connectableDevice == null ) {
            log( "reconnect from previous device" );

            if ( device.getId().equalsIgnoreCase( mRecentDeviceId ) ) {
                log( "onDeviceAdded launch connect for " + mRecentDeviceId );
                device.addListener( this );
                device.connect();
            }
        }*/
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        calculateMenuVisibility();
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        log("onDeviceRemoved");
        calculateMenuVisibility();
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        Log.e(TAG, "onDiscoveryFailed");
        calculateMenuVisibility();
    }

    public void disconnect() {
        stop();
        stopUpdating();
        connectableDevice.disconnect();
        StorageUtils.setRecentDeviceId(context, "");
        castMenuItem.setIcon(R.drawable.cast_off);
    }

    private void showDisconnectAlert(String title, final String disconnectLabel, String image) {
        View customView = View.inflate(getActivity(), R.layout.cast_disconnect, null);
        final TextView deviceName = (TextView) customView.findViewById(R.id.deviceName);
        if (connectableDevice.getFriendlyName() != null) {
            deviceName.setText(connectableDevice.getFriendlyName());
        } else {
            deviceName.setText(connectableDevice.getModelName());
        }

        final TextView mediaTitle = (TextView) customView.findViewById(R.id.mediaTitle);
        mediaTitle.setText(title);

        final ImageView mediaImage = (ImageView) customView.findViewById(R.id.mediaImage);
        if (image != null) {
            Glide.with(getActivity()).load(image).into(mediaImage);
        } else {
            mediaImage.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(customView)
                .setPositiveButton(disconnectLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        disconnect();
                    }
                });
        disconnectDialog = builder.create();
        disconnectDialog.show();
    }

    public MediaObject getMediaObject() {
        return mediaObject;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (isConnected()) {
            if (mMediaControl != null && mediaObject != null) {
                if (!TextUtils.isEmpty(mediaObject.getTitle()) && !TextUtils.isEmpty(mediaObject
                        .getImage())) {
                    showDisconnectAlert(mediaObject.getTitle(), "Dejar de enviar contenido", mediaObject
                            .getImage());
                } else {
                    showDisconnectAlert("No se está reproduciendo contenido", "Desconectar", null);
                }
            } else {
                showDisconnectAlert("No se está reproduciendo contenido", "Desconectar", null);
            }
        } else {
            final DevicePicker devicePicker = new DevicePicker(getActivity());
            connectToCastDialog = devicePicker.getPickerDialog("Selecciona dispositivo", new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    connectToCastDialog.cancel();
                    connectableDevice = (ConnectableDevice) adapterView.getItemAtPosition(i);
                    connectableDevice.addListener(CastManager.this);
                    connectableDevice.connect();
                }
            });
            connectToCastDialog.show();

            pairingAlertDialog = new AlertDialog.Builder(getActivity()).setTitle("Conectando con su TV")
                    .setMessage("Confirme la conexión con su TV")
                    .setPositiveButton("Aceptar", null)
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            devicePicker.cancelPicker();
                            connectToCastDialog.show();
                        }
                    })
                    .create();

            View v = View.inflate(getActivity(), R.layout.input_code_dialog, null);
            final EditText input = (EditText) v.findViewById(R.id.input);
            input.setMaxLines(1);


            final InputMethodManager imm = (InputMethodManager) getActivity().getApplicationContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            pairingCodeDialog = new AlertDialog.Builder(getActivity()).setTitle("Ingrese el código que ve en la TV")
                    .setView(v)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            if (connectableDevice != null) {
                                String value = input.getText().toString().trim();
                                connectableDevice.sendPairingKey(value);
                                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            devicePicker.cancelPicker();
                            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        }
                    })
                    .create();
        }
        return false;
    }


    public void playMedia(final String url, final String mimeType, final String title, final String subtitle, final String icon) {
        if (isConnected()) {

            MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType).setTitle(title)
                    .setDescription(subtitle)
                    .setIcon(icon)
                    .build();

            connectableDevice.getCapability(MediaPlayer.class)
                    .playMedia(mediaInfo, false, new MediaPlayer.LaunchListener() {

                        public void onSuccess(MediaPlayer.MediaLaunchObject object) {

                            mediaObject = new MediaObject(title, subtitle, icon, mimeType, url);
                            if (connectableDevice != null)
                                mediaObject.setCanChangeVolume(connectableDevice.hasCapability(VolumeControl.Volume_Set));
                            if (connectableDevice != null)
                                mediaObject.setCanFastForwart(connectableDevice.hasCapability(MediaControl.FastForward));

                            if (mediaObject.getCanChangeVolume()) {
                                if (connectableDevice != null) {
                                    connectableDevice.getCapability(VolumeControl.class)
                                            .getVolume(new VolumeControl.VolumeListener() {
                                                @Override
                                                public void onSuccess(Float object) {
                                                    if (mediaObject != null) {
                                                        mediaObject.setCurrentVolume((int) (object * 100.0f));
                                                    }
                                                }

                                                @Override
                                                public void onError(ServiceCommandError error) {
                                                }
                                            });
                                }
                            }

                            NotificationsHelper.showNotification(context, title, subtitle, icon, false);

                            mMediaControl = object.mediaControl;
                            mMediaControl.subscribePlayState(CastManager.this);

                            for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners
                                    .entrySet()) {
                                playStatusListener.getValue()
                                        .onPlayStatusChanged(PlayStatusListener.STATUS_START_PLAYING);
                            }

                            Intent i = new Intent(context, AntiLeakActivityService.class);
                            i.addCategory("DummyServiceControl");
                            context.startService(i);

                            createListeners();
                            startUpdating();
                            if (disconnectDialog != null) {
                                disconnectDialog.cancel();
                            }
                        }

                        @Override
                        public void onError(ServiceCommandError error) {
                            error.printStackTrace();
                            for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners
                                    .entrySet()) {
                                playStatusListener.getValue()
                                        .onPlayStatusChanged(PlayStatusListener.STATUS_NOT_SUPPORT_LISTENER);
                            }
                            stop();
                            Toast.makeText(getActivity(), "Contenido no compatible", Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
    }

    public void startControlsActivity(Activity activity, Class classToStart) {
        Intent i = new Intent(context, classToStart);
        activity.startActivity(i);
    }

    private void unsetMediaControl() {
        mMediaControl = null;
        NotificationsHelper.cancelNotification(context);
        for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners.entrySet()) {
            playStatusListener.getValue().onPlayStatusChanged(PlayStatusListener.STATUS_STOPPED);
        }
        mediaObject = null;
    }

    public void setVolume(float volume) {
        if (isConnected() && mMediaControl != null && connectableDevice.getCapability(VolumeControl.class) != null) {
            connectableDevice.getCapability(VolumeControl.class).setVolume(volume, null);
        }
    }

    public void seekTo(long position) {
        if (isConnected() && mMediaControl != null) {
            mMediaControl.seek(position, new ResponseListener<Object>() {
                @Override
                public void onError(ServiceCommandError error) {
                    error.printStackTrace();
                    for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners
                            .entrySet()) {
                        playStatusListener.getValue().onSuccessSeek();
                    }
                }

                @Override
                public void onSuccess(Object object) {
                    for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners
                            .entrySet()) {
                        playStatusListener.getValue().onSuccessSeek();
                    }
                }
            });
        }
    }

    public void rewind() {
        if (isConnected() && mMediaControl != null) {
            isPaused = false;
            togglePause();
            mMediaControl.rewind(null);
        }
    }

    public void fastForward() {
        if (isConnected() && mMediaControl != null) {
            isPaused = false;
            togglePause();
            mMediaControl.fastForward(null);
        }
    }

    public void togglePause() {
        if (isConnected() && mMediaControl != null && mediaObject != null) {
            if (!isPaused) {
                mMediaControl.pause(null);
                isPaused = true;
                NotificationsHelper.showNotification(context, mediaObject.getTitle(), mediaObject.getSubtitle(), mediaObject
                        .getImage(), isPaused);
                for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners.entrySet()) {
                    playStatusListener.getValue()
                            .onPlayStatusChanged(PlayStatusListener.STATUS_PAUSED);
                }
            } else {
                mMediaControl.play(null);
                isPaused = false;
                NotificationsHelper.showNotification(context, mediaObject.getTitle(), mediaObject.getSubtitle(), mediaObject
                        .getImage(), isPaused);
                for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners.entrySet()) {
                    playStatusListener.getValue()
                            .onPlayStatusChanged(PlayStatusListener.STATUS_RESUME_PAUSE);
                }
            }
        } else {
            NotificationsHelper.cancelNotification(context);
        }
    }

    public void stop() {
        if (isConnected() && mMediaControl != null) {
            stopUpdating();
            mMediaControl.stop(null);
        } else {
            NotificationsHelper.cancelNotification(context.getApplicationContext());
        }
        unsetMediaControl();
    }

    public Boolean isConnected() {
        return connectableDevice != null && connectableDevice.isConnected();
    }

    private void calculateMenuVisibility() {
        if (discoveryManager != null) {
            setCastMenuVisible(discoveryManager.getAllDevices().size() > 0);
            if (isConnected()) {
                castMenuItem.setIcon(R.drawable.cast_on);
            }
        }
    }


    @Override
    public void onDeviceReady(ConnectableDevice device) {
        log("onDeviceReady is connected " + device.isConnected());
        if (device.isConnected()) {
            connectableDevice = device;
            StorageUtils.setRecentDeviceId(context, device.getId());
            castMenuItem.setIcon(R.drawable.cast_on);
            for (Map.Entry<String, CastListener> castListener : castListeners.entrySet()) {
                castListener.getValue().isConnected();
            }
        }
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {
        for (Map.Entry<String, CastListener> castListener : castListeners.entrySet()) {
            castListener.getValue().isDisconnected();
        }
    }

    @Override
    public void onPairingRequired(ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType) {
        switch (pairingType) {
            case FIRST_SCREEN:
                pairingAlertDialog.show();
                break;

            case PIN_CODE:
            case MIXED:
                pairingCodeDialog.show();
                break;

            case NONE:
            default:
                break;
        }
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
        log("onCapabilityUpdated");
    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
        error.printStackTrace();
        Log.e(TAG, "onConnectionFailed");
    }

    public void onDestroy() {
        stop();
        mediaObject = null;
        statusStartPlayingFired = false;
        if (context != null) {
            NotificationsHelper.cancelNotification(context);
        }
        if (connectToCastDialog != null) {
            connectToCastDialog.cancel();
            connectToCastDialog = null;
        }

        if (disconnectDialog != null) {
            disconnectDialog.cancel();
            disconnectDialog = null;
        }
        if (connectableDevice != null) {
            connectableDevice.disconnect();
            connectableDevice.removeListener(this);
            connectableDevice = null;
        }
        if (discoveryManager != null) {
            discoveryManager.removeListener(this);
        }

        if (pairingAlertDialog != null) {
            pairingAlertDialog.cancel();
            pairingAlertDialog = null;
        }
        if (pairingCodeDialog != null) {
            pairingCodeDialog.cancel();
            pairingCodeDialog = null;
        }

        positionListener = null;
        durationListener = null;
        stopUpdating();

        castListeners.clear();
        playStatusListeners.clear();
        try {
            activityWeakReference.clear();
            activityWeakReference = null;
        } catch (Exception e) {
            //untested
        }
    }

    private void stopUpdating() {
        if (refreshTimer == null)
            return;

        refreshTimer.cancel();
        refreshTimer = null;
    }

    private void startUpdating() {
        stopUpdating();
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    mMediaControl.getPosition(positionListener);
                    mMediaControl.getDuration(durationListener);
                } catch (Exception e) {
                    sendNotSupportGetStatus();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(1));
    }

    private void sendNotSupportGetStatus() {
        if (playStatusListeners.size() > 0) {
            if (mediaObject != null) {
                mediaObject.setIsSeekable(false);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners
                                .entrySet()) {
                            playStatusListener.getValue()
                                    .onPlayStatusChanged(PlayStatusListener.STATUS_NOT_SUPPORT_LISTENER);
                        }
                    }
                });
            }
        }
    }


    //Control de reproducción del video en la pantalla remota
    @Override
    public void onSuccess(MediaControl.PlayStateStatus playState) {
        switch (playState) {
            case Playing:
                log("PlayStateStatus: playing");
                break;

            case Finished:
                log("PlayStateStatus: finished");
                NotificationsHelper.cancelNotification(context);
                for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners.entrySet()) {
                    playStatusListener.getValue()
                            .onPlayStatusChanged(PlayStatusListener.STATUS_FINISHED);
                }
                mediaObject = null;
                break;

            case Buffering:
                log("PlayStateStatus: buffering");
                break;

            case Idle:
                log("PlayStateStatus: idle");
                break;

            case Paused:
                log("PlayStateStatus: paused");
                for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners.entrySet()) {
                    playStatusListener.getValue()
                            .onPlayStatusChanged(PlayStatusListener.STATUS_PAUSED);
                }
                statusStartPlayingFired = true;
                break;

            case Unknown:
                log("PlayStateStatus: unknown");
                break;
        }
    }

    private void notificatePositionAndDuration() {
        for (Map.Entry<String, PlayStatusListener> playStatusListener : playStatusListeners.entrySet()) {
            playStatusListener.getValue().onTotalDurationObtained(totalDuration);
            playStatusListener.getValue().onPositionChanged(currentPosition);
            playStatusListener.getValue().onPlayStatusChanged(PlayStatusListener.STATUS_PLAYING);
        }
    }

    private void createListeners() {
        positionListener = new MediaControl.PositionListener() {

            @Override
            public void onError(ServiceCommandError error) {
                sendNotSupportGetStatus();
            }

            @Override
            public void onSuccess(Long position) {
                currentPosition = position;
                notificatePositionAndDuration();
            }
        };
        durationListener = new MediaControl.DurationListener() {

            @Override
            public void onError(ServiceCommandError error) {
                sendNotSupportGetStatus();
            }

            @Override
            public void onSuccess(Long duration) {
                totalDuration = duration;
                notificatePositionAndDuration();
            }
        };
    }

    @Override
    public void onError(ServiceCommandError error) {
        error.printStackTrace();
    }
    //////////////////////////////////////////////////////////////
}
