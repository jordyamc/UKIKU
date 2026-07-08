package knf.kuma.tv.details

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.DetailsOverviewLogoPresenter

class CustomLogoPresenter : DetailsOverviewLogoPresenter() {
    override fun onCreateView(parent: ViewGroup): View {
        val viewHolder = super.onCreateView(parent)
        val imageView = viewHolder as ImageView

        // Stretches the image while maintaining aspect ratio,
        // or use ScaleType.FIT_CENTER depending on your needs.
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        // Force the image to use the full bounds of the overview
        /*imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )*/
        return viewHolder
    }
}