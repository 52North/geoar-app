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
//package org.n52.geoar.map.view;
//
//import org.mapsforge.android.maps.overlay.OverlayItem;
//import org.mapsforge.core.GeoPoint;
//import org.n52.geoar.newdata.DataSourceInstanceHolder;
//import org.n52.geoar.newdata.SpatialEntity2;
//import org.n52.geoar.newdata.Visualization;
//
//import android.graphics.drawable.Drawable;
//
//import com.vividsolutions.jts.geom.Geometry;
//
///**
// * 
// * @author Arne de Wall <a.dewall@52North.org>
// *
// */
//public class VisualizationOverlayItem extends OverlayItem {
//
//	private SpatialEntity2<? extends Geometry> mSpatialEntity;
//	private Visualization.FeatureVisualization mVisualization;
//	private DataSourceInstanceHolder mDataSourceInstance;
//
//	public VisualizationOverlayItem(GeoPoint point, String title,
//			String snippet, Drawable marker, SpatialEntity2<? extends Geometry> spatialEntity,
//			Visualization.FeatureVisualization visualization,
//			DataSourceInstanceHolder dataSourceInstance) {
//		super(point, title, snippet, marker);
//		mSpatialEntity = spatialEntity;
//		mVisualization = visualization;
//		mDataSourceInstance = dataSourceInstance;
//	}
//
//	public SpatialEntity2<? extends Geometry> getSpatialEntity() {
//		return mSpatialEntity;
//	}
//
//	public Visualization.FeatureVisualization getVisualization() {
//		return mVisualization;
//	}
//
//	public DataSourceInstanceHolder getDataSourceInstance() {
//		return mDataSourceInstance;
//	}
//	
//	
//}
