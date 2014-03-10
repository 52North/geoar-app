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
import org.n52.geoar.newdata.Visualization;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 * 
 */
public abstract class OverlayType<G extends Geometry> {

    private final SpatialEntity2<? extends Geometry> mSpatialEntity;
    private final Visualization.FeatureVisualization mVisualization;
    private final DataSourceInstanceHolder mDataSourceInstance;

    protected String title;
    protected String description;
    protected byte cachedZoomLevel;

    protected G geometry;

    public OverlayType(G geometry, String title, String description,
            SpatialEntity2<? extends Geometry> spatialEntity,
            Visualization.FeatureVisualization visualization,
            DataSourceInstanceHolder dataSourceInstance) {
        this.title = title;
        this.description = description;
        this.geometry = geometry;
        
        this.mSpatialEntity = spatialEntity;
        this.mVisualization = visualization;
        this.mDataSourceInstance = dataSourceInstance;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    protected G getGeometry() {
        return this.geometry;
    }
    
    public SpatialEntity2<? extends Geometry> getSpatialEntity() {
        return mSpatialEntity;
    }

    public Visualization.FeatureVisualization getVisualization() {
        return mVisualization;
    }

    public DataSourceInstanceHolder getDataSourceInstance() {
        return mDataSourceInstance;
    }
}
