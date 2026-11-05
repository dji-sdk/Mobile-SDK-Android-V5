package com.dji.open.pilot.plugin.sampleitem.filetransfer

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityFileTransferDemoBinding
import dji.open.pilot.base.manager.file.FileTransferDestination
import java.io.File

class FileTransferDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityFileTransferDemoBinding
    private val viewModel: FileTransferDemoViewModel by globalViewModels()

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::copyAndUpload)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileTransferDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
        setupActions()
    }

    private fun setupObservers() {
        viewModel.selectedDestination.observe(this) { destination ->
            binding.btnDestination.text = getString(
                R.string.file_transfer_selected_destination,
                destination.name
            )
        }
        viewModel.toastMessage.observe(this) { message ->
            if (message.isNullOrEmpty()) {
                return@observe
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
        viewModel.isOperating.observe(this) { operating ->
            listOf(binding.btnDestination, binding.btnUpload).forEach { view ->
                view.isEnabled = !operating
                view.alpha = if (operating) 0.5f else 1f
            }
        }
    }

    private fun setupActions() {
        binding.tvBack.setOnClickListener { finish() }
        binding.btnDestination.setOnClickListener {
            val destinations = FileTransferDestination.values()
            val selectedIndex = destinations.indexOf(viewModel.selectedDestination.value).coerceAtLeast(0)
            initPopupNumberPicker(destinations.map { it.name }, selectedIndex) { index ->
                viewModel.selectDestination(destinations[index])
            }
        }
        binding.btnUpload.setOnClickListener { filePicker.launch("*/*") }
    }

    private fun initPopupNumberPicker(
        items: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(this)
            .setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, which ->
                onSelected(which)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun copyAndUpload(uri: Uri) {
        try {
            val displayName = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            val fileName = File(displayName ?: "upload_file").name.ifBlank { "upload_file" }
            val cacheDirectory = File(cacheDir, "file-transfer").apply { mkdirs() }
            val cacheFile = File(cacheDirectory, fileName)
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open selected file" }
                cacheFile.outputStream().use(input::copyTo)
            }
            viewModel.upload(cacheFile.absolutePath, fileName)
        } catch (e: Exception) {
            Toast.makeText(this, "Read file failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
