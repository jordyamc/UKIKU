package knf.kuma.tv.sections

import android.content.Context
import android.content.Intent
import knf.kuma.R
import knf.kuma.tv.directory.TVDir
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class DirSection : SectionObject() {
    override val image: Int
        get() = R.drawable.ic_directory_not

    override val title: String
        get() = "Directorio"

    override fun open(context: Context?) {
        context?.startActivity(Intent(context, TVDir::class.java))
    }
}