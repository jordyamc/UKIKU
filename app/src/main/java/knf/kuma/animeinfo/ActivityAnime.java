package knf.kuma.animeinfo;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ShareEvent;

import knf.kuma.R;
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.ExplorerObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.pojos.SeeingObject;
import xdroid.toaster.Toaster;

/**
 * Created by Jordy on 04/01/2018.
 */

public class ActivityAnime extends AppCompatActivity implements AnimeActivityHolder.Interface {
    public static int REQUEST_CODE = 558;
    private boolean isEdited = false;
    private AnimeActivityHolder holder;
    private FavoriteObject favoriteObject;
    private FavsDAO dao = CacheDB.INSTANCE.favsDAO();
    private SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();

    public static void open(Fragment fragment, RecentObject object, View view, int position) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.anime));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", object.img);
        intent.putExtra("position", position);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Fragment fragment, AnimeObject object, View view) {
        open(fragment, object, view, true,true);
    }

    public static void open(Fragment fragment, AnimeObject object, View view, boolean persist,boolean animate) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", object.img);
        intent.putExtra("persist", persist);
        intent.putExtra("noTransition", !animate);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Fragment fragment, ExplorerObject object, View view) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", String.valueOf(object.key));
        intent.putExtra("img", object.img);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Activity activity, RecordObject object, View view) {
        Intent intent = new Intent(activity, ActivityAnime.class);
        intent.setData(Uri.parse(object.animeObject.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", object.animeObject.img);
        intent.putExtra("persist", true);
        intent.putExtra("isRecord", true);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle());
    }

    public static void open(Activity activity, SeeingObject object, View view) {
        Intent intent = new Intent(activity, ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.title);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", object.img);
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
        intent.putExtra("img", object.img);
        context.startActivity(intent);
    }

    public static void open(Fragment fragment, FavoriteObject object, View view) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse(object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        intent.putExtra("img", object.img);
        intent.putExtra("from_fav", true);
        fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity(), view, "img").toBundle());
    }

    public static void open(Fragment fragment, AnimeObject.WebInfo.AnimeRelated object) {
        Intent intent = new Intent(fragment.getContext(), ActivityAnime.class);
        intent.setData(Uri.parse("https://animeflv.net/" + object.link));
        intent.putExtra("title", object.name);
        intent.putExtra("aid", object.aid);
        fragment.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_info);
        if (getIntent().getBooleanExtra("notification", false))
            sendBroadcast(NotificationObj.fromIntent(getIntent()).getBroadcast(this));
        holder = new AnimeActivityHolder(this);
        setSupportActionBar(holder.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        holder.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeActivity();
            }
        });
        AnimeViewModel viewModel = ViewModelProviders.of(this).get(AnimeViewModel.class);
        viewModel.init(getIntent().getDataString(), getIntent().getBooleanExtra("persist", true));
        viewModel.getLiveData().observe(this, new Observer<AnimeObject>() {
            @Override
            public void onChanged(@Nullable final AnimeObject object) {
                if (object != null) {
                    favoriteObject = new FavoriteObject(object);
                    holder.setTitle(object.name);
                    holder.loadImg(object.img, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(ActivityAnime.this, ActivityImgFull.class).setData(Uri.parse(object.img)).putExtra("title", object.name), ActivityOptionsCompat.makeSceneTransitionAnimation(ActivityAnime.this, holder.imageView, "img").toBundle());
                        }
                    });
                    if (dao.isFav(favoriteObject.key)) {
                        holder.setFABState(true);
                    } else if (seeingDAO.getByAid(favoriteObject.aid) != null) {
                        holder.setFABSeeing();
                    } else {
                        holder.setFABState(false);
                    }
                    holder.showFAB();
                    supportInvalidateOptionsMenu();
                }
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
        if (isfav) {
            holder.setFABState(false);
            dao.deleteFav(favoriteObject);
        } else if (isSeeing) {
            onFabLongClicked(actionButton);
        } else {
            holder.setFABState(true);
            dao.addFav(favoriteObject);
        }
    }

    @Override
    public void onFabLongClicked(FloatingActionButton actionButton) {
        final SeeingObject seeingObject = seeingDAO.getByAid(favoriteObject.aid);
        boolean isfav = dao.isFav(favoriteObject.key);
        boolean isSeeing = seeingObject != null;
        if (!isfav) {
            if (isSeeing) {
                new MaterialDialog.Builder(this)
                        .content("¿Convertir en favorito?")
                        .positiveText("si")
                        .negativeText("no")
                        .neutralText("dropear")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                seeingDAO.remove(seeingObject);
                                dao.addFav(favoriteObject);
                                holder.setFABState(true);
                            }
                        })
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                seeingDAO.remove(seeingObject);
                                holder.setFABState(false);
                            }
                        }).build().show();
            } else {
                holder.setFABSeeing();
                seeingDAO.add(SeeingObject.fromAnime(favoriteObject));
                Toaster.toast("Agregado a animes seguidos");
            }
        }
    }

    @Override
    public void onImgClicked(ImageView imageView) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (favoriteObject != null) {
            getMenuInflater().inflate(R.menu.menu_anime_info, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                share();
                break;
        }
        return true;
    }

    private void share() {
        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, favoriteObject.name)
                    .putExtra(Intent.EXTRA_TEXT, favoriteObject.link), "Compartir"));
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
