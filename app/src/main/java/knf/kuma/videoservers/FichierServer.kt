package knf.kuma.videoservers

import android.content.Context

class FichierServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("1fichier")

    override val name: String
        get() = "1Fichier (WEB)"

    override val canStream: Boolean
        get() = false

    override val canDownload: Boolean
        get() = true

    override val videoServer: VideoServer
        get() {
            return VideoServer(name, Option(name, null, baseLink, needTabs = true), skipVerification = true)
        }
}
