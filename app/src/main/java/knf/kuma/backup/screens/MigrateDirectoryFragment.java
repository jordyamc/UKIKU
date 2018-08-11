package knf.kuma.backup.screens;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.directory.DirectoryService;

import static knf.kuma.directory.DirectoryService.STATE_FINISHED;
import static knf.kuma.directory.DirectoryService.STATE_FULL;
import static knf.kuma.directory.DirectoryService.STATE_INTERRUPTED;
import static knf.kuma.directory.DirectoryService.STATE_PARTIAL;

public class MigrateDirectoryFragment extends Fragment {

    @BindView(R.id.tv_directory_count)
    TextView dir_count;
    @BindView(R.id.loading)
    LinearLayout loading;
    @BindView(R.id.tv_error)
    TextView tv_error;

    private DirectoryService.OnDirStatus onDirStatus;

    public MigrateDirectoryFragment() {
    }

    public static MigrateDirectoryFragment get(DirectoryService.OnDirStatus dirStatus) {
        MigrateDirectoryFragment fragment = new MigrateDirectoryFragment();
        fragment.setOnDirStatus(dirStatus);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_migrate_directory, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CacheDB.INSTANCE.animeDAO().getAllList().observe(this, animeObjects -> dir_count.setText(String.valueOf(animeObjects.size())));
        DirectoryService.getLiveStatus().observe(this, integer -> {
            if (integer != null)
                switch (integer) {
                    case STATE_PARTIAL:
                        Log.e("Dir", "Partial search");
                        break;
                    case STATE_FULL:
                        Log.e("Dir", "Full search");
                        break;
                    case STATE_INTERRUPTED:
                        Log.e("Dir", "Interrupted");
                        loading.setVisibility(View.GONE);
                        tv_error.setText("Error: Creacion interrumpida");
                        tv_error.setVisibility(View.VISIBLE);
                        break;
                    case STATE_FINISHED:
                        Log.e("Dir", "Finished");
                        onDirStatus.onFinished();
                        break;
                }
        });
    }

    public void setOnDirStatus(DirectoryService.OnDirStatus onDirStatus) {
        this.onDirStatus = onDirStatus;
    }
}
