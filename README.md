# GpsTracker

This project reads latitude/longitude coordinates from an NMEA-compatible GPS device via serial port and uploads them in JSON format to a server on regular intervals.

Tested with a "GlobalSat BU-353-S4".

## Arguments

`--list`

List currently connected serial ports. This is the "Serial Port Name" in the next section.

`<serial port name> <device name> <upload url> <interval>`

- Serial Port Name: Name of the serial port to connect to (see `--list`)
- Device Name: Name of your device
- Upload Url: URL to upload to
- Interval: How often to upload new coordinates (in seconds)

## Dependencies

Utilizes the following Maven packages (see `pom.xml`):

- com.fazecast.jSerialComm
- org.json.json

## Notes

- Written to be compatible with Java language level 8.
- Tested on Windows and Mac.
- Assumes a fixed baud rate of 4800.

## License

Copyright &copy; 2021 John Nahlen

MIT License

See `LICENSE.txt`