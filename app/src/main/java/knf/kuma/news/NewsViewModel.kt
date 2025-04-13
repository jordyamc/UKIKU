package knf.kuma.news

import androidx.lifecycle.ViewModel

class NewsViewModel : ViewModel(){
    var selectedFilter = 0

    val filtersList: List<String> = mutableListOf<String>().apply{
        add("Recientes")
        add("Anime")
        add("Cultura Otaku")
        add("Japón")
        add("Live Action")
        add("Manga")
        add("Mercancía / Figuras")
        add("Novelas Ligeras")
        add("VideoJuegos")
        add("Reseñas")
    }
}