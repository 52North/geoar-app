package org.n52.android.view.map.overlay;

import java.util.List;

import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.bitmap;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

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
	private Item selectedItem;
	private int selectedIndex;

	public ItemizedDataOverlay(List<Item> pList, Drawable pDefaultMarker,
			ItemizedDataOverlay.OnItemGestureListener<Item> pOnItemGestureListener,
            ResourceProxy pResourceProxy) {
	    super(pDefaultMarker, pResourceProxy);
	
	    this.mOverlayItems = pList;
	    this.mOnItemGestureListener = pOnItemGestureListener;
	    populate();
	}
	

    public ItemizedDataOverlay(List<Item> pList, ItemizedDataOverlay.OnItemGestureListener<Item> pOnItemGestureListener,
                    ResourceProxy pResourceProxy) {
            this(pList, pResourceProxy.getDrawable(bitmap.marker_default), pOnItemGestureListener,
                            pResourceProxy);
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
//		return Math.min(mItemList.size(), mDrawnItemsLimit);
		return mOverlayItems.size();
	}
	
	public boolean addItem(Item item){
		boolean res = mOverlayItems.add(item);
		populate();
		return res;
	}
	
	public void setOverlayItems(List<Item> item){
		mOverlayItems.clear();
		mOverlayItems = null;
		mOverlayItems = item;
		populate();
	}


	@Override
	public boolean onLongPress(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
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
    
    protected boolean onLongPressHelper(final int index, final Item item) {
        return this.mOnItemGestureListener.onItemLongPress(index, item);
    }
    
    protected boolean onSingleTapUpHelper(final int index, final Item item, final MapView mapView) {
        return this.mOnItemGestureListener.onItemSingleTapUp(index, item);
    }





    private boolean activateSelectedItems(final MotionEvent event, final MapView mapView,
            final ActiveItem task) {
    final Projection pj = mapView.getProjection();
    final int eventX = (int) event.getX();
    final int eventY = (int) event.getY();

    /* These objects are created to avoid construct new ones every cycle. */
    pj.fromMapPixels(eventX, eventY, mTouchScreenPoint);

    for (int i = 0; i < this.mOverlayItems.size(); ++i) {
            final Item item = getItem(i);
            final Drawable marker = (item.getMarker(0) == null) ? this.mDefaultMarker : item
                            .getMarker(0);

            pj.toPixels(item.getPoint(), mItemPoint);

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


