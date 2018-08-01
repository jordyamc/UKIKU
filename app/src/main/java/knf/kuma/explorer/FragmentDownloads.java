package knf.kuma.explorer;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;

public class FragmentDownloads extends FragmentBase {
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress)
    ProgressBar progress;
    @BindView(R.id.error)
    View error;
    private boolean isFirst = true;

    public FragmentDownloads() {
    }

    public static FragmentDownloads get() {
        return new FragmentDownloads();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CacheDB.INSTANCE.downloadsDAO().getActive().observe(this, downloadObjects -> {
            progress.setVisibility(View.GONE);
            error.setVisibility(downloadObjects.size() == 0 ? View.VISIBLE : View.GONE);
            if (isFirst || downloadObjects.size() == 0) {
                isFirst = false;
                recyclerView.setAdapter(new DownloadingAdapter(FragmentDownloads.this, downloadObjects));
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lay_type", "0").equals("0")) {
            return R.layout.recycler_downloading;
        } else {
            return R.layout.recycler_downloading_grid;
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
