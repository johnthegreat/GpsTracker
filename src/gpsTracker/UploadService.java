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
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

public class UploadService {
    private static final UploadService instance = new UploadService();

    public static UploadService getInstance() {
        return instance;
    }

    private UploadService() {}

    public void upload(final GpsTracker gpsTracker, final Position position) {
        if (gpsTracker == null || position == null) {
            return;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("deviceName", gpsTracker.getGpsTrackerConfig().getDeviceName());
        jsonObject.put("timestamp", calendar.getTime().getTime() / 1000);
        jsonObject.put("latitude", Utils.round(position.getLatitude(), 5));
        jsonObject.put("longitude", Utils.round(position.getLongitude(), 5));
        jsonObject.put("altitude", position.getAltitude());

        try {
            uploadJsonToUrl(gpsTracker.getGpsTrackerConfig().getUploadUrl(), jsonObject.toString());
            gpsTracker.lastPositionUploaded = position;
            System.out.println("Uploaded");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private void uploadJsonToUrl(final String uploadUrl,final String json) throws IOException {
        final String charset = "UTF-8";

        final URL url = new URL(uploadUrl);
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try(OutputStream output = connection.getOutputStream()) {
            output.write(json.getBytes(charset));
        }

        try(InputStream responseInputStream = connection.getInputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseInputStream));
            while(reader.ready()) {
                System.out.println(reader.readLine());
            }
        }

        connection.disconnect();
    }
}
