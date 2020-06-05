import processing.core.PApplet;
import processing.core.PVector;

import java.awt.*;

public class Sketch {
    protected final MainClass main = MainClass.processing;
    public String name;
    protected String descriptor = "";
    protected int height;
    protected int width;
    protected float halfHeight;
    protected float halfWidth;
    PVector center;
    protected GCode g;

    Sketch() {
        name = descriptor + this.getClass().getName();
        height = main.height;
        width = main.width;
        halfHeight = height / 2f;
        halfWidth = width / 2f;
        g = main.gcode;
        center = new PVector(halfWidth,halfHeight);
    }

    protected float decel(float x) { // as an easing function
        return 1 - (x - 1) * (x - 1);
    }

    protected float p(String name) {
        return main.ui.getParameterValue(name);
    }

    public void registerParameters() {
        main.ui.styleConfigShow(new Color(210, 210, 210), new Color(0, 0, 0), 0.2);
    }

    public void drawTask() {
        PApplet.print("[Generating: " + name + "]\n");
    }

    public void threadedTask() {

    }
}
