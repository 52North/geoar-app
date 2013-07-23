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

import java.util.List;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;

import android.graphics.Canvas;
import android.graphics.Point;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public abstract class DataSourceOverlay<G extends Geometry, T extends OverlayType<G>>
        extends Overlay {

    public DataSourceOverlay() {

    }
    
    public abstract void setOverlayTypes(List<T> overlayTypes);

    public abstract void addOverlayType(OverlayType<? extends Geometry> overlaytype);
    
    public abstract void addOverlayTypes(List<OverlayType<? extends Geometry>> overlaytypes);

    public abstract void removeOverlayType(OverlayType<? extends Geometry> overlayType);
    
    public abstract void clear();

    protected abstract void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            Projection projection, byte zoomLevel);

    protected void populate() {
        super.requestRedraw();
    }
}
