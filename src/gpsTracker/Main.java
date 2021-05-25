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
import java.util.*;

public class Main {
	private static boolean isShuttingDown = false;
	private static final Set<GpsPositionListener> gpsPositionListeners = new HashSet<>();

	public static void main(String[] args) {
		System.out.println("GpsTracker 1.0");
		System.out.println("Copyright (c) 2021 John Nahlen\n");

		SerialPort[] serialPorts = SerialPort.getCommPorts();
		if (serialPorts.length == 0) {
			System.err.println("No serial port appears to be connected.");
			System.exit(0);
		}

		if (args.length == 1 && args[0].equals("--list")) {
			for (final SerialPort serialPort : serialPorts) {
				System.out.println(serialPort.getDescriptivePortName());
			}
			System.exit(0);
		} else if (args.length < 4) {
			System.err.println("Arguments: <serial port name> <device name> <upload url> <interval>");
			System.exit(0);
		}

		final String serialPortName = args[0];
		serialPorts = filterSerialPortsByName(serialPorts, serialPortName);
		if (serialPorts.length == 0) {
			System.err.println("Unable to locate serial port by that name.");
			System.exit(1);
		}

		final String deviceName = args[1];
		final String uploadUrl = args[2];
		int secondsInterval = 60;
		try {
			secondsInterval = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			e.printStackTrace(System.err);
		}

		final GpsTrackerConfig gpsTrackerConfig = new GpsTrackerConfig();
		gpsTrackerConfig.setSerialPortName(serialPortName);
		gpsTrackerConfig.setDeviceName(deviceName);
		gpsTrackerConfig.setUploadUrl(uploadUrl);
		gpsTrackerConfig.setInterval(secondsInterval);
		
		final SerialPort serialPort = serialPorts[0];

		final GpsTracker gpsTracker = new GpsTracker(gpsTrackerConfig, serialPort, 4800);
		
		final Thread pollingThread = createPollingThread(gpsTracker);
		pollingThread.start();

		final Timer timer = new Timer();
		final TimerTask uploadTask = createUploadTimerTask(gpsTracker);
		timer.scheduleAtFixedRate(uploadTask,0,secondsInterval * 1000L);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				isShuttingDown = true;
				uploadTask.cancel();

				final SerialPort serialPort = gpsTracker.getComPort();
				try {
					if (serialPort != null) {
						if (serialPort.getInputStream() != null) {
							serialPort.getInputStream().close();
						}
						serialPort.closePort();
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}));

		final GpsPositionListener uploadOnFirstFixListener = new GpsPositionListener() {
			@Override
			public void run() {
				UploadService.getInstance().upload(gpsTracker, this.getGpsPosition());
				gpsPositionListeners.remove(this);
			}
		};
		gpsPositionListeners.add(uploadOnFirstFixListener);
	}

	public static void triggerGpsPositionListeners(NMEA.GPSPosition gpsPosition) {
		if (gpsPositionListeners.size() == 0) {
			return;
		}

		for(final GpsPositionListener gpsPositionListener : gpsPositionListeners) {
			gpsPositionListener.setGpsPosition(gpsPosition);
			gpsPositionListener.run();
		}
	}

	public static SerialPort[] filterSerialPortsByName(final SerialPort[] serialPorts, final String name) {
		final Set<SerialPort> filteredSerialPorts = new HashSet<>(serialPorts.length);
		for(final SerialPort serialPort : serialPorts) {
			if (serialPort.getDescriptivePortName().equals(name)) {
				filteredSerialPorts.add(serialPort);
			}
		}
		return filteredSerialPorts.toArray(new SerialPort[0]);
	}
	
	public static TimerTask createUploadTimerTask(final GpsTracker gpsTracker) {
		return new TimerTask() {
			@Override
			public void run() {
				if (isShuttingDown) {
					return;
				}

				final NMEA.GPSPosition position = gpsTracker.lastPosition;
				if (position == null) {
					return;
				}

				if (gpsTracker.lastPositionUploaded != null && !Utils.areLocationsDifferent(gpsTracker.lastPositionUploaded, position)) {
					return;
				}
				UploadService.getInstance().upload(gpsTracker, position);
			}
		};
	}
	
	public static Thread createPollingThread(final GpsTracker gpsTracker) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				while(gpsTracker.getComPort().isOpen()) {
					if (isShuttingDown) {
						break;
					}

					String line = null;
					try {
						line = gpsTracker.readFromGpsRaw();
					} catch (IOException e) {
						e.printStackTrace(System.err);
					}

					if (line == null) {
						continue;
					}

					NMEA.GPSPosition gpsPosition = null;
					try {
						gpsPosition = NMEA.parse(line);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}

					if (gpsPosition != null) {
						if (!gpsPosition.fixed) {
							continue;
						}

						if (gpsPosition.lat == 0f && gpsPosition.lon == 0f) {
							continue;
						}

						if (gpsTracker.lastPosition == null || Utils.areLocationsDifferent(gpsTracker.lastPosition, gpsPosition)) {
							System.out.printf("%s (%f,%f)%n", Utils.formatDate(new Date()), gpsPosition.lat, gpsPosition.lon);

							gpsTracker.lastPosition = gpsPosition;
							triggerGpsPositionListeners(gpsPosition);
						}
					}
				}
			}
		});
	}
}
