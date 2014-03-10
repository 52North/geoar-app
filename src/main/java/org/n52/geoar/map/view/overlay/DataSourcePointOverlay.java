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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public class DataSourcePointOverlay extends
        DataSourceOverlay<Point, PointOverlayType> {

    private final List<PointOverlayType> pointOverlays = Collections
            .synchronizedList(new ArrayList<PointOverlayType>());
    private final Set<Integer> visiblePointOverlays = Collections
            .synchronizedSet(new HashSet<Integer>());

    private Drawable defaultMarker;

    private int left, right, bottom, top;
    private android.graphics.Point itemPosition;
    
    public DataSourcePointOverlay(){
    }

    @Override
    public boolean onLongPress(GeoPoint geoPoint, MapView mapView) {
        return super.onLongPress(geoPoint, mapView);
    }

    @Override
    public boolean onTap(GeoPoint geoPoint, MapView mapView) {
        if(overlayItemTapListener == null)
            return false;
        
        Projection projection = mapView.getProjection();
        android.graphics.Point eventPosition = projection.toPixels(geoPoint,
                null);

        if (eventPosition == null)
            return false;

        android.graphics.Point checkItemPoint = new android.graphics.Point();

        for (Integer index : visiblePointOverlays) {
            PointOverlayType pointOverlay = pointOverlays.get(index);
            if (pointOverlay.getPoint() == null)
                continue;

            checkItemPoint = projection.toPixels(pointOverlay.getPoint(),
                    checkItemPoint);

            Rect markerBounds = pointOverlay.marker.getBounds();
            int checkLeft = checkItemPoint.x + markerBounds.left;
            int checkRight = checkItemPoint.x + markerBounds.right;
            int checkTop = checkItemPoint.y + markerBounds.top;
            int checkBottom = checkItemPoint.y + markerBounds.bottom;

            if (checkRight >= eventPosition.x //
                    && checkLeft <= eventPosition.x
                    && checkBottom >= eventPosition.y
                    && checkTop <= eventPosition.y) {
                overlayItemTapListener.onOverlayItemTap(pointOverlays.get(index));
                return true;
            }
        }

        return false;
    }

    @Override
    protected void drawOverlayBitmap(Canvas canvas,
            android.graphics.Point drawPosition, Projection projection,
            byte zoom) {

        android.graphics.Point pointOverlayPosition = new android.graphics.Point();
        int index = 0;

        visiblePointOverlays.clear();
        for (PointOverlayType pot : pointOverlays) {

            if (zoom != pot.cachedZoomLevel) {
                pot.cachedMapPosition = projection.toPoint(pot.getPoint(),
                        pot.cachedMapPosition, zoom);
                pot.cachedZoomLevel = zoom;
            }

            pointOverlayPosition.x = pot.cachedMapPosition.x - drawPosition.x;
            pointOverlayPosition.y = pot.cachedMapPosition.y - drawPosition.y;

            Rect markerBounds = pot.marker.copyBounds();

            this.left = pointOverlayPosition.x + markerBounds.left;
            this.right = pointOverlayPosition.x + markerBounds.right;
            this.top = pointOverlayPosition.y + markerBounds.top;
            this.bottom = pointOverlayPosition.y + markerBounds.bottom;

            // check boundingbox marker interesects with canvas
            if (this.right >= 0 && this.left <= canvas.getWidth()
                    && this.bottom >= 0 && this.top <= canvas.getHeight()) {
                pot.marker.setBounds(left, top, right, bottom);
                pot.marker.draw(canvas);
                pot.marker.setBounds(markerBounds);

                this.visiblePointOverlays.add(index);
            }

            index++;
        }
    }
    
    @Override
    public void setOverlayTypes(List<PointOverlayType> overlayTypes) {
        this.pointOverlays.clear();
        pointOverlays.addAll(overlayTypes);
    }

    @Override
    public void clear() {
        pointOverlays.clear();
    }

    @Override
    public void removeOverlayType(OverlayType<? extends Geometry> overlayType) {
        this.pointOverlays.remove(overlayType);
        populate();
    }

    @Override
    public void addOverlayType(OverlayType<? extends Geometry> overlaytype) {
        this.pointOverlays.add((PointOverlayType) overlaytype);
        populate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addOverlayTypes(
            List<OverlayType<? extends Geometry>> overlaytypes) {
        this.pointOverlays
                .addAll((List<PointOverlayType>) (List<?>) overlaytypes);
        populate();
    }

    @Override
    public List<PointOverlayType> getOverlayTypes() {
        return new ArrayList<PointOverlayType>(pointOverlays);
    }
    
}
