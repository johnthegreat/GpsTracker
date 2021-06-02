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
import net.sf.marineapi.nmea.util.Position;

public class GpsTracker {
    private final GpsTrackerConfig gpsTrackerConfig;
    protected Position lastPosition = null;
    protected Position lastPositionUploaded = null;
    protected SerialPort comPort;

    public GpsTrackerConfig getGpsTrackerConfig() {
        return gpsTrackerConfig;
    }

    public GpsTracker(GpsTrackerConfig gpsTrackerConfig, SerialPort serialPort, int baudRate) {
        this.gpsTrackerConfig = gpsTrackerConfig;
        this.comPort = serialPort;
        this.comPort.setBaudRate(baudRate);
        // https://github.com/Fazecast/jSerialComm/wiki/Java-InputStream-and-OutputStream-Interfacing-Usage-Example
        // Use TIMEOUT_READ_SEMI_BLOCKING, per the docs, to get InputStream working correctly
        this.comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING,0,0);
        this.comPort.openPort();
    }

    public SerialPort getComPort() {
        return comPort;
    }
}
