package knf.kuma.slices

import androidx.core.graphics.drawable.IconCompat
import androidx.room.Ignore
import androidx.room.TypeConverters
import knf.kuma.pojos.AnimeObject
import knf.kuma.search.SearchObject

@TypeConverters(AnimeObject.Converter::class)
class AnimeSliceObject : SearchObject() {
    var genres = listOf<String>()
    @Ignore
    lateinit var icon: IconCompat

    val genresString: String
        get() {
            if (genres.isEmpty())
                return "Sin generos"
            val builder = StringBuilder()
            for (genre in genres) {
                builder.append(genre)
                        .append(", ")
            }
            val g = builder.toString()
            return g.substring(0, g.lastIndexOf(","))
        }
}