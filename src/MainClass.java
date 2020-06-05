import org.reflections.Reflections;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Set;

public class MainClass extends PApplet {
    public static MainClass processing;
    public UIWindow ui = new UIWindow();
    public Style style = new Style();
    public Sketch sketch;
    public boolean flush = false;
    public boolean killThread = false;
    public boolean drawThreadRunning = false;
    public boolean screenshotMode = false;
    public boolean emptyScreen = false;
    public boolean video_record = false;
    public boolean sorting = false;
    public boolean waitingForPrevThread = false;
    public GCode gcode;
    Set<Class<? extends Sketch>> sketchesReflector;
    ArrayList<Sketch> sketches;


    int framesToRecord = 30 * 5;
    int frameRecord = 0;
    int frameNum = 0;

    public static void main(String[] args) {
        PApplet.main("MainClass", args);
    }

    public void settings() {
        size(1200, 1200);
        processing = this;
        gcode = new GCode(210, 210);
        sketches = new ArrayList<Sketch>();
        Reflections reflections = new Reflections("");
        sketchesReflector = reflections.getSubTypesOf(Sketch.class);
        for (Class<? extends Sketch> s : sketchesReflector) {
            Sketch sk = null;
            try {
                sk = s.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                e.printStackTrace();
            }
            sketches.add(sk);
        }
        final String[] switches = {"--sketch-path=" + sketchPath(), ""};
        runSketch(switches, ui);
    }

    public void draw() {
        if (emptyScreen) {
            background(style.background.getRGB());
            emptyScreen = false;
        }
        if (flush) {
            background(style.background.getRGB());
            stroke(style.foreground.getRGB());
            strokeWeight((float) style.strokeweight);
            gcode.optimizedDrawShape();
            flush = false;
        }
        if (sorting) {
            image(gcode.getWorkBuffer().get(), 0, 0);
        } else {
            if (video_record) {
                if (frameRecord == framesToRecord) {
                    video_record = false;
                    gcode.optimizedDrawShape();
                    image(gcode.getWorkBuffer().get(), 0, 0);
                }
                if (frameRecord == 0) {
                    background(style.background.getRGB());
                    stroke(50);
                    drawGraphPaper(1.5, 40);
                    stroke(style.foreground.getRGB());
                }
                strokeWeight((float) style.strokeweight);
                gcode.drawSection(frameRecord, framesToRecord);
                String frameName = "exportedGCODE/" + gcode.filename + "/" + frameRecord + ".png";
                saveFrame(frameName);
                frameRecord++;
            } else {
                //CONSTRUCTION DRAWTHREAD
                if (drawThreadRunning) {
                    frameNum++;
                    image(gcode.getWorkBuffer().get(), 0, 0);
                }
            }
        }

    }

    void drawGraphPaper(double dotRadius, int divisions) {
        for (int x = 0; x < divisions; x++) {
            for (int y = 0; y < divisions; y++) {
                strokeWeight((float) dotRadius);
                point((float) width / divisions * x, (float) height / divisions * y);
            }
        }
    }

    public void loadSketch() {
        if(drawThreadRunning){
            killThread = true;
            delay(200);
        }
        ui.resetUI();
        sketch.registerParameters();
        thread("drawSketch");
    }
    public void sort(){
        thread("sortStrokes");
    }
    public void sortStrokes(){
        sorting = true;
        gcode.sortStack();
        delay(2000);
        sorting = false;
        gcode.optimizedDrawShape();
    }

    public void sketchThread(){
        sketch.threadedTask();
    }
    public void drawSketch() {

        if(drawThreadRunning){
            killThread = true;
        }
        if(waitingForPrevThread){
            //cancel this boy, we are waiting in a loop.
            killThread = false;
            print("\n\nWE GOT STUCK, A SHOOK IT UP\n\n");
            return;
        }
        while(killThread){
            waitingForPrevThread = true;
            delay(10);
        }
        waitingForPrevThread = false;

        gcode.clearBuffers();
        drawThreadRunning = true;
        emptyScreen = true;
        sketch.drawTask();
        if(!killThread) {
            gcode.drawLastSection();
            delay(200);
        } else {
            killThread = false;
        }
        drawThreadRunning = false;
    }

}
