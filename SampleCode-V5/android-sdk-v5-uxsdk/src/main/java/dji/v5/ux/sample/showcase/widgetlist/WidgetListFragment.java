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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;

/**
 * Displays a list of widget names.
 */
public class WidgetListFragment extends Fragment {
    public static final String TAG = "WidgetListFragment";
    //region Views
    protected RecyclerView widgetRecyclerView;
    //region Fields
    private OnWidgetItemSelectedListener onWidgetItemSelectedListener;
    //endregion
    private int selectedPosition = -1;
    //endregion

    //region Lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.uxsdk_fragment_widget_list, container, false);
        widgetRecyclerView = rootView.findViewById(R.id.recyclerview_widgets);
        widgetRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager widgetLayoutManager = new LinearLayoutManager(getContext());
        widgetRecyclerView.setLayoutManager(widgetLayoutManager);
        RecyclerView.Adapter<WidgetListItemAdapter.WidgetListItemViewHolder> widgetAdapter = new WidgetListItemAdapter(((WidgetsActivity) getActivity()).widgetListItems,
                onWidgetItemSelectedListener);
        widgetRecyclerView.setAdapter(widgetAdapter);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            onWidgetItemSelectedListener = (OnWidgetItemSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnWidgetItemSelectedListener");
        }
    }
    //endregion

    /**
     * Select the view at the given position and deselect the previously selected view.
     *
     * @param position The position of the currently selected view.
     */
    public void updateSelectedView(int position) {
        if (position != selectedPosition) {
            ((WidgetListItemAdapter.WidgetListItemViewHolder) widgetRecyclerView
                    .findViewHolderForAdapterPosition(position))
                    .setSelected(true, getResources());
            if (selectedPosition >= 0) {
                ((WidgetListItemAdapter.WidgetListItemViewHolder) widgetRecyclerView
                        .findViewHolderForAdapterPosition(selectedPosition))
                        .setSelected(false, getResources());
            }
            selectedPosition = position;
        }
    }

    /**
     * A callback for widget item selection
     */
    public interface OnWidgetItemSelectedListener {
        /**
         * Called when a widget item is selected.
         *
         * @param position The position of the widget item that was selected.
         */
        void onWidgetItemSelected(int position);
    }

}
