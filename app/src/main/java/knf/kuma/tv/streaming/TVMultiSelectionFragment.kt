package knf.kuma.tv.streaming

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction

class TVMultiSelectionFragment : GuidedStepSupportFragment(){
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setResult(Activity.RESULT_CANCELED, Intent())
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance("Selecciona idioma","","",null)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.apply {
            add(GuidedAction.Builder(context).id(0).title("Subtitulado").build())
            add(GuidedAction.Builder(context).id(1).title("Latino").build())
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        super.onGuidedActionClicked(action)
        activity?.setResult(Activity.RESULT_OK, Intent()
            .putExtra("position", action.id.toInt())
        )
        activity?.finish()
    }
}