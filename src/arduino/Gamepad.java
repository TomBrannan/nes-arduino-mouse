
package arduino;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Enumeration;

/**
 * Gamepad: Abstract class to handle any number of digital buttons
 * (Currently unused)
 * @author Tom
 */
public abstract class Gamepad implements SerialPortEventListener
{
    private final int numberOfButtons;
    private BitSet state, current;
    public Gamepad(int numberOfButtons) {
        this.numberOfButtons = numberOfButtons;
        state = new BitSet(numberOfButtons);
        current = new BitSet(numberOfButtons);
    }
    
    public abstract void buttonPressed(int index);
    public abstract void buttonReleased(int index);
    
    
    
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
    
    public boolean initialize()
    {
        try 
        {
            CommPortIdentifier portId = null;
            Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
            // Enumerate system ports and try connecting to Arduino over each
            System.out.println( "Trying:");
            while (portId == null && portEnum.hasMoreElements()) {
                // Iterate through your host computer's serial port IDs
                CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                System.out.println( "   port" + currPortId.getName() );
                for (String portName : PORT_NAMES) {
                    if ( currPortId.getName().equals(portName) 
                      || currPortId.getName().startsWith(portName)) {
                        // Try to connect to the Arduino on this port
                        serialPort = (SerialPort)currPortId.open(appName, TIME_OUT);
                        portId = currPortId;
                        System.out.println( "Connected on port" + currPortId.getName() );
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
            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            
            return true;
        }
        catch ( Exception e ) {}
        return false;
    }
    
    public synchronized void close() {
        if ( serialPort != null ) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }
    @Override
    public void serialEvent(SerialPortEvent event) {
        try 
        {
            switch (event.getEventType() ) {
                case SerialPortEvent.DATA_AVAILABLE: 
                    input = serialPort.getInputStream();
                    int numBytes = (numberOfButtons-1) / 8 + 1;
                    byte[] bytes = new byte[numBytes];
                    for(int i = 0; i < numBytes; i++)
                    {
                        bytes[i] = (byte)input.read();
                        for(int j = 0; j < 8; j++)
                        {
                            boolean set = (bytes[i] & (1 << j)) == 1;
                            current.set(8*i + j, set);
                        }
                    }
                    BitSet temp = (BitSet)state.clone();
                    state.xor(current);
                    for(int i = 0; i < numberOfButtons; i++)
                    {
                        if(state.get(i))
                        {
                            if(temp.get(i))
                            {
                                buttonReleased(i);
                            }
                            else
                            {
                                buttonPressed(i);
                            }
                        }
                    }
                    state = (BitSet)current.clone();
                    break;
                default:
                    break;
            }
        } 
        catch (Exception e) {}
    }
    
}
