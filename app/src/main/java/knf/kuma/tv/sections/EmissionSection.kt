package knf.kuma.tv.sections

import android.content.Context
import android.content.Intent
import knf.kuma.R
import knf.kuma.tv.emission.TVEmission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class EmissionSection : SectionObject() {
    override val image: Int
        get() = R.drawable.ic_emision_not

    override val title: String
        get() = "Emisión"

    override fun open(context: Context?) {
        context?.startActivity(Intent(context, TVEmission::class.java))
    }
}