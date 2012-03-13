package org.n52.android.view.map.overlay;

import java.util.List;

import org.n52.android.geoar.R;
import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.bitmap;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

/**
 * Map {@link Overlay} to show itemized data in a {@link MapView}
 * 
 * @author Arne de Wall
 *
 * @param <Item>
 */
public class ItemizedDataOverlay<Item extends OverlayItem> 	extends ItemizedOverlay<Item> {
	
	public static interface OnItemGestureListener<T> {
		public boolean onItemSingleTapUp(final int index, final T item);
		public boolean onItemLongPress(final int index, final T item);
	}
	
	public static interface ActiveItem {
		public boolean run(final int aIndex);
	}

	protected List<Item> mOverlayItems;
	protected OnItemGestureListener<Item> mOnItemGestureListener;
	
	private final Point mTouchScreenPoint = new Point();
	private final Point mItemPoint = new Point();
	private ItemizedOverlayView mOverlayView;
	private View closeView;
	
	private OverlayItem selectedItem;
	private int selectedIndex;

	/**
	 * Constructor #1
	 * 
	 * @param list
	 * 					List of overlay items.
	 * @param defaultMarker
	 * 					the default Marker for OverlayItems
	 * @param onItemGestureListener
	 * 					itemGestureListener
	 * @param resourceProxy
	 */
	public ItemizedDataOverlay(List<Item> list, Drawable defaultMarker,
			ItemizedDataOverlay.OnItemGestureListener<Item> onItemGestureListener,
            ResourceProxy resourceProxy) {
	    super(defaultMarker, resourceProxy);
	
	    this.mOverlayItems = list;
	    this.mOnItemGestureListener = onItemGestureListener;
	    populate();
	}
	
	/**
	 * Constructor #2
	 * 
	 * @param list
	 * 				List of overlay Items.
	 * @param onItemGestureListener
	 * @param resourceProxy
	 */
    public ItemizedDataOverlay(List<Item> list, ItemizedDataOverlay.OnItemGestureListener<Item> onItemGestureListener,
                    ResourceProxy resourceProxy) {
            this(list, resourceProxy.getDrawable(bitmap.marker_default), onItemGestureListener,
                            resourceProxy);
    }

    /**
     * Creates and displays the bubble view inside the current MapView.
     * 
     * @param mapView
     * 				current MapView where the bubble will be displayed
     * @param item
     * 				selected item to display within bubble
     * @param index
     * 				index of selected item
     */
    void setBubbleItem(MapView mapView, OverlayItem item, int index){
        if(item == null)
        	return;
       
        selectedIndex = index;
        selectedItem = item;

        if(mOverlayView == null){
            mOverlayView = new ItemizedOverlayView<OverlayItem>(mapView.getContext());
            // init close button of info bubble 
            closeView = (View) mOverlayView.findViewById(R.id.bubble_close);
            closeView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hideBubble();
				}
			});
            // add overlay to the current MapView
            mapView.addView(mOverlayView);
        } 
        // set the data to display
        mOverlayView.setVisibility(View.VISIBLE);
        mOverlayView.setData(selectedItem);
        // set the new layout parameters
        GeoPoint point = selectedItem.getPoint();
        MapView.LayoutParams params = new MapView.LayoutParams(
        		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
        		MapView.LayoutParams.BOTTOM_CENTER, 0, 0);
        // set the new layout parameters
        mOverlayView.setLayoutParams(params);
    }
    
    /**
     * Unselects the current bubble viewed item.
     */
    private void hideBubble(){
    	if(mOverlayView != null)
    		mOverlayView.setVisibility(View.GONE);
    	selectedItem = null;
    }
    
	@Override
	public boolean onSnapToItem(int x, int y, Point snapPoint, IMapView mapView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Item createItem(int i) {
		return mOverlayItems.get(i);
	}

	@Override
	public int size() {
		return mOverlayItems.size();
	}
	
	/**
	 * Adds a OverlayItem to the list of overlays.
	 * 
	 * @param item
	 * 			OverlayItem to add.
	 * @return 
	 * 			true, if adding was successful.
	 */
	public boolean addItem(Item item){
		boolean res = mOverlayItems.add(item);
		populate();
		return res;
	}
	
	/**
	 * Clears the list of OverlayItems and generates a new List.
	 * @param item
	 * 			List of Overlays.
	 */
	public void setOverlayItems(List<Item> item){
		mOverlayItems.clear();
		mOverlayItems = null;
		mOverlayItems = item;
		populate();
	}


	@Override
	public boolean onLongPress(MotionEvent e, MapView mapView) {
		return super.onLongPress(e, mapView);
	}


    @Override
    public boolean onSingleTapUp(final MotionEvent event, final MapView mapView) {
            return (activateSelectedItems(event, mapView, new ActiveItem() {
                    @Override
                    public boolean run(final int index) {
                            final ItemizedDataOverlay<Item> that = ItemizedDataOverlay.this;
                            if (that.mOnItemGestureListener == null) {
                                    return false;
                            }
                            return onSingleTapUpHelper(index, that.mOverlayItems.get(index), mapView);
                    }
            })) ? true : super.onSingleTapUp(event, mapView);
    }
    
    
    private boolean onSingleTapUpHelper(final int index, final Item item, final MapView mapView) {
        return this.mOnItemGestureListener.onItemSingleTapUp(index, item);
    }

    private boolean activateSelectedItems(final MotionEvent event, final MapView mapView,
            final ActiveItem task) {
    	// get the current display coordinates
	    final int eventX = (int) event.getX();
	    final int eventY = (int) event.getY();
    	// get the current map projection
	    final Projection mapProjection = mapView.getProjection();
	    // project map pixels to screen point
	    mapProjection.fromMapPixels(eventX, eventY, mTouchScreenPoint);
	
	    for (int i = 0; i < this.mOverlayItems.size(); ++i) {
            final Item item = getItem(i);
            final Drawable marker = (item.getMarker(0) == null) 
            		? this.mDefaultMarker : item.getMarker(0);
            // project geopoint to pixel coordinates
            mapProjection.toPixels(item.getPoint(), mItemPoint);
            // Test whether a overlay item has been selected
            if (hitTest(item, marker, mTouchScreenPoint.x - mItemPoint.x, mTouchScreenPoint.y
                            - mItemPoint.y)) {
                    if (task.run(i)) {
                            return true;
                    }
            	}
    		}
	    return false;
    }	
}


