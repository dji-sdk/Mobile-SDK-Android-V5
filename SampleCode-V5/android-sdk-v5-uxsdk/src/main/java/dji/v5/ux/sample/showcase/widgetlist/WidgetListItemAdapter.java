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

package dji.v5.ux.sample.showcase.widgetlist;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import dji.v5.ux.R;

/**
 * An adapter for displaying {@link WidgetListItem} objects in a RecyclerView.
 */
public class WidgetListItemAdapter extends RecyclerView.Adapter<WidgetListItemAdapter.WidgetListItemViewHolder> {

    //region Fields
    private ArrayList<WidgetListItem> widgetListItems;
    private WidgetListFragment.OnWidgetItemSelectedListener onWidgetItemSelectedListener;
    //endregion

    //region Lifecycle
    public WidgetListItemAdapter(ArrayList<WidgetListItem> widgetListItems, WidgetListFragment.OnWidgetItemSelectedListener onWidgetItemSelectedListener) {
        this.widgetListItems = widgetListItems;
        this.onWidgetItemSelectedListener = onWidgetItemSelectedListener;
    }

    @Override
    public WidgetListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.uxsdk_widget_list_item, parent, false);

        return new WidgetListItemViewHolder(v, onWidgetItemSelectedListener);
    }

    @Override
    public void onBindViewHolder(WidgetListItemViewHolder holder, int position) {
        holder.titleTextView.setText(widgetListItems.get(position).getTitleId());
    }

    @Override
    public int getItemCount() {
        return widgetListItems.size();
    }

    @Override
    public void onBindViewHolder(WidgetListItemViewHolder holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }



    //endregion

    /**
     * A view holder for widget list items.
     */
    public class WidgetListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        //region Fields
        private TextView titleTextView;
        private WidgetListFragment.OnWidgetItemSelectedListener onWidgetItemSelectedListener;
        //endregion

        //region Lifecycle
        private WidgetListItemViewHolder(TextView itemView, WidgetListFragment.OnWidgetItemSelectedListener onWidgetItemSelectedListener) {
            super(itemView);
            titleTextView = itemView;
            this.onWidgetItemSelectedListener = onWidgetItemSelectedListener;
            titleTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onWidgetItemSelectedListener.onWidgetItemSelected(getAdapterPosition());
        }
        //endregion

        /**
         * Sets the selected state of this widget list item.
         *
         * @param selected  Whether this widget list item is selected.
         * @param resources An instance of {@link Resources}.
         */
        public void setSelected(boolean selected, Resources resources) {
            if (selected) {
                titleTextView.setBackgroundColor(resources.getColor(R.color.uxsdk_gray_4));
            } else {
                titleTextView.setBackgroundColor(resources.getColor(R.color.uxsdk_dark_theme_background));
            }
        }
    }
}
