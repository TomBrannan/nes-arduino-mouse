
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

/**
 * Uses a Robot to interpret serial events (via an Arduino) as mouse events
 * @author Tom
 */
public class NesMouse {

    public static void main(String[] args) throws AWTException {

        final Robot robot = new Robot();

        NesPad pad = new NesPad() {
            @Override
            public void APressed() {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            }

            @Override
            public void StartPressed() {
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            }

            @Override
            public void AReleased() {
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }

            @Override
            public void StartReleased() {
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            }
        };

        pad.initialize();

        int speed = 1;
        int state = pad.getButtons();
        char prev = 0;

        //Continuously poll the mouse and interpret input as mouse events
        //Loop can be exited at any time by triggering the pad.quit() combination
        while (!pad.quit()) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            int x = p.x;
            int y = p.y;

            if (pad.isBPressed()) {
                speed = 2;
            } else {
                speed = 1;
            }
            if (pad.isSelectPressed()) {
                if (pad.isDownPressed()) {
                    robot.mouseWheel(1);
                }
                if (pad.isUpPressed()) {
                    robot.mouseWheel(-1);
                }
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ie) {
                }
                continue;
            }
            if (pad.isDownPressed()) {
                robot.mouseMove(x, y + speed);
                p = MouseInfo.getPointerInfo().getLocation();
                x = p.x;
                y = p.y;
            }
            if (pad.isUpPressed()) {
                robot.mouseMove(x, y - speed);
                p = MouseInfo.getPointerInfo().getLocation();
                x = p.x;
                y = p.y;
            }
            if (pad.isRightPressed()) {
                robot.mouseMove(x + speed, y);
                p = MouseInfo.getPointerInfo().getLocation();
                x = p.x;
                y = p.y;
            }
            if (pad.isLeftPressed()) {
                robot.mouseMove(x - speed, y);
                p = MouseInfo.getPointerInfo().getLocation();
                x = p.x;
                y = p.y;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
                System.out.println(ie.getMessage());
            }
        }
        pad.close();

    }
}
