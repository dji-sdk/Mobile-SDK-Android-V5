package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.MEDIA_FILE_DETAILS_STR
import dji.sampleV5.aircraft.databinding.FragMediaPageBinding
import dji.sampleV5.aircraft.models.MediaVM
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileListState

/**
 * @author feel.feng
 * @time 2022/04/19 5:04 下午
 * @description:  回放下载操作界面
 */
class MediaFragment : DJIFragment() {
    private val mediaVM: MediaVM by activityViewModels()
    var adapter: MediaListAdapter? = null
    private var binding: FragMediaPageBinding? = null
    private var takeNum: Int = 0

    private var isload = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragMediaPageBinding.inflate(inflater, container, false)
        return binding?.root
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
        binding?.mediaRecycleList?.adapter = adapter
        mediaVM.mediaFileListData.observe(viewLifecycleOwner) {
            adapter!!.notifyDataSetChanged()
            binding?.tvListCount?.text = "Count:${it.data.size}"
        }


        mediaVM.fileListState.observe(viewLifecycleOwner) {
            if (it == MediaFileListState.UPDATING) {
                // 禁用按钮防止重复点击
                binding?.btnTakePhoto?.isEnabled = false
                binding?.fetchProgress?.visibility = View.VISIBLE
            } else {
                binding?.fetchProgress?.visibility = View.GONE
                binding?.btnTakePhoto?.isEnabled = true
            }

            binding?.tvGetListState?.text = "State:\n ${it.name}"
        }

        mediaVM.isPlayBack.observe(viewLifecycleOwner) {
            updatePlaybackMsg()
        }

        mediaVM.componentIndex.observe(viewLifecycleOwner) {
            updatePlaybackMsg()
        }
    }

    private fun updatePlaybackMsg() {
        binding?.tvPlaybackMsg?.text = "isPlayingBack : ${mediaVM.isPlayBack.value}\nIndex : ${mediaVM.componentIndex.value}\n"
    }

    private fun initView() {
        binding?.mediaRecycleList?.layoutManager = GridLayoutManager(context, 3)
        binding?.btnDelete?.setOnClickListener {
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
        binding?.btnRefreshFileList?.setOnClickListener {
            val fetchCount = if (TextUtils.isEmpty(binding?.fetchCount?.text.toString())) {
                -1  //all
            } else {
                binding?.fetchCount?.text.toString().toInt()
            }

            val mediaFileIndex = if (TextUtils.isEmpty(binding?.mediaIndex?.text.toString())) {
                -1 //start fetch index
            } else {
                binding?.mediaIndex?.text.toString().toInt()
            }
            mediaVM.pullMediaFileListFromCamera(mediaFileIndex, fetchCount)
        }

        binding?.btnSelect?.setOnClickListener {
            if (adapter == null) {
                return@setOnClickListener
            }
            adapter?.selectionMode = !adapter?.selectionMode!!
            clearSelectFiles()
            binding?.btnSelect?.setText(
                if (adapter?.selectionMode!!) {
                    R.string.unselect_files
                } else {
                    R.string.select_files
                }
            )
            updateDeleteBtn(adapter?.selectionMode!!)
        }

        binding?.btnDownload?.setOnClickListener {
            val mediafiles = ArrayList<MediaFile>()
            if (adapter?.getSelectedItems()?.size != 0)
                mediafiles.addAll(adapter?.getSelectedItems()!!)
            mediaVM.downloadMediaFile(mediafiles)

        }

        binding?.btnSetXmpCustomInfo?.setOnClickListener {
            dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showInputDialog(
                activity, "xmp custom info",
                "MSDK_V5", "", false
            ) {
                it?.let {
                    mediaVM.setMediaFileXMPCustomInfo(it)
                }
            }
        }

        binding?.btnGetXmpCustomInfo?.setOnClickListener {
            mediaVM.getMediaFileXMPCustomInfo()
        }

        binding?.spChooseComponent?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                val selectedItem = parent?.getItemAtPosition(index).toString()
                mediaVM.setComponentIndex(ComponentIndexType.valueOf(selectedItem))
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }

        binding?.spChooseStorage?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                mediaVM.setStorage(CameraStorageLocation.find(index))
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }

        binding?.btnEnablePlayback?.setOnClickListener {
            mediaVM.enable()
        }

        binding?.btnDisablePlayback?.setOnClickListener {
            mediaVM.disable()
        }

        binding?.btnTakePhoto?.setOnClickListener {
            mediaVM.takePhoto(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("take photo success")
                }
                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("take photo failed $error")
                }
            })
        }
        binding?.btnFormat?.setOnClickListener {
            mediaVM.formatSDCard(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("format SDCard success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("format SDCard failed ${error.errorCode()}")
                }

            })

        }
    }

    private fun updateDeleteBtn(enable: Boolean) {
        binding?.btnDelete?.isEnabled = enable
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
        if (mediaVM.isPlayBack.value == true) {
            mediaVM.disable()
        }
        mediaVM.destroy()
        adapter = null
    }
}