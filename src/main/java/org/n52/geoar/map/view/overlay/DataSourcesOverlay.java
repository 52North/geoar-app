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
package org.n52.geoar.map.view.overlay;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Point;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public class DataSourcesOverlay extends Overlay {

    private static final Map<Class<? extends OverlayType<?>>, Class<? extends DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>>> overlayClasses = new HashMap<Class<? extends OverlayType<?>>, Class<? extends DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>>>() {
        private static final long serialVersionUID = 1L;
        {
            put(PointOverlayType.class, DataSourcePointOverlay.class);
            put(PolylineOverlayType.class, DataSourcePolylineOverlay.class);
            put(PolygonOverlayType.class, DataSourcePolygonOverlay.class);
        }
    };

    public interface OnOverlayItemTapListener {
        boolean onOverlayItemTap(OverlayType<? extends Geometry> item);
    }

    private Map<Object, Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>>> overlayMap = Collections
            .synchronizedMap(new HashMap<Object, Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>>>());

    private OnOverlayItemTapListener overlayItemTapListener;

    @Override
    protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            Projection projection, byte zoomLevel) {

        for (Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> classMaps : overlayMap
                .values()) {
            for (DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> dataSourceOverlay : classMaps
                    .values()) {
                dataSourceOverlay.drawOverlayBitmap(canvas, drawPosition,
                        projection, zoomLevel);
            }
        }
    }

    public void setOverlayItemTapListener(OnOverlayItemTapListener listener) {
        for (Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> classMaps : overlayMap
                .values()) {
            for (DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> dataSourceOverlay : classMaps
                    .values()) {
                dataSourceOverlay.overlayItemTapListener = listener;
            }
        }
        overlayItemTapListener = listener;
    }

    /**
     * Removes all items from the overlay.
     */
    public void clear() {

        for (Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> classMaps : overlayMap
                .values()) {
            for (DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> dataSourceOverlay : classMaps
                    .values()) {
                dataSourceOverlay.clear();
            }
            classMaps.clear();
        }

        overlayMap.clear();
    }

    public void clear(Object key) {
        overlayMap.remove(key);
        populate();
    }

    /**
     * Removes the given item from the overlay.
     * 
     * @param overlayItem
     *            the item that should be removed from the overlay.
     */
    public void removeItem(OverlayType<? extends Geometry> overlayItem) {

        for (Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> classMaps : overlayMap
                .values()) {
            for (DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> dataSourceOverlay : classMaps
                    .values()) {
                dataSourceOverlay.removeOverlayType(overlayItem);
            }
        }

        populate();
    }

    @SuppressWarnings("unchecked")
    public void setOverlayItems(
            List<OverlayType<? extends Geometry>> overlayItems, Object key) {
        if (overlayItems == null || overlayItems.size() == 0)
            return;


        // Map
        Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> classMap = getClassMap(key);
        if (classMap == null) {
            classMap = createMap(
                    key,
                    (Class<? extends OverlayType<? extends Geometry>>) overlayItems //
                            .get(0).getClass()); //
        }

        DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> dsOverlay = classMap
                .get(overlayItems.get(0).getClass());
        if (dsOverlay == null) {
            try {
                dsOverlay = overlayClasses.get(overlayItems.get(0).getClass())
                        .newInstance();
                dsOverlay.overlayItemTapListener = overlayItemTapListener;
                classMap.put((Class<? extends OverlayType<?>>) overlayItems
                        .get(0).getClass(), dsOverlay);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        dsOverlay.addOverlayTypes(overlayItems);
        populate();
    }

    protected Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> getClassMap(
            Object key) {
        return overlayMap.get(key);
    }

    @SuppressWarnings("unchecked")
    protected Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> createMap(
            Object key, Class<? extends OverlayType<? extends Geometry>> class1) {
        if (overlayMap.get(key) == null) {
            Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> overlayTypeMap = new HashMap<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>>();
            overlayMap.put(key, overlayTypeMap);
            return overlayTypeMap;
        }
        return overlayMap.get(key);
    }

    public void addOverlayItems(
            List<OverlayType<? extends Geometry>> overlayItems, Object key) {

        synchronized (this.overlayMap) {
            Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> classMap = overlayMap
                    .get(key);
            if (classMap == null) {
                classMap = new HashMap<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>>();
                this.overlayMap.put(key, classMap);
            }

            @SuppressWarnings("unchecked")
            Class<? extends OverlayType<?>> clazz = (Class<? extends OverlayType<?>>) overlayItems
                    .get(0).getClass();
            DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> dataSourceOverlay = classMap
                    .get(clazz);
            if (dataSourceOverlay == null) {
                try {
                    dataSourceOverlay = overlayClasses.get(clazz).newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            dataSourceOverlay.addOverlayTypes(overlayItems);
        }

        populate();
    }

    @Override
    public boolean onTap(GeoPoint geoPoint, MapView mapView) {
        for (Map<Class<? extends OverlayType<?>>, DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>>> map : overlayMap
                .values()) {
            for (DataSourceOverlay<? extends Geometry, ? extends OverlayType<?>> overlay : map
                    .values()) {
                if (overlayItemTapListener != null
                        && overlay.onTap(geoPoint, mapView)) {
                    return true;
                }
            }
        }

        return super.onTap(geoPoint, mapView);
    }

    protected void populate() {
        super.requestRedraw();
    }

}
