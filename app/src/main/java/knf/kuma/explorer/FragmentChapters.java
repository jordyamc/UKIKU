package knf.kuma.explorer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.ExplorerObject;
import xdroid.toaster.Toaster;

public class FragmentChapters extends Fragment {
    public static final String TAG = "Chapters";
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    ExplorerChapsAdapter adapter;
    private ClearInterface clearInterface;
    private boolean isFirst = true;

    public FragmentChapters() {
    }

    public static FragmentChapters get(ClearInterface clearInterface) {
        FragmentChapters fragmentChapters = new FragmentChapters();
        fragmentChapters.setInterface(clearInterface);
        return fragmentChapters;
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
            return R.layout.recycler_explorer_chaps;
        } else {
            return R.layout.recycler_explorer_chaps_grid;
        }
    }

    public void setObject(ExplorerObject object) {
        clear();
        object.getLiveData(getContext()).observe(this, fileDownObjs -> {
            if (fileDownObjs.size() == 0) {
                Toaster.toast("Directorio vacio");
                CacheDB.INSTANCE.explorerDAO().delete(object);
                clearInterface.onClear();
            } else {
                object.chapters = fileDownObjs;
                progressBar.setVisibility(View.GONE);
                adapter = new ExplorerChapsAdapter(FragmentChapters.this, object, clearInterface);
                recyclerView.setAdapter(adapter);
                if (isFirst) {
                    isFirst = false;
                    recyclerView.scheduleLayoutAnimation();
                }
            }
            //object.clearLiveData(this);
        });
    }

    void deleteAll() {
        if (adapter != null)
            adapter.deleteAll();
    }

    private void clear() {
        isFirst = true;
        adapter = null;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (progressBar != null)
                progressBar.setVisibility(View.VISIBLE);
            if (recyclerView != null)
                recyclerView.setAdapter(null);
        });
    }

    public void setInterface(ClearInterface clearInterface) {
        this.clearInterface = clearInterface;
        if (adapter != null)
            adapter.setInterface(clearInterface);
    }

    public interface ClearInterface {
        void onClear();
    }
}
