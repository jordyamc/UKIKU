package knf.kuma.slices;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;
import knf.kuma.Main;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.PatternUtil;
import knf.kuma.pojos.AnimeObject;

public class AnimeSlice extends SliceProvider {

    /**
     * Instantiate any required objects. Return true if the provider was successfully created,
     * false otherwise.
     */

    private IconCompat launcherIcon;

    private static Bitmap getBitmap(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    @Override
    public boolean onCreateSliceProvider() {
        try {
            launcherIcon = IconCompat.createWithBitmap(getBitmap(Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher))));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Converts URL to content URI (i.e. content://knf.kuma.slices...)
     */
    @Override
    @NonNull
    public Uri onMapIntentToUri(@Nullable Intent intent) {
        // Note: implementing this is only required if you plan on catching URL requests.
        // This is an example solution.
        Uri.Builder uriBuilder = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT);
        if (intent == null) return uriBuilder.build();
        Uri data = intent.getData();
        if (data != null && data.getPath() != null) {
            String path = data.getPath().replace("/", "");
            uriBuilder = uriBuilder.path(path);
        }
        Context context = getContext();
        if (context != null) {
            uriBuilder = uriBuilder.authority(context.getPackageName());
        }
        return uriBuilder.build();
    }

    /**
     * Construct the Slice and bind data if available.
     */
    public Slice onBindSlice(Uri sliceUri) {
        Context context = getContext();
        if (context == null)
            return null;
        String path = sliceUri.getPath();
        if (path != null && path.startsWith("/anime/")) {
            if (!AnimeLoad.QUERY.equals(path)) {
                AnimeLoad.QUERY = path;
                context.sendBroadcast(new Intent(context, AnimeLoad.class));
                return new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY)
                        .addRow(
                                new RowBuilder()
                                        .setTitle("Buscando animes...", true)
                                        .setPrimaryAction(createActivityAction(null, null))
                        ).build();
            } else if (AnimeLoad.LIST.size() == 0) {
                return new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY)
                        .addRow(
                                new RowBuilder()
                                        .setTitle("No se encontraron animes")
                                        .setPrimaryAction(createActivityAction(null, path.replace("/anime/", "").trim()))
                        ).build();
            } else {
                ListBuilder listBuilder = new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY);
                listBuilder.setAccentColor(ContextCompat.getColor(context, EAHelper.getThemeColor(context)));
                List<AnimeObject> animeObjects = AnimeLoad.LIST;
                if (animeObjects.size() > 0) {
                    listBuilder.setHeader(
                            new ListBuilder.HeaderBuilder()
                                    .setTitle("UKIKU")
                                    .setSummary((animeObjects.size() < 5 ? "Solo " : "Más de ") + animeObjects.size() + " animes encontrados")
                                    .setPrimaryAction(createActivityAction(null, path.replace("/anime/", "").trim()))
                    );
                    for (AnimeObject animeObject : animeObjects)
                        listBuilder.addRow(
                                new RowBuilder()
                                        .setTitle(animeObject.name)
                                        .setSubtitle(animeObject.getGenresString())
                                        .setPrimaryAction(createOpenAnimeAction(animeObject))
                                        .setTitleItem(animeObject.icon, ListBuilder.SMALL_IMAGE)
                        );
                    listBuilder.addAction(createActivityAction(null, path.replace("/anime/", "").trim()));
                } else
                    return new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY)
                            .addRow(
                                    new RowBuilder()
                                            .setTitle("No se encontraron animes")
                                            .setPrimaryAction(createActivityAction(null, null))
                            ).build();
                return listBuilder.build();
            }
        } else
            return null;
    }

    private SliceAction createActivityAction(@Nullable AnimeObject animeObject, @Nullable String query) {
        if (animeObject == null)
            if (query != null) {
                return SliceAction.create(
                        PendingIntent.getActivity(
                                getContext(), 0,
                                new Intent(getContext(), Main.class)
                                        .putExtra("start_position", 4)
                                        .putExtra("search_query", query)
                                , PendingIntent.FLAG_CANCEL_CURRENT
                        ),
                        AnimeLoad.searchIcon,
                        ListBuilder.ICON_IMAGE,
                        "Abrir busqueda"
                );
            } else {
                return SliceAction.create(
                        PendingIntent.getActivity(
                                getContext(), 0, new Intent(getContext(), Main.class), 0
                        ),
                        launcherIcon,
                        ListBuilder.SMALL_IMAGE,
                        "Abrir App"
                );
            }
        else
            return SliceAction.create(
                    PendingIntent.getActivity(
                            getContext(), animeObject.key, new Intent(getContext(), ActivityAnime.class)
                                    .setData(Uri.parse(animeObject.link))
                                    .putExtra("title", animeObject.name)
                                    .putExtra("aid", animeObject.aid)
                                    .putExtra("img", PatternUtil.getCover(animeObject.aid)), PendingIntent.FLAG_CANCEL_CURRENT
                    ),
                    IconCompat.createWithResource(getContext(), R.drawable.ic_open),
                    ListBuilder.ICON_IMAGE,
                    "Abrir anime"
            );
    }

    private SliceAction createOpenAnimeAction(@Nullable AnimeObject animeObject) {
        return SliceAction.create(
                PendingIntent.getActivity(
                        getContext(), animeObject.key, new Intent(getContext(), ActivityAnime.class)
                                .setData(Uri.parse(animeObject.link))
                                .putExtra("title", animeObject.name)
                                .putExtra("aid", animeObject.aid)
                                .putExtra("img", PatternUtil.getCover(animeObject.aid)), PendingIntent.FLAG_CANCEL_CURRENT
                ),
                AnimeLoad.openIcon,
                ListBuilder.ICON_IMAGE,
                "Abrir anime"
        );
    }

    /**
     * Slice has been pinned to external process. Subscribe to data source if necessary.
     */
    @Override
    public void onSlicePinned(Uri sliceUri) {
        // When data is received, call context.contentResolver.notifyChange(sliceUri, null) to
        // trigger AnimeSlice#onBindSlice(Uri) again.
    }

    /**
     * Unsubscribe from data source if necessary.
     */
    @Override
    public void onSliceUnpinned(Uri sliceUri) {
        // Remove any observers if necessary to avoid memory leaks.
    }
}
