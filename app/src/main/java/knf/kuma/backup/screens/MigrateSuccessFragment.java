package knf.kuma.backup.screens;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import knf.kuma.R;
import knf.kuma.backup.objects.FavList;
import knf.kuma.backup.objects.SeenList;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.FavoriteObject;
import xdroid.toaster.Toaster;

public class MigrateSuccessFragment extends Fragment {

    private final int REQUEST_FAVS = 5628;
    private final int REQUEST_SEEN = 9986;

    public MigrateSuccessFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_migrate_success, container, false);
        ButterKnife.bind(this, view);
        return view;
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
    public void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content("Migrando...")
                .progress(true, 0)
                .cancelable(false)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            try {
                switch (requestCode) {
                    case REQUEST_FAVS:
                        List<FavoriteObject> list = FavList.decode(getContext().getContentResolver().openInputStream(data.getData()));
                        CacheDB.INSTANCE.favsDAO().addAll(list);
                        break;
                    case REQUEST_SEEN:
                        List<AnimeObject.WebInfo.AnimeChapter> chapters = SeenList.decode(getContext().getContentResolver().openInputStream(data.getData()));
                        CacheDB.INSTANCE.chaptersDAO().addAll(chapters);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                Toaster.toast("Error al migrar datos");
            }
            if (dialog.isShowing())
                dialog.dismiss();
        });
    }
}
