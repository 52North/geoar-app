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
///**
// * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
///**
// * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.n52.geoar.map.view;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.mapsforge.android.maps.overlay.ItemizedOverlay;
//import org.mapsforge.android.maps.overlay.OverlayItem;
//import org.n52.geoar.map.view.overlay.OverlayType;
//
//import android.graphics.drawable.Drawable;
//
///**
// * Special {@link ItemizedOverlay} based on a {@link Map}. This allows to
// * efficiently add and remove {@link OverlayItem}s from different sources to and
// * from the same overlay.
// */
//public class DataSourcesOverlay extends ItemizedOverlay<OverlayType<? extends Geometry>> {
//
//	interface OnOverlayItemTapListener {
//		boolean onOverlayItemTap(OverlayType item);
//	}
//
//	
//	private Map<Object, List<VisualizationOverlayItem>> overlayItemMap = new HashMap<Object, List<VisualizationOverlayItem>>();
//	private OnOverlayItemTapListener overlayItemTapListener;
//
//	public DataSourcesOverlay() {
//		this(null);
//	}
//
//	public DataSourcesOverlay(Drawable defaultMarker) {
//		super(defaultMarker);
//	}
//
//	public void setOverlayItemTapListener(OnOverlayItemTapListener listener) {
//		overlayItemTapListener = listener;
//	}
//
//	/**
//	 * Removes all items from the overlay.
//	 */
//	public void clear() {
//		synchronized (this.overlayItemMap) {
//			for (List<VisualizationOverlayItem> itemList : overlayItemMap.values()) {
//				itemList.clear();
//			}
//			overlayItemMap.clear();
//		}
//		populate();
//	}
//
//	public void clear(Object key) {
//		synchronized (this.overlayItemMap) {
//			overlayItemMap.remove(key);
//		}
//		populate();
//	}
//
//	/**
//	 * Removes the given item from the overlay.
//	 * 
//	 * @param overlayItem
//	 *            the item that should be removed from the overlay.
//	 */
//	public void removeItem(OverlayItem overlayItem) {
//		synchronized (this.overlayItemMap) {
//			for (List<VisualizationOverlayItem> itemList : overlayItemMap.values()) {
//				itemList.remove(overlayItem);
//			}
//		}
//		populate();
//	}
//
//	@Override
//	public int size() {
//		synchronized (this.overlayItemMap) {
//			int size = 0;
//			for (List<VisualizationOverlayItem> itemList : overlayItemMap.values()) {
//				size += itemList.size();
//			}
//			return size;
//		}
//	}
//
//	@Override
//	protected VisualizationOverlayItem createItem(int index) {
//		synchronized (this.overlayItemMap) {
//			int minIndex = 0;
//			for (List<VisualizationOverlayItem> itemList : overlayItemMap.values()) {
//				if (index >= minIndex && index < minIndex + itemList.size()) {
//					return itemList.get(index - minIndex);
//				}
//
//				minIndex += itemList.size();
//			}
//
//			return null;
//		}
//	}
//
//	public void setOverlayItems(List<VisualizationOverlayItem> overlayItems,
//			Object key) {
//		synchronized (this.overlayItemMap) {
//
//			List<VisualizationOverlayItem> previousMapping = this.overlayItemMap
//					.put(key, overlayItems);
//			if (previousMapping != null) {
//				previousMapping.clear();
//			}
//		}
//		populate();
//	}
//
//	public void addOverlayItems(List<VisualizationOverlayItem> overlayItems,
//			Object key) {
//		synchronized (this.overlayItemMap) {
//			List<VisualizationOverlayItem> mapping = this.overlayItemMap.get(key);
//			if (mapping == null) {
//				this.overlayItemMap.put(key, overlayItems);
//			} else {
//				mapping.addAll(overlayItems);
//			}
//		}
//		populate();
//	}
//
//	@Override
//	protected boolean onTap(int index) {
//		if (overlayItemTapListener != null
//				&& overlayItemTapListener.onOverlayItemTap(createItem(index))) {
//			return true;
//		}
//		return super.onTap(index);
//	}
//
//}
