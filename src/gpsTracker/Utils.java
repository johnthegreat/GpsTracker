/*
 * MIT License
 *
 * Copyright (c) 2021 John Nahlen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package gpsTracker;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {
	public static double round(double value) {
		return round(value,6);
	}
	
	public static double round(double value,int precision) {
		return new BigDecimal(value).setScale(precision, RoundingMode.CEILING).doubleValue();
	}
	
	public static String locationToString(NMEA.GPSPosition location) {
		return SimpleDateFormat.getTimeInstance().format(new Date((long)location.time)) + " " + Utils.round(location.lat) + ", " + Utils.round(location.lon) + " " + location.quality + " " + location.velocity + "\n";
	}
	
	/*public static boolean areLocationsDifferent(final NMEA.GPSPosition loc1, final NMEA.GPSPosition loc2, final int precision) {
		final boolean latDiff = round(loc1.lat,precision) != round(loc2.lat,precision);
		final boolean longDiff = round(loc1.lon,precision) != round(loc2.lon,precision);
		return latDiff || longDiff;
	}*/

	public static boolean areLocationsDifferent(final NMEA.GPSPosition loc1, final NMEA.GPSPosition loc2) {
		// There needs to be a small but significant difference to say the position has changed.
		return Math.abs(loc1.lat - loc2.lat) >= 0.001 || Math.abs(loc1.lon - loc2.lon) >= 0.001;
	}

	public static String formatDate(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
		simpleDateFormat.setTimeZone(TimeZone.getDefault());
		return simpleDateFormat.format(date);
	}
}