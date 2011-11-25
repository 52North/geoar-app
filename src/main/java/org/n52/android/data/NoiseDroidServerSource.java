/**
 * Copyright 2011 52°North Initiative for Geospatial Open Source Software GmbH
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
 * 
 */
package org.n52.android.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.ConnectException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.n52.android.geoar.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.graphics.RectF;
import android.location.Location;
import android.os.Build;
import android.util.Xml;

/**
 * Data source accesses the NoiseDroid web services to get measurement data.
 * Parts of this class relate to the NoiseDroid project source
 * 
 * @author Holger Hopmann
 * 
 */
public class NoiseDroidServerSource implements DataSource {

	private static final String SERVER_URL = "http://giv-noismappin1.uni-muenster.de:8080/NoiseServerServlets/AppServlet";
	private static DefaultHttpClient httpClient;

	static {
		initHttpClient();
	}

	public static Calendar stringToCalendar(String timeString) {
		Locale locale = Locale.GERMAN;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
				locale);
		try {
			Date d = format.parse(timeString);
			Calendar c = new GregorianCalendar();
			c.setTime(d);

			return c;
		} catch (ParseException e) {
			return null;
		}
	}

	public static String calendarToString(Calendar timeCalendar) {
		if (timeCalendar != null) {
			Locale locale = Locale.GERMAN;
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss", locale);
			return sdf.format(timeCalendar.getTime());
		} else
			return "";
	}

	private static void initHttpClient() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParameters, 10000);
		HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
		ClientConnectionManager cm = new ThreadSafeClientConnManager(
				httpParameters, registry);
		httpClient = new DefaultHttpClient(cm, httpParameters);
	}

	private static ByteArrayOutputStream createRequestXML(Tile tile,
			MeasurementFilter filter) throws RequestException {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();

			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(stream, "UTF-8");
			serializer.setFeature(
					"http://xmlpull.org/v1/doc/features.html#indent-output",
					Boolean.TRUE);
			serializer.startDocument(null, Boolean.TRUE);

			serializer.startTag(null, "Requests");

			// Selbstbeschreibung
			serializer.startTag(null, "Selfdescription");
			serializer.startTag(null, "Time");
			serializer
					.attribute(null, "value", "" + System.currentTimeMillis());
			serializer.endTag(null, "Time");

			serializer.startTag(null, "Device");
			serializer.attribute(null, "value", "" + Build.PRODUCT);
			serializer.endTag(null, "Device");

			serializer.endTag(null, "Selfdescription");

			// Request
			serializer.startTag(null, "Request");
			serializer.attribute(null, "type", "GetMeasures");
			serializer.startTag(null, "Filter");

			RectF bBoxE6 = tile.getLLBBox();
			bBoxE6.sort();
			if (bBoxE6 != null) {
				bBoxE6.sort();
				// top / bottom vertauschen da durch sort() top <= bottom
				// gilt, geografische Koordinaten.
				if (!bBoxE6.isEmpty()) {
					serializer.startTag(null, "BBox");
					serializer.attribute(null, "top", "" + bBoxE6.bottom);
					serializer.attribute(null, "bottom", "" + bBoxE6.top);
					serializer.attribute(null, "left", "" + bBoxE6.left);
					serializer.attribute(null, "right", "" + bBoxE6.right);
					serializer.endTag(null, "BBox");
				}
			}
			if (filter.timeFrom != null || filter.timeTo != null) {
				serializer.startTag(null, "Time");
				if (filter.timeFrom != null)
					serializer.attribute(null, "from", ""
							+ calendarToString(filter.timeFrom));
				if (filter.timeTo != null)
					serializer.attribute(null, "to", ""
							+ calendarToString(filter.timeTo));
				serializer.endTag(null, "Time");
			}
			serializer.endTag(null, "Filter");

			serializer.endTag(null, "Request");

			serializer.endTag(null, "Requests");
			serializer.endDocument();
			serializer.flush();
			stream.close();
			return stream;
		} catch (Exception e) {
			throw new RequestException(e.getMessage());
		}
	}

	private static List<Measurement> downloadMeasurements(Tile tile,
			MeasurementFilter filter) throws RequestException, ConnectException {
		try {
			HttpPost httpPost = new HttpPost(SERVER_URL);
			ByteArrayEntity requestEntity = new ByteArrayEntity(
					createRequestXML(tile, filter).toByteArray());
			requestEntity.setContentType("text/xml");
			httpPost.setEntity(requestEntity);

			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity responseEntity = response.getEntity();
			return getMeasuresFromResponse(responseEntity.getContent());
		} catch (ClientProtocolException e) {
			throw new ConnectException(e.getMessage());
		} catch (IOException e) {
			throw new ConnectException(e.getMessage());
		} catch (Exception e) {
			throw new RequestException(e.getMessage());
		}
	}

	public static Measurement getMeasureFromXML(XmlPullParser parser)
			throws MeasureParserException {
		try {
			Measurement m = new Measurement();

			while (true) {
				if (parser.getEventType() == XmlPullParser.START_TAG) {
					if (parser.getName().equals("Time")) {
						m.setTime(stringToCalendar(parser.getAttributeValue(
								null, "value")));
					} else if (parser.getName().equals("LocationMeasure")) {
						Location location = new Location("");
						location.setLongitude(Double.parseDouble(parser
								.getAttributeValue(null, "longitude")));
						location.setLatitude(Double.parseDouble(parser
								.getAttributeValue(null, "latitude")));
						location.setAccuracy(Float.parseFloat(parser
								.getAttributeValue(null, "accuracy")));
						location.setProvider(parser.getAttributeValue(null,
								"provider"));
						m.setLocation(location);
					} else if (parser.getName().equals("NoiseMeasure")) {
						m.setNoise(Float.parseFloat(parser.getAttributeValue(
								null, "value")));
					}
				}

				// Abbruch
				if (parser.getEventType() == XmlPullParser.END_TAG
						&& parser.getName().equals("Measure")) {
					break;
				}

				parser.next();
			}
			return m;
		} catch (Exception e) {
			throw new MeasureParserException(e.getMessage());
		}
	}

	private static List<Measurement> getMeasuresFromParser(XmlPullParser parser)
			throws RequestException {
		try {
			List<Measurement> measureResultList = new ArrayList<Measurement>();
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (parser.getName().equals("Measure")) {
						measureResultList.add(getMeasureFromXML(parser));
					} else if (parser.getName().equals("Error")) {
						return null;
					}
				}
				eventType = parser.next();
			}
			parser.setInput(null);
			return measureResultList;
		} catch (Exception e) {
			throw new RequestException(e.getMessage());
		}
	}

	public static List<Measurement> getMeasuresFromResponse(Reader reader)
			throws RequestException {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(reader);
			return getMeasuresFromParser(parser);
		} catch (Exception e) {
			throw new RequestException(e.getMessage());
		}
	}

	public static List<Measurement> getMeasuresFromResponse(
			InputStream inputStream) throws RequestException {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(inputStream, null);
			return getMeasuresFromParser(parser);
		} catch (Exception e) {
			throw new RequestException(e.getMessage());
		}
	}

	// DataSource implementation

	public String getTitle() {
		return "NoiseDroid Server";
	}

	public boolean isAvailable() {
		return true;
	}

	public byte getPreferredRequestZoom() {
		return 14;
	}

	public List<Measurement> getMeasurements(Tile tile, MeasurementFilter filter)
			throws ConnectException, RequestException {
		List<Measurement> measurements = downloadMeasurements(tile, filter);
		if (filter.hourFrom != null || filter.hourTo != null) {
			// Check if filter matches for all measurements if hourFrom/hourTo
			// exists
			Iterator<Measurement> iterator = measurements.iterator();
			while (iterator.hasNext()) {
				if (!filter.filter(iterator.next())) {
					iterator.remove();
				}
			}
		}
		return measurements;
	}

	public long getDataReloadMinInterval() {
		// 2 Minuten
		return 120 * 1000;
	}

	public Integer getIconDrawableId() {
		return R.drawable.noisedroid;
	}

}
