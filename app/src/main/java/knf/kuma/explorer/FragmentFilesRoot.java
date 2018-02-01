package knf.kuma.explorer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import knf.kuma.R;

/**
 * Created by Jordy on 30/01/2018.
 */

public class FragmentFilesRoot extends FragmentBase implements FragmentFiles.SelectedListener,FragmentChapters.ClearInterface {

    private FragmentFiles files;
    private FragmentChapters chapters;
    private boolean isFiles = true;

    public FragmentFilesRoot() {
        ExplorerCreator.start(getContext());
        files = FragmentFiles.get(this);
        chapters=FragmentChapters.get(this);
    }

    public static FragmentFilesRoot get() {
        return new FragmentFilesRoot();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explorer_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (!files.isAdded())
            transaction.add(R.id.root, files, FragmentFiles.TAG);
        if (!chapters.isAdded())
            transaction.add(R.id.root, chapters, FragmentChapters.TAG);
        transaction.commit();
        if (savedInstanceState == null)
            setFragment(true, null);
        super.onViewCreated(view, savedInstanceState);
    }

    private void setFragment(boolean isFiles, @Nullable String name) {
        this.isFiles = isFiles;
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (isFiles) {
            transaction.hide(chapters);
            transaction.show(files);
        } else {
            chapters.setName(name);
            transaction.hide(files);
            transaction.show(chapters);
        }
        transaction.setCustomAnimations(R.anim.fadein, R.anim.fadeout);
        transaction.commit();
    }

    @Override
    public void onSelected(String name) {
        setFragment(false, name);
    }

    @Override
    public void onClear() {
        setFragment(true,null);
    }

    @Override
    public boolean onBackPressed() {
        if (isFiles) {
            return false;
        } else {
            setFragment(true, null);
            return true;
        }
    }
}
