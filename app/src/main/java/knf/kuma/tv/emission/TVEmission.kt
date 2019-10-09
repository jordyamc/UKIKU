package knf.kuma.tv.emission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import knf.kuma.tv.TVBaseActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVEmission : TVBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragment(TVEmissionFragment())
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(Intent(context, TVEmission::class.java))
        }
    }
}
