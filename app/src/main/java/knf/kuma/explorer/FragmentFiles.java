package knf.kuma.explorer;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.List;

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
    private SelectedListener listener;
    private ExplorerFilesAdapter adapter;
    private boolean isFist=true;

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
        CacheDB.INSTANCE.explorerDAO().getAll().observe(this, new Observer<List<ExplorerObject>>() {
            @Override
            public void onChanged(List<ExplorerObject> explorerObjects) {
                if (count >= 1) {
                    progressBar.setVisibility(View.GONE);
                    error.setVisibility(explorerObjects.size() == 0 ? View.VISIBLE : View.GONE);
                    adapter.update(explorerObjects);
                    if (isFist && explorerObjects.size() != 0) {
                        isFist = false;
                        recyclerView.scheduleLayoutAnimation();
                    }
                } else {
                    count++;
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(getLayout(),container,false);
        ButterKnife.bind(this,view);
        adapter=new ExplorerFilesAdapter(this,listener);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @LayoutRes
    private int getLayout(){
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lay_type","0").equals("0")){
            return R.layout.recycler_explorer;
        }else {
            return R.layout.recycler_explorer_grid;
        }
    }

    public void onEmpty() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setListener(SelectedListener listener){
        this.listener=listener;
        if (adapter != null)
            adapter.setListener(listener);
    }

    public interface SelectedListener{
        void onSelected(String name);
    }
}
