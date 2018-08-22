package knf.kuma.animeinfo;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

public class BottomActionsDialog extends BottomSheetDialogFragment {
    public static final int STATE_SEEN = 0;
    public static final int STATE_UNSEEN = 1;
    public static final int STATE_IMPORT_MULTIPLE = 2;
    @BindView(R.id.action_seen)
    TextView action_seen;
    @BindView(R.id.action_unseen)
    TextView action_unseen;
    @BindView(R.id.action_import_all)
    TextView action_import_all;
    private ActionsCallback callback;

    public static BottomActionsDialog newInstance(ActionsCallback callback) {
        BottomActionsDialog actionsDialog = new BottomActionsDialog();
        actionsDialog.callback = callback;
        return actionsDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_bottom_actions, container, false);
        ButterKnife.bind(this, view);
        action_seen.setOnClickListener(view1 -> {
            if (callback != null)
                callback.onSelect(STATE_SEEN);
            safeDismiss();
        });
        action_unseen.setOnClickListener(view1 -> {
            if (callback != null)
                callback.onSelect(STATE_UNSEEN);
            safeDismiss();
        });
        action_import_all.setOnClickListener(view1 -> {
            if (callback != null)
                callback.onSelect(STATE_IMPORT_MULTIPLE);
            safeDismiss();
        });
        return view;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            try {
                BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
                View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
            } catch (Exception e) {
                //
            }
        });
        return dialog;
    }

    public void safeShow(FragmentManager manager, String tag) {
        try {
            show(manager, tag);
        } catch (Exception e) {
            //
        }
    }

    private void safeDismiss() {
        try {
            dismiss();
        } catch (Exception e) {
            //
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (callback != null)
            callback.onDismiss();
    }

    public interface ActionsCallback {
        void onSelect(int state);

        void onDismiss();
    }
}
