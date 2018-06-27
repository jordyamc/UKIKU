package knf.kuma.directory;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.directory.viewholders.DirMainFragmentHolder;

public class DirectoryFragment extends BottomFragment {
    private DirMainFragmentHolder fragmentHolder;

    public DirectoryFragment() {
    }

    public static DirectoryFragment get() {
        return new DirectoryFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProviders.of(this).get(DirectoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_directory, container, false);
        fragmentHolder = new DirMainFragmentHolder(view, getChildFragmentManager());
        EAHelper.enter1(getContext(), "D");
        return view;
    }

    public void onChangeOrder() {
        if (fragmentHolder != null)
            fragmentHolder.onChangeOrder();
    }

    @Override
    public void onReselect() {
        EAHelper.enter1(getContext(), "D");
        if (fragmentHolder != null)
            fragmentHolder.onReselect();
    }
}
