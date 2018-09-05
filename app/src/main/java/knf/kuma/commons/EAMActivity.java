package knf.kuma.commons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class EAMActivity extends FragmentActivity implements OnMapReadyCallback {

    @BindView(R.id.konfetti)
    KonfettiView konfettiView;
    private GoogleMap mMap;
    private Marker marker;

    public static void start(Activity context) {
        context.startActivityForResult(new Intent(context, EAMActivity.class), 5698);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lay_map);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng point = new LatLng(35.702067, 139.774528);
        marker = mMap.addMarker(new MarkerOptions().position(point).title("Easter Egg completado!").visible(false).icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVD(this, R.drawable.ic_treasure))));
        googleMap.setOnCameraMoveListener(() -> marker.setVisible(mMap.getCameraPosition().zoom >= 13));
        googleMap.setOnMarkerClickListener(m -> {
            if (m.equals(marker)) {
                EAHelper.enter3();
                konfettiView.build()
                        .addColors(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN, Color.MAGENTA)
                        .setDirection(0.0, 359.0)
                        .setSpeed(4f, 7f)
                        .setFadeOutEnabled(true)
                        .setTimeToLive(2000)
                        .addShapes(Shape.RECT, Shape.CIRCLE)
                        .addSizes(new Size(12, 6f), new Size(16, 6f))
                        .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
                        .streamFor(300, 10000L);
            }
            return false;
        });
    }

    private Bitmap getBitmapFromVD(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
