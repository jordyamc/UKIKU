package knf.kuma.recommended.sections;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.recommended.RHHolder;
import knf.kuma.recommended.RIHolder;

/**
 * Created by jordy on 26/03/2018.
 */

public class MultipleSection extends StatelessSection {
    private Activity activity;
    private String name;
    private List<AnimeObject> animeObjects = new ArrayList<>();

    public MultipleSection(Activity activity, String name, List<AnimeObject> list, boolean isGrid) {
        super(SectionParameters.builder()
                .itemResourceId(isGrid ? R.layout.item_fav_grid : R.layout.item_fav)
                .headerResourceId(R.layout.item_recommend_header)
                .build());
        this.activity = activity;
        this.animeObjects = list;
        this.name = name;
    }

    @Override
    public int getContentItemsTotal() {
        return animeObjects.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new RIHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder h, int position) {
        final RIHolder holder = (RIHolder) h;
        final AnimeObject object = animeObjects.get(position);
        PicassoSingle.get(activity).load(PatternUtil.getCover(object.aid)).into(holder.img);
        holder.title.setText(object.name);
        holder.type.setText(object.type);
        holder.cardView.setOnClickListener(view -> ActivityAnime.open(activity, object, holder.img, true, true));
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new RHHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder h) {
        RHHolder holder = (RHHolder) h;
        holder.title.setText(name);
    }
}
