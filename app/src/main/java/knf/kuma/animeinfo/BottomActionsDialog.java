package knf.kuma.animeinfo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

public class BottomActionsDialog extends BottomSheetDialogFragment {
    public static final int STATE_SEEN = 0;
    public static final int STATE_UNSEEN = 1;
    @BindView(R.id.action_seen)
    TextView action_seen;
    @BindView(R.id.action_unseen)
    TextView action_unseen;
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
            callback.onSelect(STATE_SEEN);
            dismiss();
        });
        action_unseen.setOnClickListener(view1 -> {
            callback.onSelect(STATE_UNSEEN);
            dismiss();
        });
        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        callback.onDismiss();
    }

    public interface ActionsCallback {
        void onSelect(int state);

        void onDismiss();
    }
}
