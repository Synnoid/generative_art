import SketchUtils.OpenSimplexNoise;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;

import static processing.core.PApplet.*;

public class Sketch_2020_05_30 extends Sketch{
    OpenSimplexNoise noiseClass;
    Sketch_2020_05_30(){
        descriptor = "Moire spirals";
        name = descriptor+" - "+this.getClass().getName();
    }

    @Override
    public void drawTask() {
        g.loadPixelsEveryFrame = false;
        noiseClass = new OpenSimplexNoise(g.getSeed());
        g.drawBufferPooling = 5;
        waveSpiral(new PVector(width/2.0f,height/2.0f),500,new PVector(200,200,200),0);
        waveSpiral(new PVector(width/2.0f,height/2.0f),500,new PVector(200,200,200),p("layerSpacing")/100);
        waveSpiral(new PVector(width/2.0f,height/2.0f),500,new PVector(200,200,200),-p("layerSpacing")/100);
    }

    private void waveSpiral(PVector centerPoint, float rmax, PVector col, float noiseOffset) {
        float r = 1;
        float tetha = 0;
        float dtheta = (float)(2 * Math.PI / p("shapeDensity"));
        float dr = (float)p("ringSpacing")/p("shapeDensity");
        PVector prevP = new PVector();
        int itr = 0;
        float l = 0;
        g.newShape();
        while(r < rmax){
            itr++;
            if(itr > 100000)return;
            float x = centerPoint.x + r*cos(tetha);
            float y = centerPoint.y - r*sin(tetha);
            double noiseValx =(float)(noiseClass.fit01(noiseClass.eval(x * (p("noiseDensityX") * 0.001f), y * (p("noiseDensityX") * 0.001f), noiseOffset))-0.5)*2;
            double noiseValy =(float)(noiseClass.fit01(noiseClass.eval(y * (p("noiseDensityX") * 0.001f), x * (p("noiseDensityX") * 0.001f), noiseOffset))-0.5)*2;


            float xW = x+(float)noiseValx*p("noiseIntensity")*r/rmax;
            float yW = y+(float)noiseValy*p("noiseIntensity")*r/rmax;

            g.addPoint(xW, yW);
            tetha += dtheta;
            r+=dr;
        }
    }

    public void registerParameters() {
        main.ui.registerParameter("shapeDensity",300,5,1000,5);
        main.ui.registerParameter("ringSpacing",3,0.1f,20,5);
        main.ui.registerParameter("noiseDensityX",2,0,50,1);
        main.ui.registerParameter("noiseDensityY",2,0,50,1);
        main.ui.registerParameter("noiseIntensity",20,0,100,1.1f);
        main.ui.registerParameter("layerSpacing",10,0,100,1.1f);
        main.ui.styleConfigShow(new Color(72, 43, 154), new Color(255, 255, 255), 0.5);
    }
}
