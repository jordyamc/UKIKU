package knf.kuma.tv.details;

import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.Presenter;

public class CustomFullWidthDetailsOverviewRowPresenter extends FullWidthDetailsOverviewRowPresenter {

    private int mPreviousState = STATE_FULL;

    CustomFullWidthDetailsOverviewRowPresenter(Presenter detailsPresenter) {
        super(detailsPresenter);
        setInitialState(STATE_FULL);
    }

    @Override
    protected void onLayoutLogo(ViewHolder viewHolder, int oldState, boolean logoChanged) {
        View v = viewHolder.getLogoViewHolder().view;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

        lp.setMarginStart(v.getResources().getDimensionPixelSize(
                androidx.leanback.R.dimen.lb_details_v2_logo_margin_start));
        lp.topMargin = v.getResources().getDimensionPixelSize(androidx.leanback.R.dimen.lb_details_v2_blank_height) - lp.height / 2;

        float offset = v.getResources().getDimensionPixelSize(androidx.leanback.R.dimen.lb_details_v2_actions_height) + v
                .getResources().getDimensionPixelSize(androidx.leanback.R.dimen.lb_details_v2_description_margin_top) + (lp.height / 2);

        switch (viewHolder.getState()) {
            case STATE_FULL:
            default:
                if (mPreviousState == STATE_HALF) {
                    v.animate().translationYBy(-offset);
                }

                break;
            case STATE_HALF:
                if (mPreviousState == STATE_FULL) {
                    v.animate().translationYBy(offset);
                }

                break;
        }
        mPreviousState = viewHolder.getState();
        v.setLayoutParams(lp);
    }
}
