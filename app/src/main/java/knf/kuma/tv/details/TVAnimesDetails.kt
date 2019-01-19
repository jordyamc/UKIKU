package knf.kuma.tv.details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import knf.kuma.R
import knf.kuma.commons.doOnUI
import knf.kuma.tv.TVBaseActivity
import knf.kuma.tv.TVServersFactory

class TVAnimesDetails : TVBaseActivity(), TVServersFactory.ServersInterface {
    private var fragment: TVAnimesDetailsFragment? = null
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragment = TVAnimesDetailsFragment[intent.getStringExtra("url")]
        addFragment(fragment as TVAnimesDetailsFragment)
    }

    override fun onReady(serversFactory: TVServersFactory) {
        this.serversFactory = serversFactory
    }

    override fun onFinish(started: Boolean, success: Boolean) {
        if (fragment != null && success) {
            fragment?.onStartStreaming()
            doOnUI {
                serversFactory?.viewHolder?.view?.apply {
                    findViewById<View>(R.id.indicator).visibility = View.VISIBLE
                    invalidate()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val bundle = data?.extras
            if (requestCode == TVServersFactory.REQUEST_CODE_MULTI)
                serversFactory?.analyzeMulti(bundle?.getInt("position", 0) ?: 0)
            else {
                if (bundle?.getBoolean("is_video_server", false) == true)
                    serversFactory?.analyzeOption(bundle.getInt("position", 0))
                else
                    serversFactory?.analyzeServer(bundle?.getInt("position", 0) ?: 0)
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data!!.extras!!.getBoolean("is_video_server", false))
            serversFactory?.showServerList()
    }

    companion object {

        fun start(context: Context, url: String) {
            context.startActivity(Intent(context, TVAnimesDetails::class.java).putExtra("url", url))
        }
    }
}
