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

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import dji.v5.ux.core.extension.textColor
import dji.v5.ux.core.extension.textColorStateList
import dji.v5.ux.R
import dji.v5.ux.training.util.SimulatorPresetUtils
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget

/**
 * Save Preset Dialog
 *
 * The values entered in [SimulatorControlWidget] can be saved for future simulation as preset.
 * This dialog provides a user interface to enter the name to be used for saving the preset.
 */
class SavePresetDialog (
        context: Context,
        cancelable: Boolean,
        simulatorPresetData: SimulatorPresetData
) : Dialog(context), View.OnClickListener {

    //region Fields
    private val simulatorPresetData: SimulatorPresetData
    private val titleTextView: TextView
    private val saveTextView: TextView
    private val cancelTextView: TextView
    private val presetEditText: EditText
    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawableResource(R.drawable.uxsdk_background_dialog_rounded_corners)
        window?.setLayout(context.resources.getDimension(R.dimen.uxsdk_simulator_dialog_width).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    init {
        setContentView(R.layout.uxsdk_dialog_simulator_save_preset)
        titleTextView = findViewById(R.id.textview_save_preset_header)
        presetEditText = findViewById(R.id.edit_text_preset_name)
        saveTextView = findViewById(R.id.textview_save_preset)
        saveTextView.setOnClickListener(this)
        cancelTextView = findViewById(R.id.textview_cancel_simulator_dialog)
        cancelTextView.setOnClickListener(this)

        setCancelable(cancelable)
        this.simulatorPresetData = simulatorPresetData
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.textview_save_preset) {
            savePreset(presetEditText.text.toString())
        } else if (id == R.id.textview_cancel_simulator_dialog) {
            dismiss()
        }
    }
    //endregion

    //region Helpers

    private fun savePreset(name: String) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context,
                    context.resources.getString(R.string.uxsdk_simulator_preset_name_empty_error),
                    Toast.LENGTH_SHORT).show()
        } else {
            SimulatorPresetUtils.savePreset(name, simulatorPresetData)
            dismiss()
        }
    }
    //endregion

    //region customizations

    /**
     * Text color state list of widget title
     */
    var titleTextColors: ColorStateList?
        get() = titleTextView.textColorStateList
        set(value) {
            titleTextView.textColorStateList = value
        }

    /**
     * The color of title text
     */
    var titleTextColor: Int
        get() = titleTextView.textColor
        set(value) {
            titleTextView.textColor = value
        }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setTitleTextAppearance(@StyleRes textAppearance: Int) {
        titleTextView.setTextAppearance(context, textAppearance)
    }

    /**
     *  Current background of title text
     */
    var titleBackground: Drawable?
        get() = titleTextView.background
        set(value) {
            titleTextView.background = value
        }

    /**
     * Set background to title text
     */
    fun setTitleBackground(@DrawableRes resourceId: Int) {
        titleBackground = context.resources.getDrawable(resourceId)
    }

    /**
     * Current background of button text
     */
    var buttonBackground: Drawable
        get() = saveTextView.background
        set(value) {
            saveTextView.background = value
            cancelTextView.background = value
        }

    /**
     * Set background to button text
     */
    fun setButtonBackground(@DrawableRes resourceId: Int) {
        buttonBackground = context.resources.getDrawable(resourceId)
    }

    /**
     * Color state list for button text
     */
    var buttonTextColors: ColorStateList?
        get() = saveTextView.textColorStateList
        set(value) {
            saveTextView.textColorStateList = value
            cancelTextView.textColorStateList = value
        }

    /**
     * Text color of buttons
     */
    var buttonTextColor: Int
        get() = saveTextView.textColor
        set(value) {
            saveTextView.textColor = value
            cancelTextView.textColor = value
        }

    /**
     * Set text appearance of the button
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setButtonTextAppearance(@StyleRes textAppearance: Int) {
        saveTextView.setTextAppearance(context, textAppearance)
        cancelTextView.setTextAppearance(context, textAppearance)
    }
    //endregion

}