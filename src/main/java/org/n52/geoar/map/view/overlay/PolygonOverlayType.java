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

import org.n52.geoar.newdata.DataSourceInstanceHolder;
import org.n52.geoar.newdata.SpatialEntity2;
import org.n52.geoar.newdata.Visualization.FeatureVisualization;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class PolygonOverlayType extends OverlayType<Polygon>{
    
    private static Paint staticPaintFill;
    private static Paint staticPaintOutline;
    
    static{
        staticPaintFill = new Paint();
        staticPaintFill.setStyle(Style.FILL);
        staticPaintFill.setStrokeWidth(5);
        staticPaintFill.setColor(Color.BLUE);
        staticPaintFill.setAlpha(120);
        staticPaintFill.setAntiAlias(true);
        staticPaintOutline = new Paint();
        staticPaintOutline.setStyle(Style.STROKE);
        staticPaintOutline.setColor(Color.BLUE);
        staticPaintOutline.setAlpha(200);
        staticPaintOutline.setAntiAlias(true);
    }

    protected Paint paintFill;
    protected Paint paintOutline;
    
    protected Polygon cachedPolygon;
    
    public PolygonOverlayType(Polygon geometry, String title,
            String description,
            SpatialEntity2<? extends Geometry> spatialEntity,
            FeatureVisualization visualization,
            DataSourceInstanceHolder dataSourceInstance) {
        super(geometry, title, description, spatialEntity, visualization,
                dataSourceInstance);
    }

}
