import SketchUtils.OpenSimplexNoise;
import SketchUtils.Particle;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;

import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.TWO_PI;

public class Sketch_2020_05_16 extends Sketch {
    ArrayList<ArrayList<PVector>> stack;
    boolean grid[][];
    OpenSimplexNoise noiseClass;

    Sketch_2020_05_16() {
        descriptor = "Circular Particle Perlin Flow Exploration";
        name = descriptor + " - " + this.getClass().getName();
        stack = new ArrayList<ArrayList<PVector>>();
    }

    @Override
    public void drawTask() {
        noiseClass = new OpenSimplexNoise(g.getSeed());
        g.loadPixelsEveryFrame = false;
        g.drawBufferPooling = 5;
        grid = new boolean[width + 1][height + 1];
        for (boolean[] rows : grid) {
            for (boolean cols : rows) {
                cols = false;
            }
        }
        for (int p = 0; p < p("particleCount"); p++) {
            addParticle();
        }

    }


    void addParticle() {
        if (main.killThread) return;
        ArrayList<PVector> stroke = new ArrayList<PVector>();
        float randomTetha = main.random(0, TWO_PI);
        float radiusRandom = main.random(-p("startNoise"), p("startNoise"));
        float px = (p("gravityRadius") + radiusRandom) * sin(randomTetha) + halfWidth;
        float py = (p("gravityRadius") + radiusRandom) * cos(randomTetha) + halfHeight;
        PVector pos = new PVector(px, py);


        Particle p = new Particle(new PVector(px, py), new PVector(0, 0));
        stroke.add(p.getPos().copy());
        p.setMaxForce(p("particleMaxForce"));
        p.setMaxSpeed(p("particleMaxSpeed"));
        p.setDrag(p("particleDrag"));
        g.newShape();
        int killPoints = 0;
        for (int t = 0; t < p("simSteps"); t++) {
            if (main.killThread) return;
            PVector force = new PVector(
                    sin((float) noiseClass.fit01(noiseClass.eval(p.getPos().x * (p("headingFieldXScale") * 0.0001f), p.getPos().y * (p("headingFieldYScale") * 0.0001f))) * TWO_PI),
                    cos((float) noiseClass.fit01(noiseClass.eval(p.getPos().x * (p("headingFieldXScale") * 0.0001f), p.getPos().y * (p("headingFieldYScale") * 0.0001f))) * TWO_PI));
            force = PVector.lerp(force, center.copy().sub(p.getPos()).normalize(), (p.getPos().dist(new PVector(halfWidth, halfHeight)) / p("gravityRadiusInner")));

            p.applyForce(force);
            p.update();
            if (stroke.size() > 0) {
                if (stroke.get(stroke.size() - 1).dist(p.getPos()) > p("pointSpacing")) {
                    g.addPoint(p.getPos());
                    stroke.add(p.getPos().copy());
                }
            } else {
                g.addPoint(p.getPos());
                stroke.add(p.getPos().copy());
            }

        }
    }

    public void registerParameters() {
        main.ui.registerParameter("particleCount", 1000, 0, 20000, 5);

        main.ui.registerParameter("simSteps", 3000, 0, 5000, 5);

        //main.ui.registerParameter("drawEveryNSteps", 1, 1, 30, 5);

        main.ui.registerParameter("headingFieldYScale", 30, 0, 200, 5);
        main.ui.registerParameter("headingFieldXScale", 30, 0, 200, 5);

        main.ui.registerParameter("particleMaxForce", 0.35f, 0.001f, 1, 5);
        main.ui.registerParameter("particleMaxSpeed", 8, 2, 20, 5);
        main.ui.registerParameter("particleDrag", 0.5f, 0, 1, 5);

        main.ui.registerParameter("gravityRadius", 500, 0, 2000, 5);
        main.ui.registerParameter("gravityRadiusInner", 800, 0, 2000, 5);

        main.ui.registerParameter("startNoise", 10, 0, 100, 5);

        main.ui.registerParameter("pointSpacing", 1, 1, 100, 5);


        main.ui.styleConfigShow(new Color(0, 0, 0), new Color(255, 255, 255), 0.2);
    }

}
