/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.training.simulatorcontrol.preset

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dji.v5.ux.R
import dji.v5.ux.training.util.SimulatorPresetUtils
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget

/**
 * Preset List Dialog
 *
 * Dialog shows a list of saved presets.
 * Tapping on the preset name will load the values saved in the [SimulatorControlWidget]
 */
class PresetListDialog @JvmOverloads constructor(
        context: Context,
        private val loadPresetListener: OnLoadPresetListener,
        private val dialogHeight: Int = context.resources.getDimension(R.dimen.uxsdk_simulator_dialog_height).toInt()
) : Dialog(context), View.OnClickListener {

    //region Fields
    private val presetListContainerLinearLayout: LinearLayout
    private val emptyPresetListTextView: TextView
    private val confirmDeleteTextView: TextView
    private val cancelDialogTextView: TextView
    private val deletePresetYesTextView: TextView
    private val deletePresetNoTextView: TextView
    private var keyToDelete: String? = null
    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawableResource(R.drawable.uxsdk_background_dialog_rounded_corners)
    }

    init {

        setContentView(R.layout.uxsdk_dialog_simulator_load_preset)
        presetListContainerLinearLayout = findViewById(R.id.linear_layout_preset_list_container)
        emptyPresetListTextView = findViewById(R.id.textview_empty_list)
        confirmDeleteTextView = findViewById(R.id.textview_confirmation_delete)
        cancelDialogTextView = findViewById(R.id.textview_cancel_simulator_dialog)
        deletePresetYesTextView = findViewById(R.id.textview_delete_yes)
        deletePresetNoTextView = findViewById(R.id.textview_delete_no)
        cancelDialogTextView.setOnClickListener(this)
        deletePresetYesTextView.setOnClickListener(this)
        deletePresetNoTextView.setOnClickListener(this)
        resetListUI()
        setCancelable(true)
    }

    override fun onClick(v: View) {
        if (v is TextView) {
            when {
                v.getId() == R.id.textview_cancel_simulator_dialog -> dismiss()
                v.getId() == R.id.textview_delete_yes -> deletePreset()
                v.getId() == R.id.textview_delete_no -> resetListUI()
                else -> sendPresetEvent(v.getTag() as String)
            }
        } else {
            showDeleteConfirmation(v.tag as String)
        }
    }

    //endregion

    //region private methods
    private fun populatePresetList() {
        presetListContainerLinearLayout.removeAllViews()
        val simulatorPresetList = SimulatorPresetUtils.presetList
        if (simulatorPresetList.isNotEmpty()) {
            emptyPresetListTextView.visibility = View.GONE
            presetListContainerLinearLayout.visibility = View.VISIBLE
            for ((key, value) in simulatorPresetList) {
                insertView(key, value as String)
            }
        } else {
            emptyPresetListTextView.visibility = View.VISIBLE
            presetListContainerLinearLayout.visibility = View.GONE
        }
        if (window != null) {
            window?.setLayout(context.resources.getDimension(R.dimen.uxsdk_simulator_dialog_width).toInt(),
                    dialogHeight)
        }

    }

    private fun resetListUI() {
        keyToDelete = null
        confirmDeleteTextView.visibility = View.GONE
        deletePresetYesTextView.visibility = View.GONE
        presetListContainerLinearLayout.visibility = View.VISIBLE
        deletePresetNoTextView.visibility = View.GONE
        cancelDialogTextView.visibility = View.VISIBLE
        populatePresetList()
    }

    private fun deletePreset() {
        SimulatorPresetUtils.deletePreset(keyToDelete)
        resetListUI()
    }

    private fun showDeleteConfirmation(key: String) {
        keyToDelete = key
        confirmDeleteTextView.visibility = View.VISIBLE
        deletePresetYesTextView.visibility = View.VISIBLE
        presetListContainerLinearLayout.visibility = View.GONE
        deletePresetNoTextView.visibility = View.VISIBLE
        cancelDialogTextView.visibility = View.GONE
        confirmDeleteTextView.text = context.resources
                .getString(R.string.uxsdk_simulator_save_preset_delete, key)
        if (window != null) {
            window?.setLayout(context.resources.getDimension(R.dimen.uxsdk_simulator_dialog_width).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun sendPresetEvent(data: String?) {
        if (data != null) {
            val dataParts = data.split(" ").toTypedArray()
            val latitude = dataParts[0].toDouble()
            val longitude = dataParts[1].toDouble()
            val satelliteCount = dataParts[2].toInt()
            val frequency = dataParts[3].toInt()
            loadPresetListener.onLoadPreset(SimulatorPresetData(latitude,
                    longitude,
                    satelliteCount,
                    frequency))
        } else {
            Toast.makeText(context,
                    context.resources.getString(R.string.uxsdk_simulator_preset_error),
                    Toast.LENGTH_SHORT).show()
        }
        dismiss()
    }

    @SuppressLint("InflateParams")
    private fun insertView(key: String, value: String) {
        val presetRow = LayoutInflater.from(context).inflate(R.layout.uxsdk_layout_simulator_preset_row, null)
        presetListContainerLinearLayout.addView(presetRow, presetListContainerLinearLayout.childCount)
        val presetNameTextView = presetRow.findViewById<TextView>(R.id.textview_preset_name)
        presetNameTextView.text = key
        presetNameTextView.tag = value
        presetNameTextView.setOnClickListener(this)
        val deleteImage = presetRow.findViewById<ImageView>(R.id.imageview_preset_delete)
        deleteImage.tag = key
        deleteImage.setOnClickListener(this)
    } //endregion

}