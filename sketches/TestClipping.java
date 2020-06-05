import processing.core.PVector;

import java.awt.*;
import SketchUtils.Particle;

import static processing.core.PApplet.*;

public class TestClipping extends Sketch{
    TestClipping(){
        descriptor = "";
        name = descriptor+" - "+this.getClass().getName();
    }

    @Override
    public void drawTask() {
       //g.newShape();
       //g.addPoint(halfWidth,halfHeight);
      // g.addPoint(-50,halfHeight);

       g.newShape();
        g.addPoint(width+5000f,halfHeight);
        g.addPoint(width+50f,halfHeight);
        g.addPoint(width,halfHeight);
       g.addPoint(halfWidth,halfHeight);


       //g.newShape();
       //g.addPoint(halfWidth,halfHeight);
       //g.addPoint(halfWidth,-50);

       //g.newShape();
       //g.addPoint(halfWidth,halfHeight);
       //g.addPoint(halfWidth,height+50);

       g.debugPrintStrokes();
    }



    public void registerParameters() {
        main.ui.styleConfigShow(new Color(210, 210, 210), new Color(0, 0, 0), 2);
    }

}
