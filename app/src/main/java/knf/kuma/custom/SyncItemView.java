package knf.kuma.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.backup.BUUtils;
import knf.kuma.backup.objects.BackupObject;
import knf.kuma.commons.Network;

public class SyncItemView extends RelativeLayout {

    @BindView(R.id.title)
    TextView tv_title;
    @BindView(R.id.date)
    TextView tv_date;
    @BindView(R.id.backup)
    Button backup;
    @BindView(R.id.restore)
    Button restore;
    @BindView(R.id.separator)
    View separator;

    private String title = "Error";
    private boolean showDivider = true;
    private String actionId = "neutral";

    private BackupObject backupObject;

    public SyncItemView(Context context) {
        super(context);
        inflate(context);
    }

    public SyncItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context);
        setDefaults(context, attrs);
    }

    public SyncItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context);
        setDefaults(context, attrs);
    }

    private void inflate(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sync_item_layout, this);
    }

    private void setDefaults(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SyncItemView);
        title = array.getString(R.styleable.SyncItemView_si_title);
        showDivider = array.getBoolean(R.styleable.SyncItemView_si_showDivider, true);
        actionId = array.getString(R.styleable.SyncItemView_si_actionId);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
        tv_title.setText(title);
        if (!showDivider)
            separator.setVisibility(GONE);
    }

    public void enableBackup(@Nullable final BackupObject backupObject, final OnClick onClick) {
        post(new Runnable() {
            @Override
            public void run() {
                if (Network.isConnected()) {
                    backup.setEnabled(true);
                    if (backupObject == null) {
                        tv_date.setText("Sin respaldo");
                    } else {
                        tv_date.setText(backupObject.date);
                        restore.setEnabled(true);
                    }
                    backup.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClick.onAction(SyncItemView.this, actionId, true);
                        }
                    });
                    restore.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClick.onAction(SyncItemView.this, actionId, false);
                        }
                    });
                } else {
                    tv_date.setText("Sin internet");
                }
            }
        });

    }

    public void clear() {
        backupObject = null;
        post(new Runnable() {
            @Override
            public void run() {
                backup.setEnabled(false);
                restore.setEnabled(false);
                tv_date.setText("Cargando...");
            }
        });
    }

    @Nullable
    public BackupObject getBakup() {
        return backupObject;
    }

    public void init(final OnClick onClick) {
        BUUtils.search(actionId, new BUUtils.SearchInterface() {
            @Override
            public void onResponse(@Nullable BackupObject object) {
                backupObject = object;
                enableBackup(backupObject, onClick);
            }
        });
    }

    public interface OnClick {
        void onAction(SyncItemView syncItemView, String id, boolean isBackup);
    }
}
