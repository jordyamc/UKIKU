package knf.kuma.tv.streaming

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import java.util.*

class TVServerSelectionFragment : GuidedStepSupportFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setResult(Activity.RESULT_CANCELED, Intent()
                .putExtra("is_video_server", arguments!!.getBoolean(IS_SERVER_DATA, false)))
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return if (arguments!!.getBoolean(IS_SERVER_DATA, false))
            GuidanceStylist.Guidance(arguments!!.getString("server_name"), "Selecciona calidad", "", null)
        else
            GuidanceStylist.Guidance("Selecciona servidor", "", "", null)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val list = arguments!!.getStringArrayList(SERVERS_DATA)
        for ((id, name) in list!!.withIndex()) {
            if (name != "Mega")
                actions.add(GuidedAction.Builder(context)
                        .id(id.toLong())
                        .title(name)
                        .build())
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction?) {
        super.onGuidedActionClicked(action)
        activity!!.setResult(Activity.RESULT_OK, Intent()
                .putExtra("is_video_server", arguments!!.getBoolean(IS_SERVER_DATA, false))
                .putExtra("position", action!!.id.toInt()))
        activity!!.finish()
    }

    companion object {

        const val VIDEO_DATA = "option_data"
        const val SERVERS_DATA = "list_data"
        const val IS_SERVER_DATA = "is_server"

        operator fun get(servers: ArrayList<String>, name: String, isServerData: Boolean): TVServerSelectionFragment {
            val fragment = TVServerSelectionFragment()
            val bundle = Bundle()
            bundle.putStringArrayList(SERVERS_DATA, servers)
            bundle.putBoolean(IS_SERVER_DATA, isServerData)
            bundle.putString("server_name", name)
            fragment.arguments = bundle
            return fragment
        }
    }


}
