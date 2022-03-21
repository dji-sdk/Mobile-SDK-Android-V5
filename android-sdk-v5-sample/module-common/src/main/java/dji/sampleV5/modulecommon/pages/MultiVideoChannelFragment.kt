package dji.sampleV5.modulecommon.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.models.MultiVideoChannelVM
import dji.sampleV5.modulecommon.util.ToastUtils


class MultiVideoChannelFragment : DJIFragment(), View.OnClickListener {
    private val multiVideoChannelVM: MultiVideoChannelVM by activityViewModels()
    private lateinit var primaryFragmentView: FragmentContainerView
    private lateinit var secondaryFragmentView: FragmentContainerView
    private lateinit var fabResetAllVideoChannel: FloatingActionButton
    private var isFABOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.frag_multi_video_channel_page, container, false)
        primaryFragmentView = view.findViewById(R.id.primary_video_channel_fragment)
        secondaryFragmentView = view.findViewById(R.id.secondary_video_channel_fragment)
        val fab = view.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener(this)
        fabResetAllVideoChannel =
            view.findViewById(R.id.fab_reset_all_video_channel) as FloatingActionButton
        fabResetAllVideoChannel.setOnClickListener(this)
        return view
    }

    @SuppressLint("RestrictedApi")
    private fun showFABMenu() {
        isFABOpen = true
        fabResetAllVideoChannel.visibility = View.VISIBLE
    }

    @SuppressLint("RestrictedApi")
    private fun closeFABMenu() {
        isFABOpen = false
        fabResetAllVideoChannel.visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        multiVideoChannelVM.initMultiVideoChannels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        multiVideoChannelVM.removeStreamSourceListener()
    }



    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab -> {
                if (!isFABOpen) {
                    showFABMenu()
                } else {
                    closeFABMenu()
                }
            }
            R.id.fab_reset_all_video_channel -> {
                multiVideoChannelVM.resetMultiVideoChannels()
                this@MultiVideoChannelFragment.childFragmentManager.setFragmentResult(
                    "ResetAllVideoChannel",
                    bundleOf("ResetAllVideoChannel" to true)
                )
                this@MultiVideoChannelFragment.context?.let {
                    ToastUtils.showToast(
                        it,
                        "Reset All Video Channel Success"
                    )
                }
            }
        }
    }
}