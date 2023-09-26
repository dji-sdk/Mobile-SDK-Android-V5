package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.MEDIA_FILE_DETAILS_STR
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sampleV5.modulecommon.models.MediaVM
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.DJIList
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.MediaManager
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_media_page.*
import java.util.ArrayList

/**
 * @author feel.feng
 * @time 2022/04/19 5:04 下午
 * @description:  回放下载操作界面
 */
class MediaFragment : DJIFragment() {
    private val mediaVM: MediaVM by activityViewModels()
    var adapter: MediaListAdapter? = null

    private var isload = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_media_page, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    private fun initData() {

        if (!isload) {
            mediaVM.init()
            isload = true
        }
        adapter = MediaListAdapter(mediaVM.mediaFileListData.value?.data!!, ::onItemClick)
        media_recycle_list.adapter = adapter
        mediaVM.mediaFileListData.observe(viewLifecycleOwner) {
            adapter!!.notifyDataSetChanged()
            tv_list_count.text = "Count:${it.data.size}"
        }


        mediaVM.fileListState.observe(viewLifecycleOwner) {
            if (it == MediaFileListState.UPDATING) {
                fetch_progress?.visibility = View.VISIBLE
            } else {
                fetch_progress?.visibility = View.GONE
            }

            tv_get_list_state?.text = "State:\n ${it.name}"
        }

        mediaVM.isPlayBack.observe(viewLifecycleOwner) {
            tv_playback.text = "isPlayingBack : ${it}"
        }

    }

    private fun initView() {
        media_recycle_list.layoutManager = GridLayoutManager(context, 3)
        btn_delete.setOnClickListener {
            val mediafiles = ArrayList<MediaFile>()
            if (adapter?.getSelectedItems()?.size != 0) {
                mediafiles.addAll(adapter?.getSelectedItems()!!)
                MediaDataCenter.getInstance().mediaManager.deleteMediaFiles(mediafiles, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        clearSelectFiles()
                        ToastUtils.showToast("delete success ");
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("delete failed  " + error.description());
                    }

                })
            } else {
                ToastUtils.showToast("please select files ");
            }

        }
        btn_refresh_file_list.setOnClickListener() {
            val fetchCount = if (TextUtils.isEmpty(fetchCount.text.toString())) {
                -1  //all
            } else {
                fetchCount.text.toString().toInt()
            }

            val mediaFileIndex = if (TextUtils.isEmpty(mediaIndex.text.toString())) {
                -1 //start fetch index
            } else {
                mediaIndex.text.toString().toInt()
            }
            mediaVM.pullMediaFileListFromCamera(mediaFileIndex, fetchCount)
        }

        btn_select.setOnClickListener {
            if (adapter == null) {
                return@setOnClickListener
            }
            adapter?.selectionMode = !adapter?.selectionMode!!
            clearSelectFiles()
            btn_select.setText(
                if (adapter?.selectionMode!!) {
                    R.string.unselect_files
                } else {
                    R.string.select_files
                }
            )
            updateDeleteBtn(adapter?.selectionMode!!)
        }

        btn_set_xmp_custom_info.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "xmp custom info",
                "MSDK_V5", "", false
            ) {
                it?.let {
                    mediaVM.setMediaFileXMPCustomInfo(it)
                }
            }
        }

        btn_get_xmp_custom_info.setOnClickListener {
            mediaVM.getMediaFileXMPCustomInfo()
        }

        sp_choose_component.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                mediaVM.setComponentIndex(ComponentIndexType.find(index))
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }

        sp_choose_storage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                mediaVM.setStorage(CameraStorageLocation.find(index))
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }

        btn_enable_playback.setOnClickListener {
            mediaVM.enable()
        }

        btn_disable_playback.setOnClickListener {
            mediaVM.disable()
        }

        btn_take_photo.setOnClickListener {
            mediaVM.takePhoto(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("take photo success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("take photo failed")
                }
            })
        }
    }

    private fun updateDeleteBtn(enable: Boolean) {
        btn_delete.isEnabled = enable
    }

    private fun clearSelectFiles() {
        adapter?.mSelectedItems?.clear()
        adapter?.notifyDataSetChanged()
    }

    private fun onItemClick(mediaFile: MediaFile, view: View) {

        ViewCompat.setTransitionName(view, mediaFile.fileName);

        val extra = FragmentNavigatorExtras(
            view to "tansitionImage"
        )

        Navigation.findNavController(view).navigate(
            R.id.media_details_page, bundleOf(
                MEDIA_FILE_DETAILS_STR to mediaFile
            ), null, extra
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        MediaDataCenter.getInstance().mediaManager.stopPullMediaFileListFromCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaVM.destroy()
        adapter = null
    }
}