package org.n52.android.view.map.overlay;

import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ItemizedOverlayView<Item extends OverlayItem> extends FrameLayout {

	private LinearLayout layout;
	private TextView title;
	private TextView value;
	
	
	public ItemizedOverlayView(Context context) {
		super(context);
		
		setPadding(10, 0, 10, 10); // TODO
		
		layout = new LinearLayout(context);
		layout.setVisibility(VISIBLE);
		
		setupView(context, layout);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;
		
		addView(layout, params);
	}
	
	protected void setupView(Context context, final ViewGroup parent){
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		View v = inflater.inflate(, root)
	}
	
	/**
	 * Sets the view data from a given Overlay item
	 * @param item
	 */
	public void setData(Item item){
		layout.setVisibility(VISIBLE);
		setItemData(item, layout);
	}
	
	
	protected void setItemData(Item item, ViewGroup parent){
		if(item.getTitle() != null){
			title.setVisibility(VISIBLE);
			title.setText(item.getTitle());
		} 
	}
}
