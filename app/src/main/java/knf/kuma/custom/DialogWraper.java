package knf.kuma.custom;

import android.app.Dialog;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        return dialog;
    }
}
