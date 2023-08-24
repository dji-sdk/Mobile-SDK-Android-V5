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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dji.v5.ux.R;

/**
 * A fragment that shows a single widget or a set of coupled widgets.
 */
public class WidgetFragment extends Fragment {

    //region Constants
    public static final String ARG_POSITION = "Position";
    //endregion

    //region Fields
    private LinearLayout rootView;
    //endregion

    //region Lifecycle
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (LinearLayout) inflater.inflate(R.layout.uxsdk_fragment_widget, container, false);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null) {
            updateWidgetView(args.getInt(ARG_POSITION));
        } else {
            updateWidgetView(0);
        }
    }
    //endregion

    /**
     * Adds {@link WidgetView} instances to this fragment based on the {@link WidgetListItem} at
     * the given position.
     *
     * @param position the item's position within {@link WidgetsActivity#widgetListItems}
     */
    public void updateWidgetView(int position) {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        rootView.removeAllViews();

        WidgetListItem widgetListItem = ((WidgetsActivity) getActivity()).widgetListItems.get(position);
        if (widgetListItem.getWidgetViewHolders().length == 2) {
            // coupled widgets: set state callback of first widget to second widget
            WidgetView widgetView0 = new WidgetView(getContext());
            WidgetView widgetView1 = new WidgetView(getContext());
            widgetView0.init(widgetListItem.getWidgetViewHolders()[0]);
            widgetView1.init(widgetListItem.getWidgetViewHolders()[1]);
            rootView.addView(widgetView0);
            rootView.addView(widgetView1);
        } else {
            for (WidgetViewHolder<?> widgetViewHolder : widgetListItem.getWidgetViewHolders()) {
                WidgetView widgetView = new WidgetView(getContext());
                widgetView.init(widgetViewHolder);
                rootView.addView(widgetView);
            }
        }
    }

    @Override
    public void onDestroyView() {
        rootView.removeAllViews();
        super.onDestroyView();
    }
}
