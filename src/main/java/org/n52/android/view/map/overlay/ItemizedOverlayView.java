/**
 * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.android.view.map.overlay;

import org.n52.android.geoar.R;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Map overlay view as a {@link FrameLayout} to show selected itemized 
 * data as a bubble in a {@link MapView}
 * 
 * @author Arne de Wall
 *
 * @param <Item>
 */
public class ItemizedOverlayView<Item extends OverlayItem> extends FrameLayout {
	private LinearLayout layout;
	private TextView title;
	private TextView snippet;
	
	public ItemizedOverlayView(Context context) {
		super(context);
		
		setPadding(10, 0, 10, 10); // TODO
		
		layout = new LimitLinearLayout(context);
		layout.setVisibility(VISIBLE);
		
		setupView(context, layout);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;
		
		addView(layout, params);
	}
	
	protected void setupView(Context context, final ViewGroup parent){
		
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.map_overlay_bubble, parent);
		title = (TextView) v.findViewById(R.id.bubble_item_title); 
		snippet = (TextView) v.findViewById(R.id.bubble_item_snippet);
	}
	
	/**
	 * Sets the view data from a given Overlay item
	 * @param item
	 */
	protected void setBalloonData(Item item, ViewGroup parent) {

		if (item.getTitle() != null) {
			title.setVisibility(VISIBLE);
			title.setText(item.getTitle());
		} else {
			title.setText("");
			title.setVisibility(GONE);
		}
		if (item.getSnippet() != null) {
			snippet.setVisibility(VISIBLE);
			snippet.setText(item.getSnippet());
		} else {
			snippet.setText("");
			snippet.setVisibility(GONE);
		}
	}
	
	protected void setItemData(Item item, ViewGroup parent){
		if(item.getTitle() != null){
			title.setVisibility(VISIBLE);
			title.setText(item.getTitle());
		} 
	}
	
	/**
	* Sets the view data from a given overlay item.
	*
	* @param item - The overlay item containing the relevant view data.
	*/
	public void setData(Item item) {
		layout.setVisibility(VISIBLE);
		setBalloonData(item, layout);
	}
	
	private class LimitLinearLayout extends LinearLayout {

		private static final int MAX_WIDTH_DP = 280;

		final float SCALE = getContext().getResources().getDisplayMetrics().density;

		public LimitLinearLayout(Context context) {
			super(context);
		}

		public LimitLinearLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int mode = MeasureSpec.getMode(widthMeasureSpec);
			int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
			int adjustedMaxWidth = (int)(MAX_WIDTH_DP * SCALE + 0.5f);
			int adjustedWidth = Math.min(measuredWidth, adjustedMaxWidth);
			int adjustedWidthMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedWidth, mode);
			super.onMeasure(adjustedWidthMeasureSpec, heightMeasureSpec);
		}
	}	
}
