package knf.kuma.faq

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.recycler_faq.*
import org.jetbrains.anko.toast

class FaqActivity : GenericActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_faq)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "FAQ"
        toolbar.setNavigationOnClickListener { onBackPressed() }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = FaqAdapter(createFAQList())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bug, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        MaterialDialog(this).safeShow {
            title(text = "Reportar problema")
            listItems(items = listOf("Telegram", "Facebook", "Email")) { _, index, _ ->
                when (index) {
                    0 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/unbarredstream")))
                    1 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/ukikuapp/")))
                    2 -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:?subject=Problema con UKIKU&to=jordyamc@hotmail.com"))
                        val chooser = Intent.createChooser(intent, "Enviar reporte")
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(chooser)
                        } else
                            toast("No se encontraron clientes de email")
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createFAQList(): List<FaqItem> =
            listOf(
                    FaqItem("¿Dónde está el servidor natsuki?", "Los servidores vienen de Animeflv, la app no tiene el control de los que aparecen, tan solo extrae los que están disponibles"),
                    FaqItem("¿Por qué no me funcionan los servidores?", "Los servidores vienen de Animeflv, son ellos los encargados de re subir los enlaces caídos, la app no puede hacer nada"),
                    FaqItem("¿Para que funcionan las loli-coins?", "Sirven para comprar los pasos del easter egg y revelar logros ocultos"),
                    FaqItem("¿Por qué me dice error 403?", "Solo presiona la barra roja que aparece"),
                    FaqItem("¿Por qué no está este anime en la app?", "La app utiliza Animeflv para obtener la lista de animes, si no te aparece es porque, o no esta en Animeflv o no estas escribiendo el nombre correctamente"),
                    FaqItem("¿Puedo pedir animes?", "No, la app utiliza Animeflv, no subimos los animes"),
                    FaqItem("¿Por qué no me aparece el botón de añadir a favoritos?", "Es un error muuuuuuy raro, nunca se pudo encontrar una solución, en ese caso puedes añadir y quitar animes a favoritos haciendo click largo en la imagen del anime"),
                    FaqItem("¿Se puede hacer Cast?", "Si, la app soporta transmisión por CAST, aparecerá un icono en la parte superior"),
                    FaqItem("¿Por qué no puedo hacer Cast con capítulos Online?", "Si tu TV es Samsung entonces es imposible, la TV bloquea los intentos por usar CAST, en ese caso puedes descargar el capítulo y hacer CAST local"),
                    FaqItem("¿Que significa Cloudflare activado?", "Es una protección de la página de Animeflv, la app debería poder pasar esa protección automáticamente"),
                    FaqItem("¿Puedo adelantar el OP y ED?", "Si, en el reproductor interno hay un botón para saltar 1:30m (Lo que por lo regular dura un OP o ED)"),
                    FaqItem("¿Se le puede hacer PiP (ventana flotante) a un capítulo?", "Si, pero solo es compatible con Android 8.1 o superior"),
                    FaqItem("¿Por qué me va muy lenta las descargas?", "Esto podría ser por varios factores, tu internet, la velocidad de escritura (mientras más ocupado este el dispositivo más tardará), el servidor que estés usando para la descarga"),
                    FaqItem("¿Hay forma de ver una serie de corrido (ver el siguiente cap sin regresar a los caps)?", "Si, puedes añadir los capítulos a la cola y verlos todos"),
                    FaqItem("¿Por qué se me reinicia el capítulo cuando contesto mensajes (salir y entrar a la app)?", "Algunos dispositivos no muy potentes necesitan \"matar\" las aplicaciones en segundo plano para ahorrar memoria, esto sumado a que no todos los servidores soportan el adelantar videos"),
                    FaqItem("¿Puedo añadir un sonido personalizado a las notificaciones?", "Si, hay una opción en configuraciones para ello"),
                    FaqItem("¿Qué pasará si animeflv deja de existir?", "Se considerara cambiar de página o crear una app desde 0"),
                    FaqItem("¿Puedo guardar mis animes en \"favoritos\" y \"siguiendo\"?", "Si, las dos secciones son independientes"),
                    FaqItem("¿Cómo puedo ayudar a la app?", "Puedes activar los anuncios desde configuracion, ver anuncios de video, donar mediante Paypal, o haciendote Patreon"),
                    FaqItem("¿Para qué sirven los logros?", "Para divertirte, se añadieron para que los usuarios tuvieran un objetivo aparte de ver anime"),
                    FaqItem("¿Cómo puedo cambiar de color la app?", "Debes resolver el easter egg"),
                    FaqItem("¿Como puedo reportar un error?", "Mediante la página de facebook, o mandando un mensaje al desarrollador vía Telegram o email"),
                    FaqItem("¿Que es el modo family friendly?", "Este modo inhabilita los animes con genero ecchi"),
                    FaqItem("¿Por qué al abrir la app me dice error de conexión (tiempo de conexión)?", "Animeflv podria estar lento, esto suele solucionarse después de unos minutos"),
                    FaqItem("¿Cómo puedo contactar al desarrollador?", "Mediante la página de facebook, en Telegram como @UnbarredStream, o al email jordyamc@hotmail.com"),
                    FaqItem("¿Ella en verdad me ama?", "NO")

            )

    companion object {
        fun open(context: Context) = context.startActivity(Intent(context, FaqActivity::class.java))
    }
}