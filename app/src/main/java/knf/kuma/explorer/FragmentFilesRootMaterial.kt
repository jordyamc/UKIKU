package knf.kuma.explorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.R
import knf.kuma.commons.safeShow
import knf.kuma.pojos.ExplorerObject
import xdroid.toaster.Toaster


class FragmentFilesRootMaterial : FragmentBase(), FragmentFilesMaterial.SelectedListener, FragmentChaptersMaterial.ClearInterface, ExplorerCreator.EmptyListener {

    private var files: FragmentFilesMaterial = FragmentFilesMaterial[this]
    private val chapters: FragmentChaptersMaterial = FragmentChaptersMaterial[this]
    private var isFiles = true
    private var name: String? = null
    private var stateChange: OnFileStateChange? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_explorer_files, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val transaction = childFragmentManager.beginTransaction()
        if (!files.isAdded)
            transaction.add(R.id.root, files, FragmentFiles.TAG)
        if (!chapters.isAdded)
            transaction.add(R.id.root, chapters, FragmentChapters.TAG)
        transaction.commit()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setFragment(isFiles: Boolean, explorerObject: ExplorerObject?) {
        stateChange?.onChange(isFiles)
        this.isFiles = isFiles
        this.name = explorerObject?.name
        ExplorerCreator.IS_FILES = isFiles
        ExplorerCreator.FILES_NAME = explorerObject
        val transaction = childFragmentManager.beginTransaction()
        if (isFiles) {
            transaction.hide(chapters)
            transaction.show(files)
        } else {
            chapters.setObject(explorerObject)
            transaction.hide(files)
            transaction.show(chapters)
        }
        transaction.setCustomAnimations(R.anim.fadein, R.anim.fadeout)
        transaction.commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            this.isFiles = savedInstanceState.getBoolean("isFiles", true)
            this.name = savedInstanceState.getString("name")
        }
        setFragment(ExplorerCreator.IS_FILES, ExplorerCreator.FILES_NAME)
        if (!ExplorerCreator.IS_CREATED)
            ExplorerCreator.start(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isFiles", isFiles)
        outState.putString("name", name)
    }

    internal fun setStateChange(stateChange: OnFileStateChange?) {
        this.stateChange = stateChange
    }

    internal fun onRemoveAll() {
        if (name != null)
            activity?.let {
                MaterialDialog(it).safeShow {
                    message(text = "¿Eliminar todos los capitulos de $name?")
                    positiveButton(text = "Eliminar") { chapters.deleteAll() }
                    negativeButton(text = "Cancelar")
                }
            }
        else
            Toaster.toast("Error al borrar episodios")
    }

    override fun onSelected(explorerObject: ExplorerObject) {
        setFragment(false, explorerObject)
    }

    override fun onClear() {
        setFragment(true, null)
    }

    override fun onEmpty() {
        files.onEmpty()
    }

    override fun onBackPressed(): Boolean {
        return if (isFiles) {
            false
        } else {
            setFragment(true, null)
            true
        }
    }

    companion object {

        fun get(): FragmentFilesRootMaterial {
            return FragmentFilesRootMaterial()
        }
    }
}
