package knf.kuma.custom;

import android.app.Dialog;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class DialogWrapper extends DialogFragment {

    private MaterialDialog dialog;

    public static DialogWrapper wrap(MaterialDialog dialog) {
        DialogWrapper dialogWrapper = new DialogWrapper();
        dialogWrapper.dialog = dialog;
        return dialogWrapper;
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
            return dialog;
        } catch (Exception e) {
            return new AlertDialog.Builder(getContext())
                    .setMessage("Error al mostrar diálogo!")
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .create();
        }
    }

    public void safeShow(FragmentManager manager, String tag) {
        try {
            show(manager, tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
