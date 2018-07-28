![](https://github.com/jordyamc/UKIKU/blob/master/web/img/UKIKU%20Facebook.png)

## ¿Qué es UKIKU?

Es una app derivada de mi antiguo proyecto [Animeflv App](https://github.com/jordyamc/Animeflv), tiene casi las mismas funciones, pero hecha desde cero para dispositivos con Android 5 o superior, esta pensada para tener mejor rendimiento y optimización, así como menor tamaño de [APK](https://github.com/jordyamc/UKIKU/raw/master/app/release/app-release.apk) (~6MB)

## ¿Por que un proyecto tan grande no tiene publicidad y es código abierto?

Soy un programador por hobby, no tengo experiencia profesional en la programación, pero aun así decidí aprender por mi cuenta, mis proyectos están hechos con el propósito de aprender a programar en el camino, aun con todo ese tiempo invertido quise que mis apps sean hechas por un fan para fans de anime, me gusta que la comunidad se apoye entre si, así que decidí no lucrar con ello, pero aun así las donaciones son muy bien recibidas ;)

## ¿Como puedo adaptar la app a otras paginas?

Esta app esta hecha para funcionar 100% con [Animeflv](https://animeflv.net), sin embargo tiene arquitectura [MVVM-Live](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) lo que significa que el modulo de datos y UI estan separados lo que facilita un poco el proceso.

Para la extraccion de datos uso la libreria [Jspoon](https://github.com/DroidsOnRoids/jspoon) la cual combina la obtencion de datos con [Retrofit](http://square.github.io/retrofit/) con el parseo de HTML por CSS Selector de [Jsoup](https://jsoup.org/cookbook/extracting-data/selector-syntax), esto facilita las cosas ya que al obtener los datos nos devolvera la infromacion en forma de objeto.

Estos objetos los hize compatibles con el framework de Google [Room](https://developer.android.com/training/data-storage/room/) el cual crea tablas en una base de datos con el esquema del objeto, nos deja obtener los datos en forma de objeto, y con el codigo autogenerado nos ahorra tiempo.

Ya con todo el esquema explicado pasaremos a explicar el codigo:

* Todos los objetos compatibles con [Room](https://developer.android.com/training/data-storage/room/) estan contenidos en una sola [carpeta](https://github.com/jordyamc/UKIKU/tree/master/app/src/main/java/knf/kuma/pojos)
* Las funciones de datos (los que crean los objetos mediante [Retrofit](http://square.github.io/retrofit/) y guardan/obtienen los datos desde la base de datos) estan dentro del [repositorio](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/retrofit/Repository.java)
* Las clases principales de datos tienen su propio ViewModel del cual se hace la llamada al [repositorio](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/retrofit/Repository.java)
  * Recientes: [RecentsViewModel](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/recents/RecentsViewModel.java)
  * Favoritos: [FavotiteViewModel](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/favorite/FavoriteViewModel.java)
  * Directorio: [DirectoryViewModel](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/directory/DirectoryViewModel.java)
  * Informacion de anime: [AnimeViewModel](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/animeinfo/AnimeViewModel.java)
  * Busqueda: [SearchViewModel](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/search/SearchViewModel.java)

y con esto empezamos con la guia de como adaptar el codigo:

#### Paso 1: Adaptar el parser para otra pagina

Para este paso usaremos como ejemplo la informacion de anime: [AnimeObject](https://github.com/jordyamc/UKIKU/blob/master/app/src/main/java/knf/kuma/pojos/AnimeObject.java)

Dentro de este objeto esta separada la clase estatica WebInfo la cual se usa con [CSS Selector](https://jsoup.org/cookbook/extracting-data/selector-syntax) para obtener los datos principales:
[AnimeObject#WebInfo](https://gist.github.com/jordyamc/d21abf7d418ee05809b7fa4562e1e29e#file-animeobject-webinfo-java)