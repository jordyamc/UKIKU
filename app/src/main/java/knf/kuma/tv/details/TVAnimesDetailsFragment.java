package knf.kuma.tv.details;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.palette.graphics.Palette;
import knf.kuma.R;
import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.retrofit.Repository;
import knf.kuma.tv.TVServersFactory;
import knf.kuma.tv.anime.ChapterPresenter;
import knf.kuma.tv.anime.RelatedPresenter;

public class TVAnimesDetailsFragment extends DetailsSupportFragment implements OnItemViewClickedListener, OnActionClickedListener {

    private ArrayObjectAdapter mRowsAdapter;
    private FavoriteObject favoriteObject;
    private AnimeObject.WebInfo.AnimeChapter currentChapter;
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();
    private SparseArrayObjectAdapter actionAdapter;
    private ArrayObjectAdapter listRowAdapter;

    public static TVAnimesDetailsFragment get(String url) {
        TVAnimesDetailsFragment fragment = new TVAnimesDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildDetails();
        setOnItemViewClickedListener(this);
    }

    private int getLastSeen(List<AnimeObject.WebInfo.AnimeChapter> chapters) {
        if (chapters.size() > 0) {
            AnimeObject.WebInfo.AnimeChapter chapter = CacheDB.INSTANCE.chaptersDAO().getLast(PatternUtil.getEids(chapters));
            if (chapter != null) {
                int position = chapters.indexOf(chapter);
                if (position >= 0)
                    return position;
            }
        }
        return 0;
    }

    private void buildDetails() {
        new Repository().getAnime(getContext(), getArguments().getString("url"), true).observe(getActivity(), animeObject ->
                Glide.with(getActivity()).asBitmap().load(PatternUtil.getCover(animeObject.aid)).into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Palette.from(resource).generate(palette -> {
                            Palette.Swatch swatch = palette.getDarkMutedSwatch();
                            favoriteObject = new FavoriteObject(animeObject);
                            chapters = animeObject.chapters;
                            Collections.reverse(chapters);
                            ClassPresenterSelector selector = new ClassPresenterSelector();
                            FullWidthDetailsOverviewRowPresenter rowPresenter =
                                    new CustomFullWidthDetailsOverviewRowPresenter(
                                            swatch == null ?
                                                    new DetailsDescriptionPresenter() :
                                                    new DetailsDescriptionPresenter(swatch.getTitleTextColor(), swatch.getBodyTextColor()));
                            if (swatch != null) {
                                rowPresenter.setBackgroundColor(swatch.getRgb());
                                float[] hsv = new float[3];
                                int color = swatch.getRgb();
                                Color.colorToHSV(color, hsv);
                                hsv[2] *= 0.8f;
                                rowPresenter.setActionsBackgroundColor(Color.HSVToColor(hsv));
                            }
                            selector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);
                            selector.addClassPresenter(ChaptersListRow.class, new ChaptersListPresenter(getLastSeen(chapters)));
                            selector.addClassPresenter(ListRow.class, new ListRowPresenter());
                            mRowsAdapter = new ArrayObjectAdapter(selector);
                            DetailsOverviewRow detailsOverview = new DetailsOverviewRow(animeObject);

                            // Add images and action buttons to the details view
                            detailsOverview.setImageBitmap(getContext(), resource);
                            detailsOverview.setImageScaleUpAllowed(true);
                            actionAdapter = new SparseArrayObjectAdapter();
                            if (CacheDB.INSTANCE.favsDAO().isFav(animeObject.key)) {
                                actionAdapter.set(1, new Action(1, "Quitar favorito", null, ContextCompat.getDrawable(getContext(), R.drawable.heart_full)));
                            } else {
                                actionAdapter.set(1, new Action(1, "Añadir favorito", null, ContextCompat.getDrawable(getContext(), R.drawable.heart_empty)));
                            }
                            detailsOverview.setActionsAdapter(actionAdapter);
                            rowPresenter.setOnActionClickedListener(TVAnimesDetailsFragment.this);
                            mRowsAdapter.add(detailsOverview);

                            // Add a Chapters items row
                            if (chapters.size() > 0) {
                                listRowAdapter = new ArrayObjectAdapter(
                                        new ChapterPresenter());
                                for (AnimeObject.WebInfo.AnimeChapter chapter : chapters)
                                    listRowAdapter.add(chapter);
                                HeaderItem header = new HeaderItem(0, "Episodios");
                                mRowsAdapter.add(new ChaptersListRow(header, listRowAdapter));
                            }

                            // Add a Related items row
                            if (animeObject.related.size() > 0) {
                                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(
                                        new RelatedPresenter());
                                for (AnimeObject.WebInfo.AnimeRelated related : animeObject.related)
                                    listRowAdapter.add(related);
                                HeaderItem header = new HeaderItem(0, "Relacionados");
                                mRowsAdapter.add(new ListRow(header, listRowAdapter));
                            }

                            setAdapter(mRowsAdapter);
                        });
                    }
                }));
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof AnimeObject.WebInfo.AnimeRelated) {
            TVAnimesDetails.start(getContext(), "https://animeflv.net" + ((AnimeObject.WebInfo.AnimeRelated) item).link);
        } else if (item instanceof AnimeObject.WebInfo.AnimeChapter) {
            currentChapter = (AnimeObject.WebInfo.AnimeChapter) item;
            TVServersFactory.start(getActivity(), currentChapter.link, currentChapter, itemViewHolder, (TVAnimesDetails) getActivity());
        }
    }

    public void onStartStreaming() {
        if (currentChapter != null)
            listRowAdapter.notifyArrayItemRangeChanged(chapters.indexOf(currentChapter), 1);
    }

    @Override
    public void onActionClicked(Action action) {
        actionAdapter.clear();
        if (CacheDB.INSTANCE.favsDAO().isFav(favoriteObject.key)) {
            CacheDB.INSTANCE.favsDAO().deleteFav(favoriteObject);
            action.setLabel1("Añadir favorito");
            action.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.heart_empty));
        } else {
            CacheDB.INSTANCE.favsDAO().addFav(favoriteObject);
            action.setLabel1("Quitar favorito");
            action.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.heart_full));
        }
        actionAdapter.set(1, action);
    }
}
