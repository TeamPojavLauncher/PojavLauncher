package net.kdt.pojavlaunch.prefs.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.mobileglues.MgController
import net.kdt.pojavlaunch.mobileglues.MgDialogHost
import kotlinx.coroutines.launch

/** Shows what the renderer reports about the GL driver, read through a throwaway query process. */
class MobileGluesGlInfoFragment : Fragment(R.layout.fragment_mg_gl_info) {

    private val controller: MgController by lazy { MgController.get(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.ensureLoaded()
        MgDialogHost(this, controller).attach()

        val text = view.findViewById<TextView>(R.id.mg_glinfo_text)
        val loading = view.findViewById<View>(R.id.mg_glinfo_loading)
        val note = view.findViewById<TextView>(R.id.mg_glinfo_angle_note)
        val borrow = view.findViewById<View>(R.id.mg_glinfo_borrow)
        val copy = view.findViewById<View>(R.id.mg_glinfo_copy)

        view.findViewById<View>(R.id.mg_back_row).setOnClickListener {
            ExtraCore.setValue(ExtraConstants.BACK_PREFERENCE, "true")
        }
        borrow.setOnClickListener { controller.reloadGlInfoWithAngle() }
        copy.setOnClickListener {
            val info = controller.glInfo.value ?: return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.info_mg_info), info))
            Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    controller.loadState.collect { state ->
                        if (state == MgController.LoadState.Ready) controller.loadGlInfo()
                    }
                }
                launch { controller.glInfo.collect { text.text = it.orEmpty(); copy.isEnabled = it != null } }
                launch { controller.glInfoLoading.collect { loading.visibility = if (it) View.VISIBLE else View.GONE } }
                launch { controller.glInfoNeedsAngle.collect { borrow.visibility = if (it) View.VISIBLE else View.GONE } }
                launch {
                    controller.glInfoAngle.collect { angle ->
                        val message = when (angle) {
                            MgController.GlInfoAngle.System -> null
                            MgController.GlInfoAngle.Borrowed -> R.string.md_glinfo_borrowed
                            MgController.GlInfoAngle.BorrowIneffective -> R.string.md_glinfo_borrow_ineffective
                        }
                        note.visibility = if (message == null) View.GONE else View.VISIBLE
                        if (message != null) note.setText(message)
                    }
                }
                launch {
                    combineNeedsAngleNote(note)
                }
            }
        }
    }

    private suspend fun combineNeedsAngleNote(note: TextView) {
        controller.glInfoNeedsAngle.collect { needs ->
            if (needs) {
                note.setText(R.string.md_glinfo_needs_angle)
                note.visibility = View.VISIBLE
            }
        }
    }
}
