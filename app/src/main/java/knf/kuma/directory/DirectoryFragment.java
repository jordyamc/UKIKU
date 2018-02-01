package knf.kuma.directory;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import knf.kuma.BottomFragment;
import knf.kuma.directory.viewholders.DirMainFragmentHolder;
import knf.kuma.R;

/**
 * Created by Jordy on 06/01/2018.
 */

public class DirectoryFragment extends BottomFragment {
    public static DirectoryFragment get(){
        return new DirectoryFragment();
    }

    public DirectoryFragment() {
    }

    private DirMainFragmentHolder fragmentHolder;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProviders.of(this).get(DirectoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_directory,container,false);
        fragmentHolder=new DirMainFragmentHolder(view,getChildFragmentManager());
        return view;
    }

    public void onChangeOrder(){
        fragmentHolder.onChangeOrder();
    }

    @Override
    public void onReselect() {
        fragmentHolder.onReselect();
    }
}
