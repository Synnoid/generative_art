import com.google.common.base.Charsets;
import com.google.gson.Gson;
import controlP5.*;
import processing.core.PApplet;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class UIWindow extends PApplet {
    private final ArrayList<String> exposedVarNames = new ArrayList<String>();
    private final ArrayList<Float> exposedVarValues = new ArrayList<Float>();
    ControlP5 cp5;
    private MainClass main;

    private ColorWheel cpForeground;
    private ColorWheel cpBackground;
    private CheckBox toggles;
    private Slider slStrokeWeight;
    private DropdownList dlSketches;
    private ArrayList<Controller> Parameters;
    private int exposedVarIndex = 0;
    private double exposedVarY = 100.0;
    private int refreshTimer = 0;
    private boolean doRefresh = false;

    class SettingsManager {
        private Path path;
        private Gson gson = new Gson();
        private FileWriter fileWriter;

        SettingsManager() throws IOException {
            String workPath =System.getProperty("user.dir");
            assert workPath != "";
            path = Paths.get(workPath+"\\settings.json");
            if(Files.exists(path)){
                String settingsJson = new String(Files.readAllBytes(path));
                settings = gson.fromJson(settingsJson,Settings.class);
                assert settings != null;
                print("\n---------------------\nSuccessfully loaded settings.json!\n---------------------\n");
            } else {
                settings = new Settings();
                fileWriter = new FileWriter(path.toFile(), false);
                fileWriter.write(gson.toJson(settings));
                fileWriter.close();
            }
        }

        public void saveSettings() throws IOException {
            fileWriter = new FileWriter(path.toFile(), false);
            fileWriter.write(gson.toJson(settings));
            fileWriter.close();
        }
    }

    class Settings {
        public String getActiveSketch() {
            return activeSketch;
        }

        public void setActiveSketch(String activeSketch) {
            this.activeSketch = activeSketch;
        }

        private String activeSketch = "";
    }

    private SettingsManager settingsManager;
    private Settings settings;
    private Path settingsPath;

    ControlListener clickedButton = theEvent -> {
        switch (theEvent.getName()) {
            case "EXPORT_GCODE":
                main.gcode.saveGCode();
                break;
            case "CEN":
                main.gcode.center();
                main.delay(1000);
                main.flush = true;
                break;
            case "SORT_STROKES":
                main.gcode.setSorting(true);
                main.thread("sort");
                break;
            case "RESET_SCENE":
                if(main.drawThreadRunning){
                    main.killThread = true;
                    delay(200);
                }
                main.thread("drawSketch");
                break;
            case "CONSTRUCTION_VID":
                main.video_record = true;
                main.frameRecord = 0;
                break;

            case "SEED LOCK":
                main.gcode.lockseed = toggles.getState("SEED LOCK");
        }
    };

    ControlListener changeParameter = theEvent -> {
        if (theEvent.isFrom(dlSketches)) {
            main.sketch = main.sketches.get((int) theEvent.getValue());
            main.loadSketch();
            settings.activeSketch = main.sketch.name;
            try {
                settingsManager.saveSettings();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String name = theEvent.getController().getName();
        setParameterValue(name, theEvent.getValue());

        if (name.equals("StrokeWeight")) {
            print("[Setting stroke weight: " + theEvent.getValue() / 10 + "]\n");
            main.style.strokeweight = theEvent.getValue() / 10;
        }
        if (name.equals("Strokes")) {
            main.style.foreground = new Color(cpForeground.getRGB());
            print("[Setting stroke colour: " + cpForeground.getRGB() + "]\n");
        }
        if (name.equals("Background")) {
            main.style.background = new Color(cpBackground.getRGB());
            print("[Setting background colour: " + cpBackground.getRGB() + "]\n");
        }
        delayedRefresh();
    };

    public void settings() {
        size(300, 1200);
    }

    public void setup() {
        main = MainClass.processing;
        smooth();
        cp5 = new ControlP5(this);
        Parameters = new ArrayList<>();

        try {
            settingsManager = new SettingsManager();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer();
        int begin = 0;
        int timeInterval = 100;

        //Sketch drawer with timeout to prevent spamming threads
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(doRefresh && millis()>refreshTimer){
                    main.thread("drawSketch");
                    doRefresh = false;
                }
            }
        }, begin, timeInterval);

            //Path path = Paths.get(MainClass.class.getProtectionDomain().getCodeSource().getLocation()+"settings.json");





        //Buttons
        cp5.addButton("RESET_SCENE")
                .setPosition(0, 20)
                .setSize(300, 20)
                .setColorBackground(0xff29b696)
                .setColorForeground(0xff333333)
                .addListener(clickedButton)
                .setColorActive(0xff2ae496);

        cp5.addButton("EXPORT_GCODE")
                .setPosition(0, 40)
                .setSize(85, 19)
                .setColorBackground(0xffd87a96)
                .setColorForeground(0xff333333)
                .addListener(clickedButton)
                .setColorActive(0xff555555);

        cp5.addButton("SORT_STROKES")
                .setPosition(85, 40)
                .setSize(85, 19)
                .setColorBackground(0xff276196)
                .setColorForeground(0xff333333)
                .addListener(clickedButton)
                .setColorActive(0xff555555);

        cp5.addButton("CONSTRUCTION_VID")
                .setPosition(170, 40)
                .setSize(100, 19)
                .setColorBackground(0xff797a8d)
                .setColorForeground(0xff333333)
                .addListener(clickedButton)
                .setColorActive(0xff555555);

        cp5.addButton("CEN")
                .setPosition(270, 40)
                .setSize(30, 19)
                .setColorBackground(0xffbfa787)
                .setColorForeground(0xff333333)
                .addListener(clickedButton)
                .setColorActive(0xff555555);
        toggles = cp5.addCheckBox("toggles")
                .setPosition(0,60)
                .setSize(19,19)
                .addItem("SEED LOCK",0)
                .addListener(clickedButton)
                .showLabels();
        toggles.getItem(0).addListener(clickedButton);

        dlSketches = cp5.addDropdownList("SKETCHES")
                .setPosition(0, 0);
        dlSketches.setBackgroundColor(color(190));
        dlSketches.setItemHeight(20);
        dlSketches.setBarHeight(20);
        dlSketches.setWidth(300);
        dlSketches.setOpen(false);
        for (Sketch s : main.sketches) {
            dlSketches.addItem(s.name, dlSketches.getItems().size());
            dlSketches.setValue(dlSketches.getItems().size());
            if(settings.getActiveSketch().equals(s.name)){
                main.sketch = s;
                print("Setting active sketch to:["+s.name+"] from previous session!\n");
            }
        }
        if(main.sketch == null) main.sketch = main.sketches.get(0);
        dlSketches.addListener(changeParameter);
        main.loadSketch();
    }

    private void delayedRefresh(){
        refreshTimer = millis()+500;
        doRefresh = true;
    }

    public void resetUI() {
        exposedVarY = 130.0;
        for (Controller c : Parameters) {
            c.remove();
        }
        Parameters.clear();
        exposedVarNames.clear();
        exposedVarValues.clear();
        exposedVarIndex = 0;
        slStrokeWeight = null;
        cpForeground = null;
        cpBackground = null;
    }

    public void styleConfigShow(Color foreground, Color background, double strokeWeight) {
        exposedVarY += 28;
        if (slStrokeWeight == null) {
            slStrokeWeight = cp5.addSlider("StrokeWeight")
                    .setPosition(20, (float) exposedVarY)
                    .setSize(150, 14)
                    .setId(exposedVarIndex)
                    .setScrollSensitivity(0.001f)
                    .setRange(0.01f, 20)
                    .setColorBackground(0xff111111)
                    .setColorForeground(0xff333333)
                    .setColorActive(0xff555555)
                    .setValue((float) strokeWeight * 10);
            slStrokeWeight.getCaptionLabel().enableColorBackground().setColorBackground(0xff000000);
            main.style.strokeweight = strokeWeight;
            exposedVarY += 28;
            slStrokeWeight.addListener(changeParameter);
            Parameters.add(slStrokeWeight);
        }
        if (cpForeground == null) {
            cpForeground = cp5.addColorWheel("Strokes").setPosition(20, (float) exposedVarY).setRGB(foreground.getRGB());
            //cpForeground.getCaptionLabel().enableColorBackground().setColorBackground(0xff000000);
            exposedVarY += 220;
            main.style.foreground = foreground;
            cpForeground.addListener(changeParameter);
            Parameters.add(cpForeground);
        }

        if (cpBackground == null) {
            cpBackground = cp5.addColorWheel("Background").setPosition(20, (float) exposedVarY).setRGB(background.getRGB());
            //cpBackground.getCaptionLabel().enableColorBackground().setColorBackground(0xff000000);
            exposedVarY += 220;
            main.style.background = background;
            cpBackground.addListener(changeParameter);
            Parameters.add(cpBackground);
        }
    }

    public void registerParameter(String name, float defaultValue, float min, float max, float precission) {
        if (Parameters == null) Parameters = new ArrayList<>();
        if (!exposedVarNames.contains(name)) {
            exposedVarNames.add(name);
            exposedVarValues.add(defaultValue);
            Slider c = cp5.addSlider(name)
                    .setPosition(20, (float) exposedVarY)
                    .setSize(200, 14)
                    .setId(exposedVarIndex)
                    .setScrollSensitivity(0.001f)
                    .setRange(min, max)
                    .setColorBackground(0xff111111)
                    .setColorForeground(0xff333333)
                    .setColorActive(0xff555555)
                    .setValue(defaultValue);
            c.getCaptionLabel().enableColorBackground().setColorBackground(0xff000000);
            c.addListener(changeParameter);
            Parameters.add(c);
            exposedVarY += 28;
            exposedVarIndex++;
        }
    }

    float getParameterValue(String name) {
        if (exposedVarNames.contains(name)) {
            return exposedVarValues.get(exposedVarNames.indexOf(name));
        }
        return 0;
    }

    void setParameterValue(String name, float value) {
        if (exposedVarNames.contains(name)) {
            exposedVarValues.set(exposedVarNames.indexOf(name), value);
        }
    }

    public void draw() {
        background(30);
        String infoText = "Total Segments: " + main.gcode.numStrokes() + " \nTotal Draw Distance: " + main.gcode.getTravelBuffer().x / width * 210 / 10000 + " meters\nTotal Travel Distance: "+ main.gcode.getTravelBuffer().y / width * 210 / 10000 + " meters\nTotal Draw Time: " + (round((main.gcode.getTravelBuffer().x+main.gcode.getTravelBuffer().y) / width * 210 / 12000)) + " minutes";
        text(infoText, 20, height-60);
    }
}
