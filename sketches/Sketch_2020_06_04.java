import SketchUtils.OpenSimplexNoise;
import processing.core.PVector;

import java.awt.*;

import static processing.core.PApplet.*;

public class Sketch_2020_06_04 extends Sketch {
    OpenSimplexNoise noiseClass;

    Sketch_2020_06_04() {
        descriptor = "Ripped Cloth";
        name = descriptor + " - " + this.getClass().getName();
    }

    @Override
    public void drawTask() {
        g.loadPixelsEveryFrame = false;
        g.drawBufferPooling = 5;
        main.noiseSeed(g.getSeed());
        main.noiseDetail(floor(p("noiseLod")));
        rippedCloth();
    }

    private void rippedCloth() {

        for (int y = floor(p("margin")); y < height - floor(p("margin")); y += p("density")) {
            g.newShape();
            PVector lastPoint = new PVector(0, 0);
            for (int x = floor(p("margin")); x < width - floor(p("margin")); x += p("density")) {
                float no = main.noise(x * p("noiseDensityX") * 0.001f, y * p("noiseDensityY") * 0.001f);
                float n = main.map(no, 0, 1, -1, 1);
                float n1 = map(main.noise(y * p("noiseDensityY") * 0.001f, x * p("noiseDensityX") * 0.001f), 0, 1, -1, 1);
                float yNoised = sin(radians(x * n)) * tan(radians(x * n1)) * p("noiseIntensity");
                if (lastPoint.mag() == 0 || PVector.dist(lastPoint, new PVector(x, yNoised + y)) < p("cutoff")) {
                    if (lastPoint.mag() == 0) {
                        lastPoint = new PVector(x, yNoised + y);
                    } else {
                        lastPoint = new PVector(x, yNoised + y - (PVector.dist(lastPoint, new PVector(x, yNoised + y)) * p("looseThreadGravity")));
                    }
                    g.addPoint(lastPoint.copy());
                    lastPoint = new PVector(x, yNoised + y);
                } else {
                    g.newShape();
                    lastPoint = new PVector(0, 0);
                }

            }
        }
        for (int x = floor(p("margin")); x < height - floor(p("margin")); x += p("density")) {
            g.newShape();
            PVector lastPoint = new PVector(0, 0);
            for (int y = floor(p("margin")); y < width - floor(p("margin")); y += p("density")) {
                float no = main.noise(x * p("noiseDensityX") * 0.001f, y * p("noiseDensityY") * 0.001f);
                float n = main.map(no, 0, 1, -1, 1);
                float n1 = map(main.noise(y * p("noiseDensityY") * 0.001f, x * p("noiseDensityX") * 0.001f), 0, 1, -1, 1);
                float yNoised = sin(radians(x * n)) * tan(radians(x * n1)) * p("noiseIntensity");
                if (lastPoint.mag() == 0 || PVector.dist(lastPoint, new PVector(x, yNoised + y)) < p("cutoff")) {
                    if (lastPoint.mag() == 0) {
                        lastPoint = new PVector(x, yNoised + y);
                    } else {
                        lastPoint = new PVector(x, yNoised + y - (PVector.dist(lastPoint, new PVector(x, yNoised + y)) * p("looseThreadGravity")));
                    }

                    g.addPoint(lastPoint.copy());
                    lastPoint = new PVector(x, yNoised + y);
                } else {
                    g.newShape();
                    lastPoint = new PVector(0, 0);
                }
            }
        }
    }

    public void registerParameters() {
        main.ui.registerParameter("density", 3, 1, 100, 5);
        main.ui.registerParameter("margin", 100, 1, 1000, 5);
        main.ui.registerParameter("noiseDensityX", 0.8f, 0, 50, 1);
        main.ui.registerParameter("noiseDensityY", 3, 0, 50, 1);
        main.ui.registerParameter("noiseIntensity", 20, 0, 100, 1.1f);
        main.ui.registerParameter("noisePower", 0.5f, 0.01f, 10, 5);
        main.ui.registerParameter("cutoff", 5, 0, 100, 5);
        main.ui.registerParameter("looseThreadGravity", 0.1f, 0, 10, 5);
        main.ui.registerParameter("noiseLod", 15, 1, 100, 5);
        main.ui.styleConfigShow(new Color(72, 43, 154), new Color(255, 255, 255), 0.5);
    }
}
