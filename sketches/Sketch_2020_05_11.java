import processing.core.PVector;

import java.awt.*;

import static processing.core.PApplet.*;

public class Sketch_2020_05_11 extends Sketch{
    Sketch_2020_05_11(){
        descriptor = "Mirrored Noise Shapes";
        name = descriptor+" - "+this.getClass().getName();
    }

    @Override
    public void drawTask() {
        super.drawTask();
        main.noiseSeed(floor(g.getSeed()));
        float y = p("EdgeMargin");
        while (y < height-p("EdgeMargin")) {
            y+=p("RowSpacing");
            drawWave(y);
        }
    }

    void drawWave(float y) {
        float x = p("EdgeMargin");
        float spacing = ((float)width-p("EdgeMargin")*2)/p("LineInterpolation");
        g.newShape();
        while ( x < width ) {
            float intensity = min(1,abs(((y-p("EdgeMargin"))/(height-p("EdgeMargin")*2)-0.5f)*2));
            intensity = 1-intensity;
            float sinch = pow(max(intensity,p("sinchMinimum")),p("sinchPower"));
            float mirrorX = abs(x - width/2f);


            x+=spacing;
            float noiseValx = decel((main.noise(y/p("noiseDensityY"))-0.5f))*p("XOffset");
            float noiseValy = decel((main.noise(y/p("noiseDensityY"), mirrorX/p("noiseDensityX"))-0.5f))*(p("noiseRamp"));
            float xpos = lerp(width/2f, x+noiseValx, sinch);
            PVector pos = new PVector(xpos, y+noiseValy);
            if(pos.x>halfWidth){
                g.addPoint(halfWidth, pos.y);
                break;
            }
            g.addPoint(pos.x, pos.y);
        }
        g.newShape();

        x = p("EdgeMargin");
        while ( x < width ) {
            float intensity = min(1,abs(((y-p("EdgeMargin"))/(height-p("EdgeMargin")*2)-0.5f)*2));
            intensity = 1-intensity;
            float sinch = pow(max(intensity,p("sinchMinimum")),p("sinchPower"));
            float mirrorX = abs(x - halfWidth);


            x+=spacing;
            float noiseValx = decel((main.noise(y/p("noiseDensityY"))-0.5f))*p("XOffset");
            float noiseValy = decel((main.noise(y/p("noiseDensityY"), mirrorX/p("noiseDensityX"))-0.5f))*(p("noiseRamp"));

            float xpos = lerp(halfWidth, x+noiseValx, sinch);
            PVector pos = new PVector(xpos, y+noiseValy);

            if(pos.x>halfWidth){
                g.addPoint(halfWidth, pos.y);
                break;
            }

            g.addPoint(width-pos.x, pos.y);
        }
    }

    public void registerParameters() {
        main.ui.registerParameter("RowSpacing", 2, 0, 20, 5);
        main.ui.registerParameter("LineInterpolation", 55, 1, 500, 5);
        main.ui.registerParameter("EdgeMargin", 200, 1, 600, 5);
        main.ui.registerParameter("noiseDensityX", 20, 0, 2000, 1);
        main.ui.registerParameter("noiseDensityY", 1500, 0, 2000, 1);
        main.ui.registerParameter("XOffset", 400, 0, 500, 1.1f);
        main.ui.registerParameter("noisePower", 2, 0, 50, 1.1f);
        main.ui.registerParameter("noiseRamp", 400, 0, 500, 1.1f);
        main.ui.registerParameter("cinchPower", 10, 0, 10, 1.1f);
        main.ui.registerParameter("cinchMinimum", 0, 0, 1, 1.1f);
        main.ui.styleConfigShow(new Color(210, 210, 210), new Color(0, 0, 0), 0.2);
    }

}
