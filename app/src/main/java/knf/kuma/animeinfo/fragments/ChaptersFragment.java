package knf.kuma.animeinfo.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;
import java.util.List;

import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeViewModel;
import knf.kuma.animeinfo.viewholders.AnimeChaptersHolder;
import knf.kuma.commons.FileUtil;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import xdroid.toaster.Toaster;

public class ChaptersFragment extends BottomFragment {
    private AnimeChaptersHolder holder;
    private String move_file = null;

    public ChaptersFragment() {
    }

    public static ChaptersFragment get() {
        return new ChaptersFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProviders.of(getActivity()).get(AnimeViewModel.class).getLiveData().observe(this, object -> {
            if (object != null) {
                List<AnimeObject.WebInfo.AnimeChapter> chapters = object.chapters;
                if (PrefsUtil.isChapsAsc())
                    Collections.reverse(chapters);
                holder.setAdapter(ChaptersFragment.this, chapters);
                holder.goToChapter();
            }
        });
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_chapters, container, false);
        holder = new AnimeChaptersHolder(getContext(), view);
        return view;
    }

    @Override
    public void onReselect() {
        if (holder != null)
            holder.smoothGoToChapter();
    }

    public void onMove(String to) {
        this.move_file = to;
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setType("video/mp4"), 55698);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            try {
                MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .content("Importando...")
                        .progress(false, 100)
                        .cancelable(false)
                        .build();
                dialog.show();
                FileUtil.moveFile(getContext().getContentResolver(), data.getData(), FileAccessHelper.INSTANCE.getOutputStream(move_file)).observe(this, pair -> {
                    try {
                        if (pair != null) {
                            if (pair.second) {
                                if (pair.first == -1) {
                                    Toaster.toast("Error al importar");
                                    FileAccessHelper.INSTANCE.delete(move_file);
                                } else
                                    Toaster.toast("Importado exitosamente");
                                holder.getAdapter().notifyDataSetChanged();
                                dialog.dismiss();
                            } else
                                dialog.setProgress(pair.first);
                        }
                    } catch (Exception e) {
                        Toaster.toast("Error al importar");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("Error al importar");
            }
    }
}
