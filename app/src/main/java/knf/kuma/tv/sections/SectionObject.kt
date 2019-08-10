package knf.kuma.tv.sections

import android.content.Context

abstract class SectionObject {

    abstract val image: Int

    abstract val title: String

    abstract fun open(context: Context?)
}
