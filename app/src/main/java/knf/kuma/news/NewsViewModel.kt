package knf.kuma.news

import androidx.lifecycle.ViewModel

class NewsViewModel : ViewModel(){
    var selectedFilter = 0

    val filtersList: List<String> = mutableListOf<String>().apply{
        add("Todos")
        add("Anime")
        add("Cine")
        add("Cultura Otaku")
        add("Japón")
        add("Live Action")
        add("Manga")
        add("Mercancía Anime")
        add("Música")
        add("Novelas Ligeras")
        add("VideoJuegos")
        add("Reseñas Episodios")
        add("Eventos")
    }
}