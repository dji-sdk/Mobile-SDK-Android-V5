package com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.dji.open.pilot.plugin.R;


public class KeyItemAdapter extends RecyclerView.Adapter<KeyItemAdapter.ComViewHolder> {

    private final KeyItemActionListener<KeyItem<?, ?>> callback;

    protected Context context;
    private final KeyFilterModel<KeyItem<?, ?>> filterModel;
    private final Filter keyFilter;


    @MainThread
    public KeyItemAdapter(Context context, List<KeyItem<?, ?>> dataList, KeyItemActionListener<KeyItem<?, ?>> callback) {
        requireMainThread();
        this.context = context;
        this.callback = callback;
        this.filterModel = new KeyFilterModel<>(dataList, item -> item.keyInfo.getIdentifier());
        // 固定主线程 Looper，复用同一 Filter 的后台队列去重。
        this.keyFilter = createKeyFilter();
    }

    @Override
    public ComViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ComViewHolder(LayoutInflater.from(context).inflate(R.layout.item_camera_key_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ComViewHolder holder, int position) {
        List<KeyItem<?, ?>> visibleItems = filterModel.getVisibleItems();
        if (visibleItems.size() <= position) {
            return;
        }
        convert(holder, visibleItems.get(position));
    }

    @Override
    public int getItemCount() {
        return filterModel.getVisibleItems().size();
    }


    public void convert(ComViewHolder viewHolder, final KeyItem<?, ?> keyItem) {
        if (viewHolder == null || keyItem == null) {
            return;
        }
        TextView textView = viewHolder.getView(R.id.tv_item_name);
        textView.setText(keyItem.getName());
        View container = viewHolder.getView(R.id.params_item);
        container.setBackgroundResource(keyItem.isItemSelected() ? R.drawable.bg_menu_item_selected : 0);
        textView.setOnClickListener(v -> {
            if (callback != null) {
                callback.actionChange(keyItem);
            }
        });
    }


    /** 主线程原子提交快照和查询，列表发布与数量回调共用请求版本。 */
    @MainThread
    public void filterKeys(@NonNull List<KeyItem<?, ?>> source, @NonNull String query,
                           @NonNull Filter.FilterListener listener) {
        requireMainThread();
        String token = filterModel.submit(source, query);
        // Filter 会把 constraint 转成 String，传版本而非查询，避免丢失请求身份。
        keyFilter.filter(token, count -> {
            if (filterModel.isPublished(token)) {
                listener.onFilterComplete(count);
            }
        });
    }

    private Filter createKeyFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                KeyFilterModel.Result<KeyItem<?, ?>> filtered = filterModel.filter(constraint.toString());
                FilterResults filterResults = new FilterResults();
                filterResults.values = filtered;
                filterResults.count = filtered.getCount();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults filterResults) {
                requireMainThread();
                // 框架异常兜底会传空 values，不发布也不回调数量。
                if (filterResults.values == null) {
                    return;
                }
                @SuppressWarnings("unchecked")
                KeyFilterModel.Result<KeyItem<?, ?>> filtered =
                        (KeyFilterModel.Result<KeyItem<?, ?>>) filterResults.values;
                if (filterModel.publish(filtered)) {
                    notifyDataSetChanged();
                }
            }
        };
    }

    private static void requireMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Key filtering must be submitted and published on the main thread");
        }
    }


    /**
     * 缓存容器
     */
    public class ComViewHolder extends RecyclerView.ViewHolder {

        private View convertView;
        private SparseArrayCompat<View> views;

        public ComViewHolder(View itemView) {
            super(itemView);
            this.convertView = itemView;
            this.views = new SparseArrayCompat<>();
        }


        public <T extends View> T getView(int layoutId) {
            View view = views.get(layoutId);
            if (view == null) {
                view = convertView.findViewById(layoutId);
                views.put(layoutId, view);
            }
            return (T) view;
        }
    }

}
