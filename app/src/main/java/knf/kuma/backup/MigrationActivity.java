package knf.kuma.backup;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import knf.kuma.R;
import knf.kuma.backup.objects.FavList;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.FavoriteObject;
import xdroid.toaster.Toaster;

/**
 * Created by Jordy on 22/02/2018.
 */

public class MigrationActivity extends AppCompatActivity {

    private final int REQUEST_FAVS = 5628;
    private final int REQUEST_SEEN = 9986;

    public static void start(Context context) {
        context.startActivity(new Intent(context, MigrationActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_migrate);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.migrate_favs)
    void onMigrateFavs(View view) {
        startActivityForResult(new Intent().setAction("knf.kuma.MIGRATE").putExtra("type", 0), REQUEST_FAVS);
    }

    @OnClick(R.id.migrate_seen)
    void onMigrateSeen(View view) {
        startActivityForResult(new Intent().setAction("knf.kuma.MIGRATE").putExtra("type", 1), REQUEST_SEEN);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("Migrando...")
                .progress(true, 0)
                .cancelable(false)
                .build();
        dialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (requestCode) {
                        case REQUEST_FAVS:
                            List<FavoriteObject> list = FavList.decode(getContentResolver().openInputStream(data.getData()));
                            CacheDB.INSTANCE.favsDAO().addAll(list);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                    Toaster.toast("Error al migrar datos");
                }
                if (dialog.isShowing())
                    dialog.dismiss();
            }
        });
    }
}
