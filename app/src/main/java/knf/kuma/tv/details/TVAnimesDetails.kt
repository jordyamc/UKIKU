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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVAnimesDetails : TVBaseActivity(), TVServersFactory.ServersInterface {
    private var fragment: TVAnimesDetailsFragment? = null
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragment = TVAnimesDetailsFragment[intent.getStringExtra(keyUrl) ?: ""]
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
                serversFactory?.analyzeMulti(bundle?.getInt(keyPosition, 0) ?: 0)
            else {
                if (bundle?.getBoolean(keyIsVideoServer, false) == true)
                    serversFactory?.analyzeOption(bundle.getInt(keyPosition, 0))
                else
                    serversFactory?.analyzeServer(bundle?.getInt(keyPosition, 0) ?: 0)
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data?.extras?.getBoolean(keyIsVideoServer, false) == true)
            serversFactory?.showServerList()
    }

    companion object {

        private const val keyPosition = "position"
        private const val keyUrl = "url"
        private const val keyIsVideoServer = "is_video_server"

        fun start(context: Context, url: String?) {
            url ?: return
            context.startActivity(Intent(context, TVAnimesDetails::class.java).putExtra(keyUrl, url))
        }
    }
}
