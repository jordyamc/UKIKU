package knf.kuma.custom

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice

class ListPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    internal /* synthetic access */ var mClickedDialogEntryIndex: Int = 0
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference

            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                        "ListPreference requires an entries array and an entryValues array.")
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setSingleChoiceItems(
            mEntries, mClickedDialogEntryIndex
        ) { dialog, which ->
            mClickedDialogEntryIndex = which

            // Clicking on an item simulates the positive button click, and dismisses
            // the dialog.
            this@ListPreferenceDialogFragmentCompat.onClick(
                dialog,
                DialogInterface.BUTTON_POSITIVE
            )
            dialog.dismiss()
        }

        // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
        // dialog instead of the user having to press 'Ok'.
        builder.setPositiveButton(null, null)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            try {
                val entryList = mutableListOf<String>()
                mEntries?.forEach { entry ->
                    entryList.add(entry.toString())
                }
                MaterialDialog(it).apply {
                    lifecycleOwner()
                    title(text = preference.dialogTitle.toString())
                    listItemsSingleChoice(items = entryList, initialSelection = mClickedDialogEntryIndex, waitForPositiveButton = false) { dialog, index, _ ->
                        mClickedDialogEntryIndex = index
                        this@ListPreferenceDialogFragmentCompat.onClick(dialog,
                                DialogInterface.BUTTON_POSITIVE)
                        dialog.dismiss()
                    }
                    positiveButton(android.R.string.ok)
                }
            } catch (e: Exception) {
                super.onCreateDialog(savedInstanceState)
            }
        } ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = mEntryValues!![mClickedDialogEntryIndex].toString()
            val preference = listPreference
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    companion object {

        private const val SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "ListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): ListPreferenceDialogFragmentCompat {
            val fragment = ListPreferenceDialogFragmentCompat()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

}

