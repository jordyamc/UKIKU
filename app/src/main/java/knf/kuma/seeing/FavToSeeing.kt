package knf.kuma.seeing

import knf.kuma.commons.noCrashLet
import knf.kuma.pojos.SeenObject

object FavToSeeing {

    fun getLast(list: List<SeenObject>): SeenObject? =
        list.maxByOrNull {
            noCrashLet(-1.0) {
                "(\\d+\\.?\\d?)".toRegex().findAll(it.number).last().destructured.component1()
                    .toDouble()
            }
        }
}