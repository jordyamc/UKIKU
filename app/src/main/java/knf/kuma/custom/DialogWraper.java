package knf.kuma.custom;

import android.app.Dialog;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogWraper extends DialogFragment {

    private MaterialDialog dialog;

    public static DialogWraper wrap(MaterialDialog dialog) {
        DialogWraper dialogWraper = new DialogWraper();
        dialogWraper.dialog = dialog;
        return dialogWraper;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        try {
            if (dialog == null)
                return new MaterialDialog.Builder(getActivity())
                        .content("Error al mostrar diálogo!")
                        .positiveText(android.R.string.ok)
                        .build();
        } catch (Exception e) {
            return new AlertDialog.Builder(getContext())
                    .setMessage("Error al mostrar diálogo!")
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .create();
        }
        return dialog;
    }
}
