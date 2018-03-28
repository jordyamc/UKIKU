package knf.kuma.search;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GenresDialog extends DialogFragment {

    private List<String> genres=new ArrayList<>();
    private List<String> selected=new ArrayList<>();
    @NonNull
    private MultichoiseListener listener;

    public void init(List<String> genres,List<String> selected,MultichoiseListener listener){
        this.genres=genres;
        this.selected=selected;
        this.listener=listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("Generos")
                .setMultiChoiceItems(genres.toArray(new String[genres.size()]), getStates(), new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index, boolean isSelected) {
                        if (isSelected){
                            selected.add(genres.get(index));
                        }else {
                            selected.remove(genres.get(index));
                        }
                    }
                }).setPositiveButton("BUSCAR", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Collections.sort(selected);
                        listener.onOkay(selected);
                    }
                }).setNegativeButton("CERRAR", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create();
    }

    private boolean[] getStates(){
        boolean[] states=new boolean[genres.size()];
        int index=0;
        for (String genre:genres){
            states[index++]=selected.contains(genre);
        }
        return states;
    }

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

    public interface MultichoiseListener {
        void onOkay(List<String> selected);
    }
}
