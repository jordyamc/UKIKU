package knf.kuma.custom;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.GridLayoutAnimationController;

/**
 * Created by Jordy on 24/01/2018.
 */

public class GridRecyclerView extends RecyclerView {

    /** @see View#View(Context) */
    public GridRecyclerView(Context context) { super(context); }

    /** @see View#View(Context, AttributeSet) */
    public GridRecyclerView(Context context, AttributeSet attrs) { super(context, attrs); }

    /** @see View#View(Context, AttributeSet, int) */
    public GridRecyclerView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    @Override
    protected void attachLayoutAnimationParameters(View child, ViewGroup.LayoutParams params,
                                                   int index, int count) {
        final RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (getAdapter() != null && layoutManager instanceof GridLayoutManager){

            GridLayoutAnimationController.AnimationParameters animationParams =
                    (GridLayoutAnimationController.AnimationParameters) params.layoutAnimationParameters;

            if (animationParams == null) {
                // If there are no animation parameters, create new once and attach them to
                // the LayoutParams.
                animationParams = new GridLayoutAnimationController.AnimationParameters();
                params.layoutAnimationParameters = animationParams;
            }

            // Next we are updating the parameters

            // Set the number of items in the RecyclerView and the index of this item
            animationParams.count = count;
            animationParams.index = index;

            // Calculate the number of columns and rows in the grid
            final int columns = ((GridLayoutManager) layoutManager).getSpanCount();
            animationParams.columnsCount = columns;
            animationParams.rowsCount = count / columns;

            // Calculate the column/row position in the grid
            final int invertedIndex = count - 1 - index;
            animationParams.column = columns - 1 - (invertedIndex % columns);
            animationParams.row = animationParams.rowsCount - 1 - invertedIndex / columns;

        } else {
            // Proceed as normal if using another type of LayoutManager
            super.attachLayoutAnimationParameters(child, params, index, count);
        }
    }
}
