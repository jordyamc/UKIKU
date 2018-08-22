package knf.kuma.animeinfo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import es.munix.multidisplaycast.CastManager;
import knf.kuma.R;
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.ExplorerObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.pojos.QueueObject;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.pojos.SeeingObject;
import knf.kuma.recommended.RankType;
import knf.kuma.recommended.RecommendHelper;
import knf.kuma.widgets.emision.WEListItem;
import xdroid.toaster.Toaster;

public class ActivityAnime extends AppCompatActivity implements AnimeActivityHolder.Interface {
    public static int REQUEST_CODE = 558;
    private boolean isEdited = false;
    private AnimeViewModel viewModel;
    private AnimeActivityHolder holder;
    private FavoriteObject favoriteObject;
    private FavsDAO dao = CacheDB.INSTANCE.favsDAO();
    private SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();
    private List<String> genres = new ArrayList<>();

    public static void open(Fragment fragment, RecentObject object, View view, int position) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.anime));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        intent.putExtra("position", position);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Fragment fragment, AnimeObject object, View view) {
        open(fragment, object, view, true, true);
    }

    public static void open(Fragment fragment, AnimeObject object, View view, boolean persist, boolean animate) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        intent.putExtra("persist", persist);
        intent.putExtra("noTransition", !animate);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Activity activity, AnimeObject object, View view, boolean persist, boolean animate) {
        Intent intent = new Intent(activity, ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        intent.putExtra("persist", persist);
        intent.putExtra("noTransition", !animate);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle());
    }

    public static void open(Fragment fragment, ExplorerObject object, View view) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", String.valueOf(object.key));
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Activity activity, RecordObject object, View view) {
        Intent intent = new Intent(activity, ActivityAnime.class);
        intent.setData(Uri.parse(object.animeObject.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.animeObject.aid));
        intent.putExtra("persist", true);
        intent.putExtra("isRecord", true);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle());
    }

    public static void open(Activity activity, SeeingObject object, View view) {
        Intent intent = new Intent(activity, ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.title);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        intent.putExtra("persist", true);
        intent.putExtra("noTransition", true);
        intent.putExtra("isRecord", true);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle());
    }

    public static void open(Context context, AnimeObject object) {
        Intent intent = new Intent(context, ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        context.startActivity(intent);
    }

    public static void open(Fragment fragment, FavoriteObject object, View view) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", PatternUtil.getCover(object.aid));
        intent.putExtra("from_fav", true);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Activity activity, QueueObject object, String img, View view) {
        Intent intent = new Intent(activity, ActivityAnime.class);
        intent.putExtra("title", object.chapter.name);
        intent.putExtra("aid", object.chapter.aid);
        intent.putExtra("img", PatternUtil.getCover(object.chapter.aid));
        intent.putExtra("aid_only", true);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle());
    }

    public static void open(Fragment fragment, AnimeObject.WebInfo.AnimeRelated object) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse("https://animeflv.net/" + object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        fragment.startActivityForResult(intent, REQUEST_CODE);
    }

    public static Intent getSimpleIntent(Context context, WEListItem item) {
        Intent intent = new Intent(context, ActivityAnime.class);
        intent.setData(Uri.parse(item.link));
        intent.putExtra("title", item.title);
        intent.putExtra("aid", item.aid);
        intent.putExtra("img", PatternUtil.getCover(item.aid));
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getThemeNA(this));
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_anime_info);
        } catch (InflateException e) {
            setContentView(R.layout.activity_anime_info_nwv);
        }
        if (getIntent().getBooleanExtra("notification", false))
            sendBroadcast(NotificationObj.fromIntent(getIntent()).getBroadcast(this));
        holder = new AnimeActivityHolder(this);
        setSupportActionBar(holder.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        holder.toolbar.setNavigationOnClickListener(view -> closeActivity());
        viewModel = ViewModelProviders.of(this).get(AnimeViewModel.class);
        if (getIntent().getBooleanExtra("aid_only", false))
            viewModel.init(getIntent().getStringExtra("aid"));
        else
            viewModel.init(this, getIntent().getDataString(), getIntent().getBooleanExtra("persist", true));
        load();
    }

    private void load() {
        viewModel.getLiveData().observe(this, object -> {
            if (object != null) {
                Crashlytics.setString("screen", "Anime: " + object.name);
                Answers.getInstance().logContentView(new ContentViewEvent().putContentName(object.name).putContentType(object.type).putContentId(object.aid));
                chapters = object.chapters;
                genres = object.genres;
                favoriteObject = new FavoriteObject(object);
                holder.setTitle(object.name);
                holder.loadImg(PatternUtil.getCover(object.aid), v -> startActivity(new Intent(ActivityAnime.this, ActivityImgFull.class).setData(Uri.parse(PatternUtil.getCover(object.aid))).putExtra("title", object.name), ActivityOptionsCompat.makeSceneTransitionAnimation(ActivityAnime.this, holder.imageView, "img").toBundle()));
                holder.setFABState(dao.isFav(favoriteObject.key), seeingDAO.getByAid(favoriteObject.aid) != null);
                holder.showFAB();
                supportInvalidateOptionsMenu();
                RecommendHelper.registerAll(genres, RankType.CHECK);
            } else {
                Toaster.toast("Error al cargar informacion del anime");
                onBackPressed();
            }
        });
    }

    private void setResult() {
        isEdited = true;
    }

    @Override
    public void onFabClicked(FloatingActionButton actionButton) {
        setResult();
        boolean isfav = dao.isFav(favoriteObject.key);
        boolean isSeeing = seeingDAO.getByAid(favoriteObject.aid) != null;
        if (isfav && isSeeing)
            onFabLongClicked(actionButton);
        else if (isfav) {
            holder.setFABState(false);
            dao.deleteFav(favoriteObject);
            RecommendHelper.registerAll(genres, RankType.UNFAV);
        } else if (isSeeing) {
            onFabLongClicked(actionButton);
        } else {
            holder.setFABState(true);
            dao.addFav(favoriteObject);
            RecommendHelper.registerAll(genres, RankType.FAV);
        }
    }

    @Override
    public void onFabLongClicked(FloatingActionButton actionButton) {
        try {
            final SeeingObject seeingObject = seeingDAO.getByAid(favoriteObject.aid);
            boolean isfav = dao.isFav(favoriteObject.key);
            boolean isSeeing = seeingObject != null;
            if (isSeeing) {
                new MaterialDialog.Builder(this)
                        .content("¿Convertir en favorito?")
                        .positiveText("si")
                        .negativeText("no")
                        .neutralText("dropear")
                        .onPositive((dialog, which) -> {
                            dao.addFav(favoriteObject);
                            seeingDAO.remove(seeingObject);
                            holder.setFABState(true);
                            RecommendHelper.registerAll(genres, RankType.FAV);
                        })
                        .onNegative((dialog, which) -> {
                            dao.deleteFav(favoriteObject);
                            holder.setFABSeeing();
                            RecommendHelper.registerAll(genres, RankType.UNFAV);
                        })
                        .onNeutral((dialog, which) -> {
                            seeingDAO.remove(seeingObject);
                            holder.setFABState(isfav);
                            RecommendHelper.registerAll(genres, RankType.UNFOLLOW);
                        }).build().show();
            } else if (!isfav) {
                new MaterialDialog.Builder(this)
                        .items("Seguir", "Seguir y favorito")
                        .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
                            switch (which) {
                                case 0:
                                    seeingDAO.add(SeeingObject.fromAnime(favoriteObject));
                                    holder.setFABSeeing();
                                    RecommendHelper.registerAll(genres, RankType.FOLLOW);
                                    Toaster.toast("Agregado a animes seguidos");
                                    break;
                                case 1:
                                    dao.addFav(favoriteObject);
                                    seeingDAO.add(SeeingObject.fromAnime(favoriteObject));
                                    holder.setFABState(true, true);
                                    RecommendHelper.registerAll(genres, RankType.FAV);
                                    RecommendHelper.registerAll(genres, RankType.FOLLOW);
                                    Toaster.toast("Agregado a animes seguidos y favoritos");
                                    break;
                            }
                            return true;
                        })
                        .positiveText("aceptar")
                        .negativeText("cancelar")
                        .build().show();
            } else {
                seeingDAO.add(SeeingObject.fromAnime(favoriteObject));
                holder.setFABState(true, true);
                RecommendHelper.registerAll(genres, RankType.FOLLOW);
                Toaster.toast("Agregado a animes seguidos");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onImgClicked(ImageView imageView) {

    }

    @Override
    public void onNeedRecreate() {
        try {
            if (!getIntent().getBooleanExtra("aid_only", false)) {
                viewModel.reload(this, getIntent().getDataString(), getIntent().getBooleanExtra("persist", true));
                load();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (favoriteObject != null) {
            getMenuInflater().inflate(R.menu.menu_anime_info, menu);
            CastManager.getInstance().registerForActivity(this, menu, R.id.castMenu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                share();
                break;
            case R.id.action_comments:
                new CommentsDialog(chapters).show(this);
                break;
        }
        return true;
    }

    private void share() {
        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, favoriteObject.name + "\n" + favoriteObject.link), "Compartir"));
            Answers.getInstance().logShare(new ShareEvent().putContentName(favoriteObject.name).putContentId(favoriteObject.aid));
        } catch (ActivityNotFoundException e) {
            Toaster.toast("No se encontraron aplicaciones para enviar");
        }
    }

    private void closeActivity() {
        holder.hideFABForce();
        if (getIntent().getBooleanExtra("from_fav", false) && isEdited) {
            finish();
        } else if (getIntent().getBooleanExtra("noTransition", false)) {
            finish();
        } else {
            supportFinishAfterTransition();
        }
    }

    @Override
    public void onBackPressed() {
        closeActivity();
    }
}
