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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.custom.GridRecyclerView;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.ExplorerObject;

/**
 * Created by Jordy on 30/01/2018.
 */

public class FragmentFiles extends Fragment {

    public static FragmentFiles get(SelectedListener listener){
        FragmentFiles fragmentFiles=new FragmentFiles();
        fragmentFiles.setListener(listener);
        return fragmentFiles;
    }
    public FragmentFiles() {
    }

    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.error)
    View error;

    public static final String TAG="Files";
    private SelectedListener listener;
    private ExplorerFilesAdapter adapter;
    private boolean isFist=true;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CacheDB.INSTANCE.explorerDAO().getAll().observe(this, new Observer<List<ExplorerObject>>() {
            @Override
            public void onChanged(List<ExplorerObject> explorerObjects) {
                error.setVisibility(explorerObjects.size()==0?View.VISIBLE:View.GONE);
                adapter.update(explorerObjects);
                if (isFist&&explorerObjects.size()!=0){
                    isFist=false;
                    recyclerView.scheduleLayoutAnimation();
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

    private void setListener(SelectedListener listener){
        this.listener=listener;
    }

    public interface SelectedListener{
        void onSelected(String name);
    }
}
