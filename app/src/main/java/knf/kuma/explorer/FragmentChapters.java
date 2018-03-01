package knf.kuma.explorer;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.ExplorerObject;

public class FragmentChapters extends Fragment {
    public static final String TAG = "Chapters";
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
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

    public void setName(String name) {
        clear();
        CacheDB.INSTANCE.explorerDAO().getItem(name).observe(this, new Observer<ExplorerObject>() {
            @Override
            public void onChanged(@Nullable ExplorerObject object) {
                if (isFirst) {
                    isFirst = false;
                    adapter = new ExplorerChapsAdapter(FragmentChapters.this, object, clearInterface);
                    recyclerView.setAdapter(adapter);
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });
    }

    private void clear(){
        isFirst=true;
        if (recyclerView!=null)
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    recyclerView.setAdapter(null);
                }
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
