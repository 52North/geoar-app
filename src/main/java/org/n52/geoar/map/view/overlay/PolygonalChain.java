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
//package org.n52.geoar.map.view.overlay;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//import org.mapsforge.core.GeoPoint;
//import org.mapsforge.core.model.Point;
//import org.mapsforge.core.util.MercatorProjection;
//
//import android.graphics.Path;
//
///**
// * 
// * @author Arne de Wall <a.dewall@52North.org>
// * 
// */
//public class PolygonalChain {
//    private final List<GeoPoint> geoPoints;
//
//    public PolygonalChain(Collection<GeoPoint> geoPoints) {
//        this.geoPoints = (geoPoints == null) ? Collections
//                .synchronizedList(new ArrayList<GeoPoint>()) : Collections
//                .synchronizedList(new ArrayList<GeoPoint>(geoPoints));
//    }
//    
//    protected Path draw(byte zoomLevel, Point canvasPosition, boolean closeAutomatically){
//        if(this.geoPoints.size() <= 1){
//            return null;
//        }
//        
//        Path path = new Path();
//        boolean indexed = false;
//        for(GeoPoint gp : geoPoints){
//            float pixelX = (float) (MercatorProjection.longitudeToPixelX(gp.getLongitude(), zoomLevel));
//            float pixelY = (float) (MercatorProjection.latitudeToPixelY(gp.getLatitude(), zoomLevel));
//            
//            if(indexed){
//                path.lineTo(pixelX, pixelY);
//            } else {
//                path.moveTo(pixelX, pixelY);
//                indexed = true;
//            }
//        }
//        return path;
//    }
//    
//}
