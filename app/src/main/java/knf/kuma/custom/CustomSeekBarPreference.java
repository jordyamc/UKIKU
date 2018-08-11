package knf.kuma.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

public class CustomSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener{

    @BindView(R.id.custom_title)
    TextView title;
    @BindView(R.id.seek)
    SeekBar seekBar;
    @BindView(R.id.value)
    TextView value;

    private Context context;
    private int MAX_VALUE=100;
    private int MIN_VALUE=0;
    private int INIT_VALUE=0;
    private int STEP_VALUE=1;
    private String TEXT_MASK="%d";
    private String TEXT_ZERO="-";


    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomSeekBarPreference(Context context) {
        super(context);
        init(context,null);
    }

    private void init(Context context, @Nullable AttributeSet attrs){
        this.context=context;
        setLayoutResource(R.layout.custom_seekbar_preference);
        if (attrs!=null)
            setDefValues(attrs);
    }

    private void setDefValues(AttributeSet defValues){
        TypedArray array=context.obtainStyledAttributes(defValues,R.styleable.CustomSeekBarPreference);
        MAX_VALUE=array.getInt(R.styleable.CustomSeekBarPreference_cs_max,100);
        MIN_VALUE=array.getInt(R.styleable.CustomSeekBarPreference_cs_min,0);
        TEXT_MASK=array.getString(R.styleable.CustomSeekBarPreference_cs_mask);
        TEXT_ZERO=array.getString(R.styleable.CustomSeekBarPreference_cs_zero_value);
        array.recycle();
        INIT_VALUE=getPersistedInt(PreferenceManager.getDefaultSharedPreferences(context).getInt(getKey(),0));
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        View view=((LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE )).inflate(R.layout.custom_seekbar_preference,parent,false);
        ButterKnife.bind(this, view);
        title.setText(getTitle());
        seekBar.setMax(MAX_VALUE);
        seekBar.setProgress(INIT_VALUE);
        seekBar.incrementProgressBy(STEP_VALUE);
        if (INIT_VALUE==0) {
            value.setText(TEXT_ZERO);
        }else {
            value.setText(String.format(TEXT_MASK,INIT_VALUE));
        }
        seekBar.setOnSeekBarChangeListener(this);
        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress==0){
            value.setText(TEXT_ZERO);
        }else {
            value.setText(String.format(TEXT_MASK,progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        persistInt(seekBar.getProgress());
        callChangeListener(seekBar.getProgress());
    }
}
