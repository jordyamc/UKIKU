package knf.kuma.backup.screens;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

public class MigrateVersionFragment extends Fragment {

    @BindView(R.id.tv_version_bad)
    TextView version;

    public MigrateVersionFragment() {
    }

    public static int getInstalledCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("knf.animeflv", 0);
            return info.versionCode;
        } catch (Exception e) {
            return -1;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_migrate_version, container, false);
        ButterKnife.bind(this, view);
        version.setText(String.valueOf(getInstalledCode(getContext())));
        return view;
    }
}
