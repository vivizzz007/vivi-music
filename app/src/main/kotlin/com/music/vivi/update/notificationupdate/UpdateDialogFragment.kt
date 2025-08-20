package com.music.vivi.update.notificationupdate

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.music.vivi.R

class UpdateDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_update_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val version = arguments?.getString("version") ?: ""
        val releaseNotes = arguments?.getString("release_notes") ?: ""

        view.findViewById<TextView>(R.id.versionText).text = "Version $version"
        view.findViewById<TextView>(R.id.releaseNotesText).text = releaseNotes

        view.findViewById<Button>(R.id.updateButton).setOnClickListener {
            openDownloadPage(version)
            dismiss()
        }

        view.findViewById<Button>(R.id.remindLaterButton).setOnClickListener {
            dismiss()
        }
    }

    private fun openDownloadPage(version: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/vivizzz007/vivi-music/releases/tag/v$version")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Handle error
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        }
    }

    companion object {
        fun newInstance(version: String, releaseNotes: String): UpdateDialogFragment {
            return UpdateDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("version", version)
                    putString("release_notes", releaseNotes)
                }
            }
        }
    }
}