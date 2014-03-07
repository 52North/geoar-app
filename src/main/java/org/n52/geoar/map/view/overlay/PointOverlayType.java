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

import org.mapsforge.core.GeoPoint;
import org.n52.geoar.newdata.DataSourceInstanceHolder;
import org.n52.geoar.newdata.SpatialEntity2;
import org.n52.geoar.newdata.Visualization;

import android.graphics.drawable.Drawable;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public class PointOverlayType extends OverlayType<Point> {

    protected Drawable marker;
    protected android.graphics.Point cachedMapPosition;

    public PointOverlayType(Point geometry, String title, String description,
            SpatialEntity2<? extends Geometry> spatialEntity,
            Visualization.FeatureVisualization visualization,
            DataSourceInstanceHolder dataSourceInstance) {
        super(geometry, title, description, spatialEntity, visualization, dataSourceInstance);
    }

    public PointOverlayType(Point geometry, String title, String description,
            Drawable marker, SpatialEntity2<? extends Geometry> spatialEntity,
            Visualization.FeatureVisualization visualization,
            DataSourceInstanceHolder dataSourceInstance) {
        super(geometry, title, description, spatialEntity, visualization, dataSourceInstance);
        this.marker = marker;
    }

    public GeoPoint getPoint() {
        Point p = geometry.getCentroid();
        return new GeoPoint(p.getY(), p.getX());
    }

    public android.graphics.Point getCachedMapPosition() {
        return this.cachedMapPosition;
    }

}
