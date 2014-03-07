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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public class DataSourcePolylineOverlay extends
        DataSourceOverlay<LineString, PolylineOverlayType> {

    private Paint defaultPaintFill;
    private Paint defaultPaintOutline;
    private final Path path = new Path();

    private Set<PolylineOverlayType> polylines = new HashSet<PolylineOverlayType>();

    public DataSourcePolylineOverlay() {
        super();
        this.path.setFillType(Path.FillType.EVEN_ODD);
    }

    @Override
    protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            Projection projection, byte drawZoomLevel) {
        if (polylines.size() == 0)
            return;

        synchronized (polylines) {
            for (PolylineOverlayType polyline : polylines) {
                if (polyline.geometry == null
                        || polyline.geometry.getCoordinates().length <= 1)
                    continue;

                if (drawZoomLevel != polyline.cachedZoomLevel) {
                    android.graphics.Point point = new Point();
                    List<Coordinate> projectedCoordinates = new ArrayList<Coordinate>(
                            polyline.getGeometry().getNumPoints());
                    for (Coordinate coordinate : polyline.getGeometry()
                            .getCoordinates()) {
                        android.graphics.Point res = projection.toPoint(
                                new GeoPoint(coordinate.y, coordinate.x),
                                point, drawZoomLevel);
                        projectedCoordinates.add(new Coordinate(res.x, res.y));
                    }

                    polyline.cachedLineString = FACTORY
                            .createLineString(projectedCoordinates
                                    .toArray(new Coordinate[projectedCoordinates
                                            .size()]));

                    polyline.cachedZoomLevel = drawZoomLevel;
                }

                createPath(path, drawPosition, polyline.cachedLineString);
                drawPath(path, canvas, polyline);
                path.rewind();
            }
        }
    }

    protected void createPath(final Path path, final Point drawPosition,
            final LineString lineString) {
        path.reset();
        boolean start = true;
        for (Coordinate coordinate : lineString.getCoordinates()) {
            if (start) {
                path.moveTo((float) coordinate.x - drawPosition.x,
                        (float) coordinate.y - drawPosition.y);
                start = false;
            } else {
                path.lineTo((float) coordinate.x - drawPosition.x,
                        (float) coordinate.y - drawPosition.y);
            }
        }
    }

    protected void drawPath(final Path path, final Canvas canvas,
            PolylineOverlayType overlayType) {
        canvas.drawPath(path, overlayType.paintOutline);
        canvas.drawPath(path, overlayType.paintFill);
    }

    @Override
    public void setOverlayTypes(List<PolylineOverlayType> overlayTypes) {
        synchronized (polylines) {
            this.polylines.clear();
            this.polylines.addAll(overlayTypes);
        }
        populate();
    }

    @Override
    public void clear() {
        synchronized (polylines) {
            this.polylines.clear();
        }
        populate();
    }

    @Override
    public void removeOverlayType(OverlayType<? extends Geometry> overlayType) {
        synchronized (polylines) {
            this.polylines.remove(overlayType);
        }
        populate();
    }

    @Override
    public void addOverlayType(OverlayType<? extends Geometry> overlaytype) {
        synchronized (polylines) {
            this.polylines.add((PolylineOverlayType) overlaytype);
        }
        populate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addOverlayTypes(
            List<OverlayType<? extends Geometry>> overlaytypes) {
        synchronized (polylines) {
            this.polylines
                    .addAll((List<PolylineOverlayType>) (List<?>) overlaytypes);
        }
        populate();
    }

    @Override
    public List<PolylineOverlayType> getOverlayTypes() {
        return new ArrayList<PolylineOverlayType>(polylines);
    }

}
