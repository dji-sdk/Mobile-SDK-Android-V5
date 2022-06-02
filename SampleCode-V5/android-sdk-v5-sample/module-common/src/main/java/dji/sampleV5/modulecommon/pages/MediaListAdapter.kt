package dji.sampleV5.modulecommon.pages

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.FragmentPageInfoItem
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.utils.common.ContextUtil
import kotlinx.android.synthetic.main.item_mediafile_list.view.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

/**
 * @author feel.feng
 * @time 2022/04/20 11:16 上午
 * @description:
 */
class MediaListAdapter(
    val data: List<MediaFile>,
    val context: Context?,
    private val onClick: (MediaFile, View) -> Unit
) :
    RecyclerView.Adapter<MediaListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_mediafile_list, parent, false)
        return ViewHolder(view, onClick)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val mediaFile = data.get(position)
        holder.imageView.setTag(position)
        holder.setItemData(mediaFile)
        holder.imageView.setImageDrawable(
            ContextCompat.getDrawable(
                ContextUtil.getContext(),
                R.drawable.ic_media_play
            )
        )
        if (mediaFile.thumbNail != null) {
            holder.imageView.setImageBitmap(mediaFile.thumbNail)
        } else {
            mediaFile.pullThumbnailFromCamera(object : CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
                override fun onSuccess(t: Bitmap?) {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        if (holder.imageView.tag == position) {
                            holder.imageView.setImageBitmap(t)
                        }
                    }

                }

                override fun onFailure(error: IDJIError) {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        holder.imageView.setImageDrawable(
                            ContextCompat.getDrawable(
                                ContextUtil.getContext(),
                                R.drawable.aircraft
                            )
                        )
                    }

                }

            })
        }


        holder.textView.text = mediaFile.fileName.toString()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(view: View, private val onItemClick: (MediaFile, View) -> Unit) :
        RecyclerView.ViewHolder(view) {
        var imageView = view.iv_thumbnail
        var textView = view.tv_media_info
        var mediaFile: MediaFile? = null

        init {

            view.setOnClickListener {
                mediaFile?.let {

                    onItemClick(it, view)
                }
            }
        }

        fun setItemData(data: MediaFile) {
            mediaFile = data
        }

    }

}