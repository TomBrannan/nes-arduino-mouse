# NES-Arduino-Mouse
A fun little Arduino project which uses an NES controller as a computer mouse.  This particular implementation uses an Arduino UNO and is configured for Windows, but should be easy to get working with other hardware.

![NES mouse hooked up to an Arduino UNO](http://www.imgur.com/MSo4UDB.jpg)

# Background
The NES controller uses a simple 8-bit shift register to store and transmit the state of its 8 buttons.  This means that the data for all 8 buttons
is sent over a single pin.  The Arduino can detect the
voltages sent over the NES controller's data pin, and with a small amount of Arduino code, we can make the Arduino send the byte (8 bits, one for each button state)
to the computer over its USB serial connection.  

I decided to make the D-Pad move the cursor, B to speed up, A to left click, Start to right click, and Select to trigger scrolling.  However,
the same logic could be used to trigger other events (such as key events for use with an emulator, not that you'd want to do that)

Here's an example video:  https://www.youtube.com/watch?v=ji81rnHlw88
