import processing.core.PGraphics;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;

import SketchUtils.*;

import static processing.core.PApplet.*;

public class Sketch_2020_05_12 extends Sketch{
    ArrayList<ArrayList<PVector>> stack;
    boolean grid[][];
    OpenSimplexNoise noiseClass;
    Sketch_2020_05_12(){
        descriptor = "Particle Perlin Flow Exploration";
        name = descriptor+" - "+this.getClass().getName();
        stack = new ArrayList<ArrayList<PVector>>();
    }

    @Override
    public void drawTask() {
        noiseClass = new OpenSimplexNoise(g.getSeed());
        g.loadPixelsEveryFrame = false;
        g.drawBufferPooling = 5;
        grid = new boolean[width+1][height+1];
        for(boolean[] rows:grid){
            for(boolean cols:rows){
                cols = false;
            }
        }
        for (int p = 0; p < p("particleCount");p++){
            addParticle();
        }

    }


    void addParticle() {
        if(main.killThread)return;
        ArrayList<PVector> stroke = new ArrayList<PVector>();
        Particle p = new Particle(new PVector(main.random(width),main.random(height)),new PVector());
        stroke.add(p.getPos().copy());
        p.setMaxForce(p("particleMaxForce"));
        p.setMaxSpeed(p("particleMaxSpeed"));
        p.setDrag(p("particleDrag"));
        g.newShape();
        int killPoints = 0;
        for(int t = 0; t < p("simSteps");t++){
            if(main.killThread)return;
            PVector force = new PVector(
                    sin((float)noiseClass.fit01(noiseClass.eval(p.getPos().x*(p("headingFieldXScale")*0.0001f),p.getPos().y*(p("headingFieldYScale")*0.0001f)))*TWO_PI),
                    cos((float)noiseClass.fit01(noiseClass.eval(p.getPos().x*(p("headingFieldXScale")*0.0001f),p.getPos().y*(p("headingFieldYScale")*0.0001f)))*TWO_PI));
            //p.getPos().sub(new PVector(halfWidth,halfHeight)).setMag(1)
            force = PVector.lerp(force, center.copy().sub(p.getPos()).normalize(),min((p.getPos().dist(new PVector(halfWidth,halfHeight))/p("GravityRadius")),1));
            force.mult((float)main.noise(p.getPos().x*(p("speedFieldXScale")*0.0001f),p.getPos().y*(p("speedFieldYScale")*0.0001f),1));
            //let's do some mad shit and sample for pixels in a random direction
            /*
            if(t%(int)p("SampleRate")==0){
                int row = floor(p.getPos().y);
                int col = floor(p.getPos().x);
                if(grid[min(max(0,col),width)][min(max(0,row),height)])killPoints++;
                if(killPoints>3)return;
                for(int sampleRow = max(0,row-(int)p("SampleRadius")/2); sampleRow < min(height,row+(int)p("SampleRadius")/2) ; sampleRow++){
                    for(int sampleCol = max(0,col-(int)p("SampleRadius")/2); sampleCol < min(width,col+(int)p("SampleRadius")/2) ; sampleCol++) {
                        if(grid[sampleCol][sampleRow]){
                            PVector hitPixel = new PVector(sampleCol,sampleRow);
                            PVector repulsionForce = PVector.sub(hitPixel,p.getPos());
                            repulsionForce.mult(p("Repulsion")/1000);
                            force.add(repulsionForce);
                        }
                    }
                }
            }
            */

            p.applyForce(force);
            p.update();
            stroke.add(p.getPos().copy());
            if(t%(int)p("drawEveryNSteps")==0)g.addPoint(p.getPos());
        }
        /*
        for(PVector s : stroke){
            grid[min(max(0,floor(s.x)),width)][min(max(0,floor(s.y)),height)]=true;
        }*/
    }

    public void registerParameters() {
        main.ui.registerParameter("particleCount", 1000, 0, 20000, 5);
        main.ui.registerParameter("simSteps", 300, 0, 2000, 5);
        main.ui.registerParameter("drawEveryNSteps", 1, 1, 30, 5);
        main.ui.registerParameter("headingFieldYScale", 10, 0, 200, 5);
        main.ui.registerParameter("headingFieldXScale", 10, 0, 200, 5);
        main.ui.registerParameter("speedFieldXScale", 10, 0, 200, 5);
        main.ui.registerParameter("speedFieldYScale", 10, 0, 200, 5);
        main.ui.registerParameter("particleMaxForce", 0.1f, 0.001f, 1, 5);
        main.ui.registerParameter("particleMaxSpeed", 4, 2, 20, 5);
        main.ui.registerParameter("particleDrag", 0.5f, 0, 1, 5);
        main.ui.registerParameter("GravityRadius", 500, 0, 2000, 5);
        //main.ui.registerParameter("SampleRadius", 20, 1, 20, 5);
        //main.ui.registerParameter("SampleRate", 3, 1, 20, 5);
        //main.ui.registerParameter("Repulsion", 1, 0, 1, 5);
        main.ui.styleConfigShow(new Color(142, 142, 142), new Color(43, 43, 43), 0.2);
    }

}
