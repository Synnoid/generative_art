import processing.core.PApplet;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;


public class Sketch_2020_05_03 extends Sketch {
    float wavePeriod = 50;
    ArrayList<PVector> CirclePosStack;
    ArrayList<Float> CircleRadiusStack;
    ArrayList<Bracket> Brackets;

    Sketch_2020_05_03() {
        descriptor = "Bracketed sin waves";
        name = descriptor + " - " + this.getClass().getName();
    }

    public void registerParameters() {
        main.ui.registerParameter("bracketSize", 100, 5, 1000, 5);
        main.ui.registerParameter("waveRows", 200, 1, 5000, 5);
        main.ui.registerParameter("waveAmplitude", 50, 0, 200, 5);
        main.ui.registerParameter("noiseDensityX", 10, 0, 50, 1);
        main.ui.registerParameter("noiseDensityY", 10, 0, 50, 1);
        main.ui.registerParameter("noisePower", 4, 0, 50, 1.1f);
        main.ui.registerParameter("noiseMadness", 1, 0, 50, 1.1f);
        main.ui.registerParameter("DrawGrid", 1, 0, 1, 1);
        main.ui.styleConfigShow(new Color(210, 210, 210), new Color(0, 0, 0), 0.2);
    }

    @Override
    public void drawTask() {
        super.drawTask();
        main.noiseSeed(g.getSeed());
        CirclePosStack = new ArrayList<PVector>();
        CircleRadiusStack = new ArrayList<Float>();
        CirclePosStack.add(new PVector(halfWidth, halfHeight));
        CircleRadiusStack.add(550f);
        Brackets = new ArrayList<Bracket>();


        for (int i = 0; i < width - 200; i += p("bracketSize")) {
            if (main.killThread) return;
            Bracket b = new Bracket();
            b.CenteredSquare(i, i + p("bracketSize"));
            if (p("DrawGrid") > 0) b.trace();
            waves(new PVector(150, 150, 150), 1 + (float) i / 500, b);
            Brackets.add(b);
        }
    }

    void waves(PVector col, float waveOffset, Bracket bracket) {
        for (float y = 0.0f; y < p("waveRows"); y++) {
            if (main.killThread) return;
            float l = 0;
            PVector lastPos = new PVector();
            g.newShape();
            for (float x = 0; x < width * wavePeriod; x++) {
                if (main.killThread) return;
                float xx = x / wavePeriod;

                PVector newPos = new PVector(xx, PApplet.map((y + 0.5f) * height / p("waveRows") + 500, -500, height + 500, -500, height + 500));

                float noiseVal = PApplet.pow(main.noise(newPos.x * p("noiseDensityX") / 10000, newPos.y * p("noiseDensityY") / 10000, waveOffset), p("noisePower"));

                l += noiseVal / wavePeriod; // period of the wave

                float m = (1 - noiseVal) * p("noiseMadness"); // separate it from an increasing variable (l)
                PVector pos = new PVector(xx, PApplet.map((y + 0.5f) * height / p("waveRows"), 0, height, 0, height) + PApplet.sin(l * main.PI / 2.0f) * p("waveAmplitude") * decel(m));


                if (!bracket.pointInside(new PVector(pos.x, pos.y))) {
                    g.newShape();
                } else {
                    g.addPoint(pos.x, pos.y);


                }
            }
        }
    }

    class Bracket {
        PVector minPos;
        PVector minSize;
        PVector maxPos;
        PVector maxSize;

        Bracket() {
            minPos = new PVector((float) main.width / 2, (float) main.height / 2);
            maxPos = new PVector((float) main.width / 2, (float) main.height / 2);
            minSize = new PVector(0, 0);
            maxSize = new PVector(0, 0);
        }

        Bracket(float minX, float minY, float minWidth, float minHeight, float maxX, float maxY, float maxWidth, float maxHeight) {
            minPos = new PVector(minX, minY);
            maxPos = new PVector(maxX, maxY);
            minSize = new PVector(minWidth, minHeight);
            maxSize = new PVector(maxWidth, maxHeight);
        }

        void CenteredSquare(float min, float max) {
            minPos = new PVector((float) main.width / 2, (float) main.height / 2);
            maxPos = new PVector((float) main.width / 2, (float) main.height / 2);
            minSize = new PVector(min, min);
            maxSize = new PVector(max, max);
        }

        boolean pointInside(PVector pos) {
            return (
                    pos.x < (maxPos.x + maxSize.x / 2) &&
                            pos.x > (maxPos.x - maxSize.x / 2) &&
                            pos.y < (maxPos.y + maxSize.y / 2) &&
                            pos.y > (maxPos.y - maxSize.y / 2) &&
                            !(
                                    pos.x < (minPos.x + minSize.x / 2) &&
                                            pos.x > (minPos.x - minSize.x / 2) &&
                                            pos.y < (minPos.y + minSize.y / 2) &&
                                            pos.y > (minPos.y - minSize.y / 2))
            );
        }

        void trace() {
            g.newShape();
            g.addPoint((maxPos.x + maxSize.x / 2), (maxPos.y + maxSize.y / 2));
            g.addPoint((maxPos.x - maxSize.x / 2), (maxPos.y + maxSize.y / 2));
            g.addPoint((maxPos.x - maxSize.x / 2), (maxPos.y - maxSize.y / 2));
            g.addPoint((maxPos.x + maxSize.x / 2), (maxPos.y - maxSize.y / 2));
            g.addPoint((maxPos.x + maxSize.x / 2), (maxPos.y + maxSize.y / 2));
        }
    }
}
