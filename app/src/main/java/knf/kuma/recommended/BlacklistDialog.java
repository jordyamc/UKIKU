package knf.kuma.recommended;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BlacklistDialog extends DialogFragment {

    private List<String> genres = getGenres();
    private List<String> selected = new ArrayList<>();
    @NonNull
    private MultiChoiceListener listener;

    public static List<String> getGenres() {
        return Arrays.asList(
                "Acción",
                "Artes Marciales",
                "Aventuras",
                "Carreras",
                "Comedia",
                "Demencia",
                "Demonios",
                "Deportes",
                "Drama",
                "Ecchi",
                "Escolares",
                "Espacial",
                "Fantasía",
                "Ciencia Ficción",
                "Harem",
                "Historico",
                "Infantil",
                "Josei",
                "Juegos",
                "Magia",
                "Mecha",
                "Militar",
                "Misterio",
                "Musica",
                "Parodia",
                "Policía",
                "Psicológico",
                "Recuentos de la vida",
                "Romance",
                "Samurai",
                "Seinen",
                "Shoujo",
                "Shounen",
                "Sin Generos",
                "Sobrenatural",
                "Superpoderes",
                "Suspenso",
                "Terror",
                "Vampiros",
                "Yaoi",
                "Yuri");
    }

    public void init(List<String> selected, MultiChoiceListener listener) {
        this.selected = selected;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("Lista negra")
                .setMultiChoiceItems(genres.toArray(new String[0]), getStates(), (dialogInterface, index, isSelected) -> {
                    if (isSelected) {
                        selected.add(genres.get(index));
                    } else {
                        selected.remove(genres.get(index));
                    }
                }).setPositiveButton("SELECCIONAR", (dialogInterface, i) -> {
                    Collections.sort(selected);
                    listener.onOkay(selected);
                }).setNegativeButton("CERRAR", (dialogInterface, i) -> dialogInterface.dismiss()).create();
    }

    private boolean[] getStates() {
        boolean[] states = new boolean[genres.size()];
        int index = 0;
        for (String genre : genres) {
            states[index++] = selected.contains(genre);
        }
        return states;
    }

    public interface MultiChoiceListener {
        void onOkay(List<String> selected);
    }
}
