package knf.kuma.explorer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import knf.kuma.R;

public class FragmentFilesRoot extends FragmentBase implements FragmentFiles.SelectedListener, FragmentChapters.ClearInterface, ExplorerCreator.EmptyListener {

    private FragmentFiles files;
    private FragmentChapters chapters;
    private boolean isFiles = true;
    private String name;
    private Boolean isRestored = false;

    public FragmentFilesRoot() {
        files = FragmentFiles.get(this);
        chapters = FragmentChapters.get(this);
    }

    public static FragmentFilesRoot get() {
        return new FragmentFilesRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
        super.onViewCreated(view, savedInstanceState);
    }

    private void setFragment(boolean isFiles, @Nullable String name) {
        this.isFiles = isFiles;
        this.name = name;
        ExplorerCreator.IS_FILES = isFiles;
        ExplorerCreator.FILES_NAME = name;
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            this.isFiles = savedInstanceState.getBoolean("isFiles", true);
            this.name = savedInstanceState.getString("name");
            this.isRestored = savedInstanceState.getBoolean("isRestored", false);
        }
        setFragment(ExplorerCreator.IS_FILES, ExplorerCreator.FILES_NAME);
        if (!ExplorerCreator.IS_CREATED)
            ExplorerCreator.start(getContext(), this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isFiles", isFiles);
        outState.putString("name", name);
        outState.putBoolean("isRestored", true);
    }

    @Override
    public void onSelected(String name) {
        setFragment(false, name);
    }

    @Override
    public void onClear() {
        setFragment(true, null);
    }

    @Override
    public void onEmpty() {
        if (files != null)
            files.onEmpty();
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
