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
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public class DataSourcePolygonOverlay extends
        DataSourceOverlay<Polygon, PolygonOverlayType> {

    private Paint defaultPaintFill;
    private Paint defaultPaintOutline;

    private final Path path = new Path();

    private Set<PolygonOverlayType> polygons = new HashSet<PolygonOverlayType>();

    public DataSourcePolygonOverlay() {
        super();
        this.path.setFillType(Path.FillType.EVEN_ODD);
    }

    @Override
    public void setOverlayTypes(List<PolygonOverlayType> overlayTypes) {
        synchronized (polygons) {
            this.polygons.clear();
            this.polygons.addAll(overlayTypes);
        }
        populate();
    }

    @Override
    public void addOverlayType(OverlayType<? extends Geometry> overlaytype) {
        synchronized (polygons) {
            polygons.add((PolygonOverlayType) overlaytype);
        }
        populate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addOverlayTypes(
            List<OverlayType<? extends Geometry>> overlaytypes) {
        synchronized(polygons){
            polygons.addAll((List<PolygonOverlayType>)(List<?>) overlaytypes);
        }
        populate();
    }

    @Override
    public void removeOverlayType(OverlayType<? extends Geometry> overlayType) {
        synchronized(polygons){
            this.polygons.remove(overlayType);
        }
        populate();
    }

    @Override
    public void clear() {
        synchronized(polygons){
            this.polygons.clear();
        }
        populate();
    }

    @Override
    protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            Projection projection, byte drawZoomLevel) {
        if (polygons.size() == 0)
            return;
        
        synchronized (polygons) {
            for (PolygonOverlayType polygon : polygons) {
                if (polygon.geometry == null
                        || polygon.geometry.getCoordinates().length <= 1)
                    continue;

                if (drawZoomLevel != polygon.cachedZoomLevel) {
                    android.graphics.Point point = new Point();
                    List<Coordinate> projectedCoordinates = new ArrayList<Coordinate>(
                            polygon.getGeometry().getNumPoints());
                    for (Coordinate coordinate : polygon.getGeometry()
                            .getCoordinates()) {
                        android.graphics.Point res = projection.toPoint(
                                new GeoPoint(coordinate.y, coordinate.x),
                                point, drawZoomLevel);
                        projectedCoordinates.add(new Coordinate(res.x, res.y));
                    }

                    // TODO XXX FIXME
//                    polygon.cachedPolygon = FACTORY.create
//                            .createPolygon(projectedCoordinates
//                                    .toArray(new Coordinate[projectedCoordinates
//                                            .size()]));

                    polygon.cachedZoomLevel = drawZoomLevel;
                }

                createPath(path, drawPosition, polygon.cachedPolygon);
                drawPath(path, canvas, polygon);
            }
        }
    }
    
    protected void createPath(final Path path, final Point drawPosition,
            final Polygon lineString) {
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
            PolygonOverlayType overlayType) {
        canvas.drawPath(path, overlayType.paintOutline);
        canvas.drawPath(path, overlayType.paintFill);
    }

    @Override
    public List<PolygonOverlayType> getOverlayTypes() {
        return new ArrayList<PolygonOverlayType>(polygons);
    }

}
