package knf.kuma.iap

import android.net.Uri
import androidx.annotation.NonNull
import com.appcoins.billing.AppcoinsBilling


object PayloadHelper {
    private const val SCHEME = "appcoins"
    private const val ADDRESS_PARAMETER = "address"
    private const val PAYLOAD_PARAMETER = "payload"

    /**
     * Method to build the payload required on the [AppcoinsBilling.getBuyIntent] method.
     * @param developerAddress The developer's ethereum address
     * @param developerPayload The additional payload to be sent
     *
     * @return The final developers payload to be sent
     */
    fun buildIntentPayload(@NonNull developerAddress: String, developerPayload: String?): String {
        val builder = Uri.Builder()
        builder.scheme(SCHEME)
                .authority("appcoins.io")
                .appendQueryParameter(ADDRESS_PARAMETER, developerAddress)
        if (developerPayload != null) {
            builder.appendQueryParameter(PAYLOAD_PARAMETER, developerPayload)
        }
        return builder.toString()
    }

    /**
     * Given a uri string validate if it is part of the expected scheme and if so return the
     * developer's ethereum address.
     *
     * @param uriString The payload uri content
     *
     * @return The developers ethereum address
     */
    fun getAddress(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return if (uri.scheme.equals(SCHEME, true)) {
            uri.getQueryParameter(ADDRESS_PARAMETER)
        } else {
            throw IllegalArgumentException()
        }
    }

    /**
     * Given a uri string validate if it is part of the expected scheme and if so return the
     * addition payload content.
     *
     * @param uriString The payload uri content
     *
     * @return The additional payload content
     */
    fun getPayload(uriString: String): String {
        val uri = Uri.parse(uriString)
        return if (uri.scheme.equals(SCHEME, true)) {
            uri.getQueryParameter(PAYLOAD_PARAMETER) ?: ""
        } else {
            throw IllegalArgumentException()
        }
    }
}