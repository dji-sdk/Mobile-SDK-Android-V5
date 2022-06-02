package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import dji.sampleV5.modulecommon.R

import dji.sampleV5.modulecommon.models.MediaVM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaManager


/**
 * @author feel.feng
 * @time 2022/04/19 5:04 下午
 * @description: 回放下载操作界面
 */
class MediaMainFragment : DJIFragment() {
    private val mediaVM: MediaVM by activityViewModels()
    var adapter: MediaListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_media_main_page, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}