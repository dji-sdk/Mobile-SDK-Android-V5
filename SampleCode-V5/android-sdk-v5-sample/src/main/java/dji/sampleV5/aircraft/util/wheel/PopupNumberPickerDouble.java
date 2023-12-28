package dji.sampleV5.aircraft.util.wheel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import dji.sampleV5.aircraft.R;
import dji.sampleV5.aircraft.util.DensityUtil;

public class PopupNumberPickerDouble extends PopupWindow {
	
	private List<String> item_texts1 = null;

	private List<String> item_texts2 = null;
	Map<Integer, List<Integer>> item_image2;
	
	String[] strItemValue1;
	String[] strItemValue2;
	
	PickerValueChangeListener valueChangeListen;
	WheelView Wheelpicker1;
	WheelView Wheelpicker2;
	
	//int picker1_num = 0;
	//int picker2_num = 1;
	
	int picker_currentPos1 = 0;
	int picker_currentPos2 = 0;
	
	int jpeg_seq_interval = 0;
	
	public PopupNumberPickerDouble(Context context)
	{
		super(context);
	}
	@SuppressLint("InflateParams")
    public PopupNumberPickerDouble(Context context, 
				List<String> item_strings1, //第一个picker 的数据
				List<String> item_strings2, //第二个picker 的数据;
			PickerValueChangeListener itemClickEvent, PopupNumberPickerPosition position) {

		super(context);

		item_texts1 = item_strings1;
		item_texts2 = item_strings2;

		valueChangeListen = itemClickEvent;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.numpicker_continue_shot, null);
		this.setContentView(view);
		this.setWidth(DensityUtil.dip2px(context, position.width));
		this.setHeight(DensityUtil.dip2px(context, position.height));
		this.setFocusable(true);//

		ColorDrawable dw = new ColorDrawable(-00000);
		this.setBackgroundDrawable(dw);
		strItemValue1 = new String[item_texts1.size()];
		for (int i = 0; i < item_texts1.size(); i++) 
		{
			strItemValue1[i] = item_texts1.get(i);

		}  
		
		strItemValue2 = new String[item_texts2.size()];
		for (int i = 0; i < item_texts2.size(); i++) 
		{
			strItemValue2[i] = item_texts2.get(i);

		}  
		  
		Wheelpicker1 = (WheelView) view.findViewById(R.id.id_numberPicker1);
		Wheelpicker1.addScrollingListener(onWheelScrollListener1);
		Wheelpicker1.addClickingListener(onWheelClickedListener1);
		Wheelpicker1.setViewAdapter(new TypeTextAdapter(context,strItemValue1));
		
		Wheelpicker1.setCurrentItem(position.pos);

		Wheelpicker2 = (WheelView) view.findViewById(R.id.id_numberPicker2);
		Wheelpicker2.addScrollingListener(onWheelScrollListener2);
		Wheelpicker2.addClickingListener(onWheelClickedListener2);
		Wheelpicker2.setViewAdapter(new TypeTextAdapter(context,strItemValue2));
		
		Wheelpicker2.setCurrentItem(position.pos);
		
		ImageButton select_button = (ImageButton) view.findViewById(R.id.id_select_imageButton1);
		select_button.setOnClickListener(v -> {
			// TODO Auto-generated method stub
			valueChangeListen.onValueChange(picker_currentPos1, picker_currentPos2);

		});

	}
  

	class TypeTextAdapter extends AbstractWheelTextAdapter {

		String[] strItemValue;
		protected TypeTextAdapter(Context context,String[] strItemValue2) {
			// super(context, R.layout.type_layout, R.id.type_name, isSelected);
			super(context, R.layout.numpicker_type_layout, R.id.type_name);
			strItemValue = strItemValue2;
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

			View view = super.getItem(index, convertView, parent);
			
			return view;
		}
	}
	
	class TypeImageAdapter extends AbstractWheelTextAdapter {

		List<Integer> imagelist;
		protected TypeImageAdapter(Context context, List<Integer> imagelis) {
			// super(context, R.layout.type_layout, R.id.type_name, isSelected);
			super(context, R.layout.numpicker_type_layout, R.id.type_name);
			imagelist = imagelis;
		}

		@Override
		public int getItemsCount() {
			return imagelist.size();
		}

		@Override
		protected CharSequence getItemText(int index) {
			// TODO Auto-generated method stub
			return "";
		}

		@Override
		public View getItem(int index, View convertView, ViewGroup parent) {

			View view = super.getItem(index, convertView, parent);
			TextView tv = (TextView) view.findViewById(R.id.type_name);
			tv.setVisibility(View.GONE);
			tv.setText("fff 0");
			ImageView image = (ImageView) view.findViewById(R.id.flag);
			image.setVisibility(View.VISIBLE);
			image.setImageResource(imagelist.get(index));
			tv.setText("fff 1");
			return view;
		}
	}
	
	
	class ItemAdapter extends AbstractWheelTextAdapter {

		List<Integer> imagelist;
		protected ItemAdapter(Context context,List<Integer> imagelis) {
			// super(context, R.layout.type_layout, R.id.type_name, isSelected);
			super(context, R.layout.numpicker_type_layout, R.id.type_name);
			imagelist = imagelis;
		}

		@Override
		public int getItemsCount() {
			return imagelist.size();
		}

		@Override
		protected CharSequence getItemText(int index) {
			// TODO Auto-generated method stub
			return "";
		}

		@Override
		public View getItem(int index, View convertView, ViewGroup parent) {

			View view = super.getItem(index, convertView, parent);
			TextView tv = (TextView) view.findViewById(R.id.type_name);
			tv.setVisibility(View.GONE);
			tv.setText("fff 0");
			ImageView image = (ImageView) view.findViewById(R.id.flag);
			image.setVisibility(View.VISIBLE);
			image.setImageResource(imagelist.get(index));
			tv.setText("fff 1");
			return view;
		}
	}
	
	OnWheelScrollListener onWheelScrollListener1 = new OnWheelScrollListener() {
		@Override
		public void onScrollingStarted(WheelView wheel) {
			//select_type = type[wheelView.getCurrentItem()];
		}

		@Override
		public void onScrollingFinished(WheelView wheel) {
			int pos = wheel.getCurrentItem();
			picker_currentPos1 = pos;
			//String select = strItemValue[pos];
			
		}
	};

	OnWheelClickedListener onWheelClickedListener1 = (wheel, itemIndex) -> {
		picker_currentPos1 = itemIndex;

		wheel.setCurrentItem(itemIndex);
	};
	
	OnWheelScrollListener onWheelScrollListener2 = new OnWheelScrollListener() {
		@Override
		public void onScrollingStarted(WheelView wheel) {
			//select_type = type[wheelView.getCurrentItem()];
		}

		@Override
		public void onScrollingFinished(WheelView wheel) {
			int pos = wheel.getCurrentItem();
			picker_currentPos2 = pos;
			
			
		}
	};

	OnWheelClickedListener onWheelClickedListener2 = (wheel, itemIndex) -> {
		wheel.setCurrentItem(itemIndex);
		picker_currentPos2 = itemIndex;

	};

}
