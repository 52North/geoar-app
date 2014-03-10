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
package org.n52.geoar.exception;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class UnsupportedGeometryType extends Exception {
    private static final long serialVersionUID = 1L;
    
    public UnsupportedGeometryType(String classType){
        super(String.format("The Geometry Type: \"%s\" is not supported", classType));
    }
}
