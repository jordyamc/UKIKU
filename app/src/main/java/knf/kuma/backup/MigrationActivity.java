package knf.kuma.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import knf.kuma.R;
import knf.kuma.backup.screens.MigrateDirectoryFragment;
import knf.kuma.backup.screens.MigrateSuccessFragment;
import knf.kuma.backup.screens.MigrateVersionFragment;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.directory.DirectoryService;

public class MigrationActivity extends AppCompatActivity implements DirectoryService.OnDirStatus {

    public static void start(Context context) {
        context.startActivity(new Intent(context, MigrationActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_migrate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MigrateVersionFragment.getInstalledCode(this) < 252)
            setFragment(new MigrateVersionFragment());
        else if (!PrefsUtil.INSTANCE.isDirectoryFinished()) {
            DirectoryService.run(this);
            setFragment(MigrateDirectoryFragment.get(this));
        } else
            setFragment(new MigrateSuccessFragment());
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.root, fragment);
        transaction.commit();
    }

    @Override
    public void onFinished() {
        setFragment(new MigrateSuccessFragment());
    }
}
