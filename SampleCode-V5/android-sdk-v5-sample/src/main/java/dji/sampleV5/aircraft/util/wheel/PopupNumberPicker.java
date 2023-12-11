package dji.sampleV5.aircraft.util.wheel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import java.util.List;

import dji.sampleV5.aircraft.R;
import dji.sampleV5.aircraft.util.DensityUtil;

public class PopupNumberPicker extends PopupWindow {

	private List<String> item_texts = null;
	private String[] strItemValue;
	private TypeTextAdapter typeTextAdapter;
	private int pickerToSelectPos;
	private WheelView wheelPicker;

	private int pickerSelectedPos = -1;
	private View view;

	private boolean isSingleSelected;

	public interface PickerOtherActionListener {
		void onOtherAction();
	}

	PickerValueChangeListener onCallBack;
	PickerOtherActionListener otherAction;

	public PopupNumberPicker(Context context) 
	{
		super(context);
	}
	@SuppressLint("InflateParams")
	public PopupNumberPicker(Context context, List<String> item_strings,
							 PickerValueChangeListener itemClickEvent, PopupNumberPickerPosition position) {
		this(context, item_strings, itemClickEvent, position, false, false);
	}

    public PopupNumberPicker(Context context, List<String> item_strings,
			PickerValueChangeListener itemClickEvent, PopupNumberPickerPosition position , boolean selectable, boolean isSingle) {
		super(context);
		isSingleSelected = isSingle;
		item_texts = item_strings;
		onCallBack = itemClickEvent;


		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.numpicker, null);
		this.setContentView(view);
		this.setWidth(DensityUtil.dip2px(context, position.width));
		this.setHeight(DensityUtil.dip2px(context, position.height));
		this.setFocusable(true);//

		ColorDrawable dw = new ColorDrawable(Color.TRANSPARENT);
		this.setBackgroundDrawable(dw);
		strItemValue = new String[item_texts.size()];
		for (int i = 0; i < item_texts.size(); i++) 
		{
			strItemValue[i] = item_texts.get(i);
		}

		wheelPicker = (WheelView) view.findViewById(R.id.id_numberPicker1);
		wheelPicker.addScrollingListener(onWheelScrollListener);
		wheelPicker.addClickingListener(onWheelClickedListener);

		if (selectable) {
			boolean[] isSelected = new boolean[item_strings.size()];
			typeTextAdapter = new TypeTextAdapter(context, isSelected);
		} else {
			typeTextAdapter = new TypeTextAdapter(context);
		}
		
		//typeTextAdapter.init(0);
		wheelPicker.setViewAdapter(typeTextAdapter);
		//放到setViewAdapter 之后
		wheelPicker.setCurrentItem(position.pos);
		

		ImageButton select_button = (ImageButton) view.findViewById(R.id.id_select_imageButton1);
		
		pickerToSelectPos = position.pos;
		
		select_button.setOnClickListener(v -> onCallBack.onValueChange(pickerToSelectPos, -1));
	}

	public int getPickerSelectedPos() {
		return this.pickerSelectedPos;
	}

	public void setPickerSelectedPos(int pickerSelectedPos) {
		this.pickerSelectedPos = pickerSelectedPos;
		if (pickerSelectedPos == pickerToSelectPos) {
			typeTextAdapter.setChange(pickerSelectedPos);
		}
	}

	class TypeTextAdapter extends AbstractWheelTextAdapter {

		protected TypeTextAdapter(Context context) {
			// super(context, R.layout.type_layout, R.id.type_name, isSelected);
			super(context, R.layout.numpicker_type_layout, R.id.type_name);
		}

		protected TypeTextAdapter(Context context, boolean[] isSelected) {
			super(context, R.layout.numpicker_type_layout, R.id.type_name, isSelected);
		}
		@Override
		public int getItemsCount() {
			return strItemValue.length;
		}

		@Override
		protected CharSequence getItemText(int index) {
			// TODO Auto-generated method stub
			return strItemValue[index].toString();
		}

		@Override
		public View getItem(int index, View convertView, ViewGroup parent) {
			return super.getItem(index, convertView, parent);
		}

		@Override
		public void setSelected(int index){
			if (isSelected != null) {
				for (int i = 0; i < isSelected.length; i++) {
					if (i == index) {
						isSelected[i] = true;
					} else {
						if (isSingleSelected) {
							isSelected[i] = false;
						}
					}
				}
			}
		}
	}
	
	OnWheelScrollListener onWheelScrollListener = new OnWheelScrollListener() {
		@Override
		public void onScrollingStarted(WheelView wheel) {
			//select_type = type[wheelView.getCurrentItem()];
		}

		@Override
		public void onScrollingFinished(WheelView wheel) {
			pickerToSelectPos = wheel.getCurrentItem();
		}
	};

	OnWheelClickedListener onWheelClickedListener = (wheel, itemIndex) -> {
		wheel.setCurrentItem(itemIndex);
		pickerToSelectPos = itemIndex;
	};

}
