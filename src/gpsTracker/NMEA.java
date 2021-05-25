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

// NOTICE: This code was adapted from https://gist.github.com/kollerfuzzi/744ec8fd3c091dd899de2a872220cbe2

package gpsTracker;

import java.util.HashMap;
import java.util.Map;

public class NMEA {

    interface SentenceParser {
        void parse(String[] tokens, GPSPosition position);
    }

    static float Latitude2Decimal(String lat, String NS) {
        if (lat == null || lat.length() == 0) {
            return 0f;
        }

        float med = Float.parseFloat(lat.substring(2)) / 60.0f;
        med += Float.parseFloat(lat.substring(0, 2));
        if (NS.startsWith("S")) {
            med = -med;
        }
        return med;
    }

    static float Longitude2Decimal(String lon, String WE) {
        if (lon == null || lon.length() == 0) {
            return 0f;
        }

        float med = Float.parseFloat(lon.substring(3)) / 60.0f;
        med += Float.parseFloat(lon.substring(0, 3));
        if (WE.startsWith("W")) {
            med = -med;
        }
        return med;
    }

    // parsers
    static class GPGGA implements SentenceParser {

        public void parse(String[] tokens, GPSPosition position) {
            position.time = Float.parseFloat(tokens[1]);
            position.lat = Latitude2Decimal(tokens[2], tokens[3]);
            position.lon = Longitude2Decimal(tokens[4], tokens[5]);
            position.quality = Integer.parseInt(tokens[6]);
            position.altitude = Float.parseFloat(tokens[9]);
        }
    }

    static class GPGGL implements SentenceParser {

        public void parse(String[] tokens, GPSPosition position) {
            position.lat = Latitude2Decimal(tokens[1], tokens[2]);
            position.lon = Longitude2Decimal(tokens[3], tokens[4]);
            position.time = Float.parseFloat(tokens[5]);
        }
    }

    static class GPRMC implements SentenceParser {

        public void parse(String[] tokens, GPSPosition position) {
            position.time = Float.parseFloat(tokens[1]);
            position.lat = Latitude2Decimal(tokens[3], tokens[4]);
            position.lon = Longitude2Decimal(tokens[5], tokens[6]);
            if (tokens[7].length() > 0) {
                position.velocity = Float.parseFloat(tokens[7]);
            }
            if (tokens[8].length() > 0) {
                position.dir = Float.parseFloat(tokens[8]);
            }
        }
    }

    static class GPVTG implements SentenceParser {

        public void parse(String[] tokens, GPSPosition position) {
            position.dir = Float.parseFloat(tokens[3]);
        }
    }

    static class GPRMZ implements SentenceParser {

        public void parse(String[] tokens, GPSPosition position) {
            position.altitude = Float.parseFloat(tokens[1]);
        }
    }

    public static class GPSPosition {

        public float time = 0.0f;
        public float lat = 0.0f;
        public float lon = 0.0f;
        public boolean fixed = false;
        public int quality = 0;
        public float dir = 0.0f;
        public float altitude = 0.0f;
        public float velocity = 0.0f;

        public void updatefix() {
            fixed = quality > 0;
        }

        public String toString() {
            return String.format("POSITION: lat: %f, lon: %f, time: %f, Q: %d, dir: %f, alt: %f, vel: %f", lat, lon, time, quality, dir, altitude, velocity);
        }
    }

    private static final Map<String, SentenceParser> sentenceParsers;

    static {
        sentenceParsers = new HashMap<String, SentenceParser>();
        sentenceParsers.put("GPGGA", new GPGGA());
        sentenceParsers.put("GPGGL", new GPGGL());
        sentenceParsers.put("GPRMC", new GPRMC());
        sentenceParsers.put("GPRMZ", new GPRMZ());
        //only really good GPS devices have this sentence but ...
        sentenceParsers.put("GPVTG", new GPVTG());


        // $GPGGA,201039.000,4337.0350,N,11640.8126,W,1,11,0.8,756.2,M,-18.8,M,,0000*6F
        //sentenceParsers.put("GPGSA", new GPGSA());
        // $GPGSV,3,1,12,32,88,156,36,14,65,219,35,10,56,074,39,18,44,304,25*76
        //sentenceParsers.put("GPGSV", new GPGSV());
    }

    public static GPSPosition parse(String line) {
        GPSPosition position = null;
        if (line.startsWith("$") && line.contains("*")) {
            String nmea = line.substring(1);
            int listedChecksum = Integer.parseInt(nmea.substring(nmea.indexOf("*") + 1), 16);
            int computedChecksum = calculateChecksum(nmea);
            if (listedChecksum != computedChecksum) {
                // Invalid checksum
                return null;
            }
            String[] tokens = nmea.split(",");
            String type = tokens[0];
            if (sentenceParsers.containsKey(type)) {
                position = new GPSPosition();
                try {
                    sentenceParsers.get(type).parse(tokens, position);
                    position.updatefix();
                } catch (NumberFormatException exc) {
                    position = null;
                }
            }
        }
        return position;
    }

    //
    //
    //
    public static int calculateChecksum(String line) {
        // https://rietman.wordpress.com/2008/09/25/how-to-calculate-the-nmea-checksum/
        int idx = line.charAt(0) == '$' ? 1 : 0;
        line = line.substring(idx, line.indexOf("*"));

        int checksum = 0;
        for (int i = 0; i < line.length(); i++) {
            checksum ^= line.codePointAt(i);
        }
        return checksum;
    }
}
