
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

/**
 * Connects to a serial port to communicate with an NES controller via Arduino.
 * The NES controller has 8 buttons; the state of the controller is stored as a
 * byte (with 1 = pressed, 0 = not pressed), and the Arduino sends the state
 * over the serial port when the state has changed. The change in state then
 * triggers one or more calls to the buttonPressed or buttonReleased methods
 * which are implementation-dependent. View the "arduinoCode" file for the
 * arduino code to be used. Some code from:
 * http://playground.arduino.cc/Interfacing/Java
 *
 * @author Tom
 */
public class NesPad implements SerialPortEventListener {

    private byte buttons; //The current button state

    /**
     * Enumerates the buttons on an NES pad. This ordering is necessary and
     * reflects the order they appear in the byte storing the state received
     * from the shift register (from least-significant to most)
     */
    public enum Button {
        A, B, SELECT, START, UP, DOWN, LEFT, RIGHT;
    }

    private SerialPort serialPort = null;
    private static final String PORT_NAMES[] = {
        //        "/dev/tty.usbmodem", // Mac OS X
        //        "/dev/usbdev", // Linux
        //        "/dev/tty", // Linux
        //        "/dev/serial", // Linux
        "COM3", // Windows
    };
    private String appName;
    private InputStream input;
    private static final int TIME_OUT = 1000; // Port open timeout
    private static final int DATA_RATE = 9600; // Arduino serial port

    public byte getButtons() {
        return buttons;
    }

    /**
     * To be invoked from the serial event handler. Should use custom events
     * instead, but for now, override these events in your instance.
     *
     * @param b the button that was pressed/released
     */
    public void buttonPressed(Button b) {}
    public void buttonReleased(Button b) {}
    public void APressed() {}
    public void BPressed() {}
    public void SelectPressed() {}
    public void StartPressed() {}
    public void UpPressed() {}
    public void DownPressed() {}
    public void RightPressed() {}
    public void LeftPressed() {}
    public void AReleased() {}
    public void BReleased() {}
    public void SelectReleased() {}
    public void StartReleased() {}
    public void UpReleased() {}
    public void DownReleased() {}
    public void LeftReleased() {}
    public void RightReleased() {}

    public boolean isAPressed() {
        return (buttons & 0b00000001) == 0b00000001;
    }

    public boolean isBPressed() {
        return (buttons & 0b00000010) == 0b00000010;
    }

    public boolean isSelectPressed() {
        return (buttons & 0b00000100) == 0b00000100;
    }

    public boolean isStartPressed() {
        return (buttons & 0b00001000) == 0b00001000;
    }

    public boolean isUpPressed() {
        return (buttons & 0b00010000) == 0b00010000;
    }

    public boolean isDownPressed() {
        return (buttons & 0b00100000) == 0b00100000;
    }

    public boolean isLeftPressed() {
        return (buttons & 0b01000000) == 0b01000000;
    }

    public boolean isRightPressed() {
        return (buttons & 0b10000000) == 0b10000000;
    }

    /**
     * Determines if a particular button is currently held down.
     *
     * @param b the Button of interest
     * @return true if the button is held down, false otherwise
     */
    public boolean isButtonPressed(Button b) {
        return (buttons & (1 << b.ordinal())) == (1 << b.ordinal());
    }

    /**
     * Helper method for quitting a program.
     *
     * @return true if only A, B, Start, and Select are currently held down.
     */
    public boolean quit() {
        return buttons == 0b00001111;
    }

    /**
     * Attempts to initialize the connection to the serial port
     *
     * @return true if successful connection, false otherwise
     */
    public boolean initialize() {
        try {
            CommPortIdentifier portId = null;
            Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
            // Enumerate system ports and try connecting to Arduino over each
            System.out.println("Trying:");
            while (portId == null && portEnum.hasMoreElements()) {
                // Iterate through your host computer's serial port IDs
                CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                System.out.println("   port" + currPortId.getName());
                for (String portName : PORT_NAMES) {
                    if (currPortId.getName().equals(portName)
                            || currPortId.getName().startsWith(portName)) {
                        // Try to connect to the Arduino on this port
                        serialPort = (SerialPort) currPortId.open(appName, TIME_OUT);
                        portId = currPortId;
                        System.out.println("Connected on port" + currPortId.getName());
                        break;
                    }
                }
            }

            if (portId == null || serialPort == null) {
                System.out.println("Oops... Could not connect to Arduino");
                return false;
            }

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            // Give the Arduino some time
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
            }

            return true;
        } catch (PortInUseException | UnsupportedCommOperationException | TooManyListenersException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    /**
     * Processes a serial port event. This method is called when some data is
     * sent over the serial port (i.e. from the Arduino). The Arduino, in this
     * case, only sends a new byte when it's different from the last one. We can
     * compare the byte sent from the Arduino (the next state) with the last
     * recorded state and determine which button has been pressed or released,
     * and invoke the appropriate method.
     *
     * @param event
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        try {
            switch (event.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
                    input = serialPort.getInputStream();
                    byte nextState = (byte) input.read();

                    //Mask the bytes 8 times for each bit and compare
                    for (int i = 0; i < 8; i++) {
                        //Can consider their difference rather than using xor
                        if (((nextState & (1 << i)) - (buttons & (1 << i))) != 0) {
                            //If the bit in nextState was 1, that means it was
                            //0 before, so it has just been pressed.
                            if ((nextState & (1 << i)) == (1 << i)) {
                                buttonPressed(Button.values()[i]);
                            } else {
                                buttonReleased(Button.values()[i]);
                            }
                        }
                    }
                    buttons = nextState;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
