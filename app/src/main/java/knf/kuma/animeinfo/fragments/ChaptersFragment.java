package knf.kuma.animeinfo.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeViewModel;
import knf.kuma.animeinfo.viewholders.AnimeChaptersHolder;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.pojos.AnimeObject;

public class ChaptersFragment extends BottomFragment {
    private AnimeChaptersHolder holder;

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
}
