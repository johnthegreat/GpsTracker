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

import net.sf.marineapi.nmea.util.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {
	public static double round(double value,int precision) {
		return new BigDecimal(value).setScale(precision, RoundingMode.CEILING).doubleValue();
	}

	public static boolean areLocationsDifferent(final Position loc1, final Position loc2, long threshold) {
		// https://www.javadoc.io/static/net.sf.marineapi/marineapi/0.11.0/net/sf/marineapi/nmea/util/Position.html
		// distanceTo returns distance in meters
		return loc1.distanceTo(loc2) >= threshold;
	}

	public static String formatDate(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
		simpleDateFormat.setTimeZone(TimeZone.getDefault());
		return simpleDateFormat.format(date);
	}
}