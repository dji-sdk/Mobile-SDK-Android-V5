package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.recyclerview.widget.RecyclerView

import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.util.AnimationUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.utils.common.ContextUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * @author feel.feng
 * @time 2022/04/20 11:16 上午
 * @description:
 */
class MediaListAdapter(
    private val onClick: (MediaFile, View) -> Unit
) :
    RecyclerView.Adapter<MediaListAdapter.ViewHolder>() {

    /**
     * 内部数据副本，避免直接持有 SDK 内部被后台线程并发修改的 List，
     * 从而防止 RecyclerView "Inconsistency detected" 崩溃。
     */
    private val data: ArrayList<MediaFile> = ArrayList()

    val mSelectedItems: ArrayList<MediaFile> = ArrayList<MediaFile>()
    var selectionMode: Boolean = false

    /**
     * 在主线程提交数据快照并刷新。调用方需保证传入的是一份拷贝。
     */
    fun submitList(newList: List<MediaFile>) {
        data.clear()
        data.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_mediafile_list, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

        val mediaFile = data[position]
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
            val weakHolder = WeakReference(holder)
            mediaFile.pullThumbnailFromCamera(object :
                CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
                override fun onSuccess(t: Bitmap?) {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        // 用 adapterPosition 校验，避免 ViewHolder 复用导致缩略图错绑
                        if (holder.adapterPosition == position) {
                            holder.imageView.setImageBitmap(t)
                        }
                    }

                }

                override fun onFailure(error: IDJIError) {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        val h = weakHolder.get() ?: return@scheduleDirect
                        h.imageView.setImageDrawable(
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
        var imageView: ImageView = view.findViewById(R.id.iv_thumbnail)
        var textView: TextView = view.findViewById(R.id.tv_media_info)
        var mediaFile: MediaFile? = null
        var rightTopIcon: ImageView = view.findViewById(R.id.right_top_icon)

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

    fun setSelectMode(selectionMode: Boolean) {
        this.selectionMode = selectionMode;
    }


}