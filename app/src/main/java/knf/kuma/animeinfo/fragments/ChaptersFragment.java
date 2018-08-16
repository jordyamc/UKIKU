package knf.kuma.animeinfo.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProviders;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeViewModel;
import knf.kuma.animeinfo.viewholders.AnimeChaptersHolder;
import knf.kuma.commons.FileUtil;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import xdroid.toaster.Toaster;

public class ChaptersFragment extends BottomFragment implements AnimeChaptersHolder.ChapHolderCallback {
    private AnimeChaptersHolder holder;
    private String move_file = null;
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();

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
                if (PrefsUtil.INSTANCE.isChapsAsc())
                    Collections.reverse(chapters);
                holder.setAdapter(ChaptersFragment.this, chapters);
                holder.goToChapter();
            }
        });
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        View view = inflater.inflate(R.layout.recycler_chapters, container, false);
        holder = new AnimeChaptersHolder(getContext(), view, getChildFragmentManager(), this);
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
    public void onImportMultiple(List<AnimeObject.WebInfo.AnimeChapter> chapters) {
        if (chapters.size() == 0)
            Toaster.toast("No se puede importar ningun episodio");
        else if (chapters.size() == 1) {
            this.move_file = chapters.get(0).getFileName();
            onMove(chapters.get(0).getFileName());
        } else {
            this.chapters = chapters;
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    .setType("video/mp4"), 55698);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            try {
                if (data.getClipData() == null || data.getClipData().getItemCount() == 0) {
                    if (move_file == null && chapters.size() > 0) {
                        Uri uri = data.getData();
                        DocumentFile file = DocumentFile.fromSingleUri(getContext(), uri);
                        String last = getLastNumber(file.getName());
                        move_file = findChapter(last).getFileName();
                    }
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
                                    move_file = null;
                                    try {
                                        dialog.dismiss();
                                    } catch (Exception e) {
                                        //
                                    }
                                } else
                                    dialog.setProgress(pair.first);
                            }
                        } catch (Exception e) {
                            Toaster.toast("Error al importar");
                        }
                    });
                } else {
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .content("Comprobando archivos...")
                            .progress(false, 100)
                            .cancelable(false)
                            .build();
                    dialog.show();
                    List<Pair<Uri, String>> move_requests = new ArrayList<>();
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        try {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            DocumentFile file = DocumentFile.fromSingleUri(getContext(), uri);
                            String last = getLastNumber(file.getName());
                            move_requests.add(new Pair<>(uri, findChapter(last).getFileName()));
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (move_requests.size() == 0) {
                        Toaster.toast("No se pudo inferir el numero de los episodios");
                        try {
                            dialog.dismiss();
                        } catch (Exception e) {
                            //
                        }
                    } else {
                        FileUtil.moveFiles(getContext().getContentResolver(), move_requests).observe(ChaptersFragment.this, pairBooleanPair -> {
                            try {
                                if (pairBooleanPair != null) {
                                    if (pairBooleanPair.second) {
                                        Toaster.toast("Importados " + pairBooleanPair.first.second + " archivos exitosamente");
                                        holder.getAdapter().notifyDataSetChanged();
                                        chapters = new ArrayList<>();
                                        try {
                                            dialog.dismiss();
                                        } catch (Exception e) {
                                            //
                                        }
                                    } else {
                                        dialog.setContent(pairBooleanPair.first.first);
                                        dialog.setProgress(pairBooleanPair.first.second);
                                    }
                                }
                            } catch (Exception e) {
                                Toaster.toast("Error al importar");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("Error al importar");
            }
    }

    @Nullable
    private AnimeObject.WebInfo.AnimeChapter findChapter(String num) {
        for (AnimeObject.WebInfo.AnimeChapter c : new ArrayList<>(chapters)) {
            if (c.number.equals("Episodio " + num)) {
                chapters.remove(c);
                return c;
            }
        }
        return null;
    }

    @Nullable
    private String getLastNumber(String name) {
        Matcher matcher = Pattern.compile(".*[_ ]0?(\\d+)[_ ].*$|0?(\\d+)$").matcher(name.replace(".mp4", ""));
        String last = null;
        while (matcher.find()) {
            try {
                last = matcher.group(1);
                if (last == null)
                    last = matcher.group(2);
            } catch (Exception e) {
                try {
                    last = matcher.group(2);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
        return last;
    }
}
