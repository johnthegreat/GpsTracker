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

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;

public class GpsTracker {
    private final GpsTrackerConfig gpsTrackerConfig;
    protected NMEA.GPSPosition lastPosition = null;
    protected NMEA.GPSPosition lastPositionUploaded = null;
    protected SerialPort comPort;

    public GpsTrackerConfig getGpsTrackerConfig() {
        return gpsTrackerConfig;
    }

    public GpsTracker(GpsTrackerConfig gpsTrackerConfig, SerialPort serialPort, int baudRate) {
        this.gpsTrackerConfig = gpsTrackerConfig;
        this.comPort = serialPort;
        this.comPort.setBaudRate(baudRate);
        this.comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING,2000,2000);
        this.comPort.openPort();
    }

    public SerialPort getComPort() {
        return comPort;
    }

    public String readFromGpsRaw() throws IOException {
        final InputStream in = comPort.getInputStream();
        final StringBuilder builder = new StringBuilder(100);
        char c;
        do {
            if (!comPort.isOpen()) {
                break;
            }

            c = (char)in.read();
            builder.append(c);
            if (c == 10) {
                break;
            }
        } while (c != 0);
        return builder.toString().trim();
    }
}
