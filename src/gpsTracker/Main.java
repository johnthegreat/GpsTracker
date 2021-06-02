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
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.PositionListener;

import java.util.*;

public class Main {
	private static boolean isShuttingDown = false;
	private static final Set<GpsPositionListener> gpsPositionListeners = new HashSet<>();
	private static final int differenceThresholdMeters = 33;

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

		beginReadingFromGps(gpsTracker);

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

	public static void triggerGpsPositionListeners(final Position gpsPosition) {
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

				final Position position = gpsTracker.lastPosition;
				if (position == null) {
					return;
				}

				if (gpsTracker.lastPositionUploaded != null && !Utils.areLocationsDifferent(gpsTracker.lastPositionUploaded, position, differenceThresholdMeters)) {
					return;
				}
				UploadService.getInstance().upload(gpsTracker, position);
			}
		};
	}
	
	public static void beginReadingFromGps(final GpsTracker gpsTracker) {
		final SentenceReader sentenceReader = new SentenceReader(gpsTracker.getComPort().getInputStream());
		final PositionProvider positionProvider = new PositionProvider(sentenceReader);
		positionProvider.addListener(new PositionListener() {
			@Override
			public void providerUpdate(PositionEvent positionEvent) {
				final Position position = positionEvent.getPosition();
				if (gpsTracker.lastPosition == null || Utils.areLocationsDifferent(gpsTracker.lastPosition, position, differenceThresholdMeters)) {
					System.out.printf("%s (%f,%f)%n", Utils.formatDate(new Date()), position.getLatitude(), position.getLongitude());

					gpsTracker.lastPosition = position;
					triggerGpsPositionListeners(position);
				}
			}
		});

		sentenceReader.start();
	}
}
