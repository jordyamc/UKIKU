package knf.kuma.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import butterknife.ButterKnife;
import knf.kuma.R;

public class HiddenOverlay extends LinearLayout {
    public HiddenOverlay(Context context) {
        super(context);
        inflate(context);
    }

    public HiddenOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context);
    }

    public HiddenOverlay(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context);
    }

    private void inflate(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_hidden_overlay, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    public void setHidden(final boolean hidden, boolean animate) {
        setState(hidden);
        if (animate)
            post(new Runnable() {
                @Override
                public void run() {
                    Animation animation = AnimationUtils.loadAnimation(getContext(), hidden ? R.anim.fadein : R.anim.fadeout);
                    animation.setDuration(200);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    startAnimation(animation);
                }
            });
    }

    private void setState(final boolean hidden) {
        post(new Runnable() {
            @Override
            public void run() {
                setVisibility(hidden ? VISIBLE : GONE);
            }
        });
    }
}
