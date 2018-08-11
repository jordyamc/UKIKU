package knf.kuma.explorer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class FragmentFiles extends Fragment {

    public static final String TAG = "Files";
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.error)
    View error;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.state)
    TextView state;
    private SelectedListener listener;
    private ExplorerFilesAdapter adapter;
    private boolean isFist = true;

    private int count = 0;

    public FragmentFiles() {
    }

    public static FragmentFiles get(SelectedListener listener) {
        FragmentFiles fragmentFiles = new FragmentFiles();
        fragmentFiles.setListener(listener);
        return fragmentFiles;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CacheDB.INSTANCE.explorerDAO().getAll().observe(this, explorerObjects -> {
            adapter.update(explorerObjects);
            if (explorerObjects.size() != 0) {
                progressBar.setVisibility(View.GONE);
                state.setVisibility(View.GONE);
                if (isFist) {
                    isFist = false;
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });
        ExplorerCreator.getStateListener().observe(this, s -> {
            state.setText(s);
            state.setVisibility(s == null ? View.GONE : View.VISIBLE);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        ButterKnife.bind(this, view);
        adapter = new ExplorerFilesAdapter(this, listener);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lay_type", "0").equals("0")) {
            return R.layout.recycler_explorer;
        } else {
            return R.layout.recycler_explorer_grid;
        }
    }

    public void onEmpty() {
        new Handler(Looper.getMainLooper()).post(() -> {
            progressBar.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            state.setVisibility(View.GONE);
        });
    }

    public void setListener(SelectedListener listener) {
        this.listener = listener;
        if (adapter != null)
            adapter.setListener(listener);
    }

    public interface SelectedListener {
        void onSelected(ExplorerObject object);
    }
}
