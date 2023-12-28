package dji.sampleV5.aircraft.pages

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.recyclerview.widget.RecyclerView

import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.util.AnimationUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.utils.common.ContextUtil
import kotlinx.android.synthetic.main.item_mediafile_list.view.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.util.ArrayList

/**
 * @author feel.feng
 * @time 2022/04/20 11:16 上午
 * @description:
 */
class MediaListAdapter(
    val data: List<MediaFile>,
    private val onClick: (MediaFile, View) -> Unit
) :
    RecyclerView.Adapter<MediaListAdapter.ViewHolder>() {

    val mSelectedItems: ArrayList<MediaFile> = ArrayList<MediaFile>()
    var selectionMode : Boolean = false

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


        holder.textView.text = mediaFile.fileName.toString() + ":${mediaFile.fileIndex}"
        holder.updateSelection(selectionMode, mSelectedItems.contains(mediaFile))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(view: View, private val onItemClick: (MediaFile, View) -> Unit) :
        RecyclerView.ViewHolder(view) {
        var imageView = view.iv_thumbnail
        var textView = view.tv_media_info
        var mediaFile: MediaFile? = null
        var rightTopIcon = view.right_top_icon

        init {

            imageView.setOnClickListener {

                mediaFile?.let {

                    if (selectionMode) {
                        holderClickAction(this, mediaFile!!)
                    } else {
                        onItemClick(it, view)
                    }


                }
            }
        }

        fun setItemData(data: MediaFile) {
            mediaFile = data
        }

        fun updateSelection(selectionMode: Boolean, selected: Boolean) {
            rightTopIcon.setVisibility(if (selectionMode) View.VISIBLE else View.GONE)
            rightTopIcon.setSelected(selected)
            AnimationUtils.springView(rightTopIcon, DynamicAnimation.SCALE_X, 1.1f)
            AnimationUtils.springView(rightTopIcon, DynamicAnimation.SCALE_Y, 1.1f)
        }

    }


     fun holderClickAction(
         holder: ViewHolder,
         media: MediaFile
    ) {

         if (selectionMode) {
             if (mSelectedItems.contains(holder.mediaFile)) {
                 mSelectedItems.remove(holder.mediaFile)
             } else {
                 holder.mediaFile?.let { it1 -> mSelectedItems.add(it1) }
             }
             val selected = mSelectedItems.contains(media)
             holder.updateSelection(selectionMode, selected)

         }

    }

     fun getSelectedItems(): ArrayList<MediaFile> {
        return mSelectedItems
    }

    fun  setSelectMode(selectionMode: Boolean) {
        this.selectionMode = selectionMode;
    }



}