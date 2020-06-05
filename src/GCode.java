import de.ixdhof.hershey.HersheyFont;
import org.jetbrains.annotations.NotNull;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;

import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

import static processing.core.PApplet.*;
import static processing.core.PConstants.REPLACE;

class StrokePoint {
    PVector pos = new PVector(0, 0);
    Color col = new Color(0);
    double weight = 0.2;
    int sortedPos = 0;
    boolean hasBeenSorted = false;

    StrokePoint(PVector p, Style style) {
        pos = p;
        col = style.foreground;
        weight = style.strokeweight;
    }
}

class GCode {
    //Constants
    static final String CONFIG_GCODE_PEN_UP = "M3 S100;PEN UP!\nG4P0.1";
    static final String CONFIG_GCODE_PEN_DOWN = "M3 S0;PEN DOWN!";
    static final String CONFIG_GCODE_MOVE_FEEDRATE = "2000";
    static final String CONFIG_GCODE_PRE = "G21 ; All units in mm";
    static final String CONFIG_GCODE_POST = "G0 F" + CONFIG_GCODE_MOVE_FEEDRATE + " X0 Y0";
    static final PVector PRINTER_MAX_SIZE = new PVector(280, 210);
    static final double PRECISSION = 0.01;
    static final int HIGHRES_SIZE = 4000;
    static HersheyFont hersheyFont;
    private final MainClass main = MainClass.processing;
    private final boolean debug = true;
    private final boolean fillCanvas = true;
    private final boolean overrideColor = false;
    private final Color oColor = new Color(0);
    private final ArrayList<String> gCodeBuffer;
    private final PVector scale;
    public boolean lockseed = false;
    public int drawBufferPooling = 1;
    public boolean loadPixelsEveryFrame = false;
    String filename;
    private PVector documentSize;
    private boolean newShape = true;
    private ArrayList<StrokePoint> activeStroke;
    private boolean activeStrokeDrawn = false;
    private ArrayList<ArrayList<StrokePoint>> strokes;
    private ArrayList<ArrayList<StrokePoint>> drawBuffer;
    private PVector penPos;
    private boolean penUp;
    private float[] bounds = new float[4];
    private float drawLength;
    private float travelLength;
    private int stackPosition;

    private PGraphics highResBuffer;
    private PGraphics workBuffer;
    private int seed = 0;
    private boolean sorting = false;

    GCode(int documentWidth, int documentHeight) {
        documentSize = new PVector(documentWidth, documentHeight);

        //Figure Out Scale
        scale = new PVector((documentSize.x) / main.width, (documentSize.y) / main.height);
        debugMessage("\n------------------------\n------------------------\nINITIALIZING GCODE MODULE\n------------------------\n------------------------\n");
        debugMessage("Document Dimensions: " + documentWidth + "x" + documentHeight + "\n");
        debugMessage("Sketch Dimensions: " + main.width + "x" + main.height + "\n");
        debugMessage("Scale: " + scale.x + "x" + scale.y + "\n");
        debugMessage("\n------------------------\n------------------------\nREADY TO RECEIVE INPUT\n-----------------------\n------------------------\n");

        filename = day() + "_" + month() + "_" + year() + "/" + hour() + "_" + minute() + "_" + second() + "_" + floor(main.random(1000, 9999));

        strokes = new ArrayList<ArrayList<StrokePoint>>();
        drawBuffer = new ArrayList<ArrayList<StrokePoint>>();
        gCodeBuffer = new ArrayList<String>();
        penPos = new PVector(-99999, -99999);
        hersheyFont = new HersheyFont(main, "futural.jhf");
        drawLength = 0;
        travelLength = 0;
        penUp = true;

        //reset bounds
        bounds[0] = 999999;
        bounds[1] = 999999;
        bounds[2] = -999999;
        bounds[3] = -999999;
    }

    //ON SCREEN DRAWING
    void renderHighRes() {
        highResBuffer = main.createGraphics(HIGHRES_SIZE, HIGHRES_SIZE);
        highResBuffer.smooth(4);
        highResBuffer.beginDraw();
        highResBuffer.background(main.style.background.getRGB());
        for (ArrayList<StrokePoint> sa : strokes) {
            highResBuffer.strokeWeight((float) main.style.strokeweight / main.width * HIGHRES_SIZE);
            highResBuffer.stroke(main.style.foreground.getRGB());
            highResBuffer.noFill();
            highResBuffer.beginShape();
            for (StrokePoint s : sa) {
                highResBuffer.vertex(s.pos.x / main.width * HIGHRES_SIZE, s.pos.y / main.height * HIGHRES_SIZE);
            }
            highResBuffer.endShape();
        }
    }


    public PGraphics getWorkBuffer() {
        return workBuffer;
    }

    public void setWorkBuffer(PGraphics workBuffer) {
        this.workBuffer = workBuffer;
    }

    void optimizedDrawShape() {
        optimizedDrawShape(strokes);
    }

    void optimizedDrawShape(ArrayList<ArrayList<StrokePoint>> stack) {
        main.drawThreadRunning = true;
        main.noFill();
        if (workBuffer == null) {
            workBuffer = main.createGraphics(main.width, main.height);
        } else {
            workBuffer.clear();
        }
        workBuffer.smooth(4);
        workBuffer.beginDraw();
        workBuffer.background(main.style.background.getRGB());
        try {
            ArrayList<ArrayList<StrokePoint>> DrawCopy = (ArrayList<ArrayList<StrokePoint>>) stack.clone();
            for (ArrayList<StrokePoint> SA : DrawCopy) {
                drawStroke(SA);
            }
        } catch (Exception ignored) {
        }
        main.delay(200);
        main.drawThreadRunning = false;
    }

    ArrayList<ArrayList<StrokePoint>> getTextList(String text, int size, PVector pos, float spacing) {
        hersheyFont.textSize(size);
        PShape shape = hersheyFont.getShape(text);
        ArrayList<ArrayList<StrokePoint>> textStrokes = new ArrayList<ArrayList<StrokePoint>>();
        for (PShape l : shape.getChildren()) {
            pos.sub(new PVector((float) size * spacing, 0));
            for (int v = 1; v < l.getVertexCodeCount(); v += 2) {
                ArrayList<StrokePoint> letterStroke = new ArrayList<StrokePoint>();
                PVector vPos1 = l.getVertex(v - 1);
                PVector vPos2 = l.getVertex(v);
                vPos1.add(pos);
                vPos2.add(pos);
                vPos1.y = vPos1.y * -1;
                vPos2.y = vPos2.y * -1;
                letterStroke.add(new StrokePoint(vPos1, main.style));
                letterStroke.add(new StrokePoint(vPos2, main.style));
                textStrokes.add(letterStroke);
            }
        }
        return textStrokes;
    }

    void drawSection(int delta, int total) {
        main.noFill();

        float chunk = (float) strokes.size() / total;
        float start = (float) delta * chunk;

        for (int i = floor(start); i < floor(start + chunk); i++) {
            if (strokes.size() <= i) continue;
            drawStroke(strokes.get(i));
        }
    }

    void drawStroke(ArrayList<StrokePoint> Stroke) {
        workBuffer.beginShape();
        workBuffer.noFill();
        for (StrokePoint s : Stroke) {
            workBuffer.strokeWeight((float) s.weight);
            workBuffer.stroke(s.col.getRGB());
            if (overrideColor) workBuffer.stroke(oColor.getRGB());
            workBuffer.vertex(s.pos.x, s.pos.y);
        }
        workBuffer.endShape();
    }

    //DATA
    void addPoint(float x, float y) {
        if (main.killThread) return;
        PVector pos = new PVector(x, y);
        if (newShape) {
            if (pointIsVisible(pos)) {
                //if(ActiveStroke != null)PrintStroke(ActiveStroke);
                ArrayList<StrokePoint> newStroke;
                if (strokes.size() > 0 && strokes.get(strokes.size() - 1).size() == 1 && !pointIsVisible(strokes.get(strokes.size() - 1).get(0).pos)) {
                    //previous stroke is not on the screen at all, let's recycle it
                    newStroke = activeStroke;
                } else {
                    newStroke = new ArrayList<StrokePoint>();
                }
                newStroke.add(new StrokePoint(pos, main.style));
                activeStroke = newStroke;
                activeStrokeDrawn = false;
                strokes.add(newStroke);
                if (pointIsVisible(pos)) expandBounds(pos);
                newShape = false;
            }
        } else {
            /*
            if (activeStroke.size() == 1) {
                if (!pointIsVisible(activeStroke.get(0).pos)) {
                    if (pointIsVisible(pos)) {
                        PVector[] lineSegment = new PVector[2];
                        lineSegment[0] = activeStroke.get(0).pos.copy();
                        lineSegment[1] = pos.copy();
                        PVector[] clippedLineSegment = clipLine(lineSegment);
                        if(clippedLineSegment[0].dist(clippedLineSegment[1])<0){
                            activeStroke.set(0, new StrokePoint(lineSegment[0], main.style));
                            activeStroke.add(new StrokePoint(lineSegment[1], main.style));
                            expandBounds(clipLine(lineSegment)[0]);
                            expandBounds(clipLine(lineSegment)[1]);
                        } else {
                            activeStroke.set(0, new StrokePoint(clipLine(lineSegment)[0], main.style));
                            expandBounds(clipLine(lineSegment)[0]);
                        }
                        return;
                    } else {
                        activeStroke.set(0, new StrokePoint(pos, main.style));
                        return;
                    }
                }
            }
            */

            if (pointIsVisible(pos)) {
                if (PVector.dist(pos, activeStroke.get(activeStroke.size() - 1).pos) > 0.5f) {
                    if (activeStroke.size() == 1 && strokes.size() > 1) {
                        ArrayList<StrokePoint> sp = strokes.get(strokes.size() - 2);
                        travelLength += PVector.dist(sp.get(sp.size() - 1).pos, activeStroke.get(0).pos);
                    }
                    drawLength += PVector.dist(activeStroke.get(activeStroke.size() - 1).pos, pos);

                    activeStroke.add(new StrokePoint(pos, main.style));
                    expandBounds(pos);
                }

            } else {
                newShape();
            }
            /*
            PVector[] lineSegment = new PVector[2];
            lineSegment[0] = activeStroke.get(activeStroke.size() - 1).pos;
            lineSegment[1] = pos;
            if (lineNeedsClipping(lineSegment)) {
                //clip this line
                PVector clippedPos = clipLine(lineSegment)[1];
                activeStroke.add(new StrokePoint(clippedPos, main.style));
                expandBounds(clippedPos);
            } else {
                //add this point as is

                activeStroke.add(new StrokePoint(pos, main.style));
                expandBounds(pos);
            }

             */
        }
    }

    void addLine(float x1, float y1, float x2, float y2) {
        newShape = true;
        addPoint(x1, y1);
        addPoint(x2, y2);
        newShape = true;
    }

    void addLine(PVector v1, PVector v2) {
        addLine(v1.x, v1.y, v2.x, v2.y);
    }

    void newShape() {

        if (main.killThread) return;

        //Let's clean up the previous stroke if it was either empty or only has one point.
        if (strokes.size() > 0) {
            if (strokes.get(strokes.size() - 1).size() < 2) {
                strokes.remove(strokes.size() - 1);
                newShape = true;
                return;
            }
        }
        newShape = true;

        //Logic for drawing to the PGraphic
        if (!activeStrokeDrawn && activeStroke != null && main.drawThreadRunning) {
            if (activeStroke.size() > 1) {
                if (drawBuffer.size() > drawBufferPooling) {
                    for (ArrayList<StrokePoint> as : drawBuffer) {
                        workBuffer.beginShape();
                        for (StrokePoint s : as) {
                            if (main.killThread) {
                                workBuffer.endShape();
                                activeStrokeDrawn = true;
                                return;
                            }
                            workBuffer.strokeWeight((float) s.weight);
                            workBuffer.stroke(s.col.getRGB());
                            if (overrideColor) workBuffer.stroke(oColor.getRGB());
                            workBuffer.vertex(s.pos.x, s.pos.y);
                        }
                        workBuffer.endShape();
                        if (loadPixelsEveryFrame) workBuffer.loadPixels();
                    }
                    drawBuffer.clear();
                }

            }
            drawBuffer.add(activeStroke);
            activeStrokeDrawn = true;
        }
    }

    //GCODE SAVING
    void SaveGCode(String fileName) {
        //verify that our last stroke is not off screen.
        if (strokes.size() > 0 && strokes.get(strokes.size() - 1).size() == 1 && !pointIsVisible(strokes.get(strokes.size() - 1).get(0).pos)) {
            strokes.remove(strokes.get(strokes.size() - 1));
        }
        gCodeBuffer.clear();
        debugMessage("\n------------------------\n------------------------\nGENERATING GCODE FOR THIS SKETCH, PLEASE STAND BY\n------------------------\n------------------------\n");
        debugMessage("Original bounds:" + bounds[0] + "-" + bounds[1] + "   " + bounds[2] + "-" + bounds[3] + "\n");
        gCodeBuffer.add(CONFIG_GCODE_PRE);
        for (ArrayList<StrokePoint> Stroke : strokes) {
            gcodeStroke(Stroke);
        }
        penUp();
        gCodeBuffer.add(CONFIG_GCODE_POST);

        //Convert our arraylist to a vanilla array
        String[] VanillaStrings = new String[gCodeBuffer.size()];
        for (int i = 0; i < gCodeBuffer.size(); i++) {
            VanillaStrings[i] = gCodeBuffer.get(i);
        }
        //Output file
        main.saveStrings("exportedGCODE/" + fileName + ".gcode", VanillaStrings);
        debugMessage("\n------------------------\n------------------------\nDONE GENERATING GCODE FOR THIS SKETCH\n------------------------\n------------------------\n");
        gcodeGenerateSignature(fileName);
    }

    void gcodeGenerateSignature(String fileName) {
        ArrayList<ArrayList<StrokePoint>> signatureStrokes = getTextList(year() + "/" + month() + "/" + day() + " - PLOT.xyz", 10, new PVector(50, 50), .8f);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(main.sketch.name.getBytes());
            byte[] digest = md.digest();
            String myHash = DatatypeConverter
                    .printHexBinary(digest).toUpperCase();
            signatureStrokes.addAll(getTextList(myHash, 8, new PVector(50, 65), 1));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        gCodeBuffer.clear();
        debugMessage("\n------------------------\n------------------------\nGENERATING SIGNATURE FOR THIS SKETCH, PLEASE STAND BY\n------------------------\n------------------------\n");
        gCodeBuffer.add(CONFIG_GCODE_PRE);
        refreshBounds(signatureStrokes);
        PVector holdDocSize = documentSize.copy();
        documentSize = new PVector(60, 60);
        for (ArrayList<StrokePoint> Stroke : signatureStrokes) {
            gcodeStroke(Stroke);
        }
        penUp();
        gCodeBuffer.add(CONFIG_GCODE_POST);
        //Convert our arraylist to a vanilla array
        String[] VanillaStrings = new String[gCodeBuffer.size()];
        for (int i = 0; i < gCodeBuffer.size(); i++) {
            VanillaStrings[i] = gCodeBuffer.get(i);
        }
        documentSize = holdDocSize.copy();
        refreshBounds(strokes);
        //Output file
        main.saveStrings("exportedGCODE/" + fileName + "_signature.gcode", VanillaStrings);
        debugMessage("\n------------------------\n------------------------\nDONE GENERATING SIGNATURE FOR THIS SKETCH\n------------------------\n------------------------\n");

    }

    void gcodeStroke(ArrayList<StrokePoint> stroke) {
        //this is our first stroke. let's move there and draw it.
        //first make sure our pen is definitely up
        gCodeBuffer.add("\n;--------------------------;\n;SHAPE " + strokes.indexOf(stroke) + " Contains: " + stroke.size() + " points;");
        if (penNeedsToGoUp(penPos, stroke.get(0).pos)) {
            penUp();
            moveTo(stroke.get(0).pos);
            penDown();
        }
        for (int i = 1; i < stroke.size(); i++) {
            moveTo(stroke.get(i).pos);
        }
    }


    void penUp() {
        if (!penUp) {
            gCodeBuffer.add(CONFIG_GCODE_PEN_UP);
            penUp = true;
        }
    }

    void penDown() {
        if (penUp) {
            gCodeBuffer.add(CONFIG_GCODE_PEN_DOWN);
            penUp = false;
        }
    }

    void moveTo(PVector loc) {
        penPos = loc.copy();
        gCodeBuffer.add("G0 F" + CONFIG_GCODE_MOVE_FEEDRATE +
                " X" + nf(convertScale(loc, documentSize).x, 0, 5) +
                " Y" + nf(convertScale(loc, documentSize).y, 0, 5));
    }

    boolean penNeedsToGoUp(PVector p1, PVector p2) {
        return (abs(PVector.dist(p1, p2)) > PRECISSION);
    }

    void refreshBounds(ArrayList<ArrayList<StrokePoint>> stack) {
        bounds[0] = 999999;
        bounds[1] = 999999;
        bounds[2] = -999999;
        bounds[3] = -999999;
        for (ArrayList<StrokePoint> se : stack) {
            for (StrokePoint s : se) {
                if (s.pos.x < bounds[0]) bounds[0] = s.pos.x;
                if (s.pos.y < bounds[1]) bounds[1] = s.pos.y;
                if (s.pos.x > bounds[2]) bounds[2] = s.pos.x;
                if (s.pos.y > bounds[3]) bounds[3] = s.pos.y;
            }
        }
    }

    //HELPERS
    void center() {
        float[] newBounds = new float[4];
        newBounds[0] = 999999;
        newBounds[1] = 999999;
        newBounds[2] = -999999;
        newBounds[3] = -999999;
        for (ArrayList<StrokePoint> se : strokes) {
            for (StrokePoint s : se) {
                s.pos = convertScaleCentered(s.pos, new PVector(main.width, main.height));
                if (s.pos.x < newBounds[0]) newBounds[0] = s.pos.x;
                if (s.pos.y < newBounds[1]) newBounds[1] = s.pos.y;
                if (s.pos.x > newBounds[2]) newBounds[2] = s.pos.x;
                if (s.pos.y > newBounds[3]) newBounds[3] = s.pos.y;
            }
        }
        bounds = newBounds.clone();
    }


    PVector[] clipLine(PVector @NotNull [] line) {
        PVector[] clippedLine = new PVector[2];
        clippedLine = line.clone();
        //is start point outside?
        if (!pointIsVisible(line[0])) {
            if (line[0].x < 0) {
                /* point is to the left of clip window */
                clippedLine[0].x = 0;
                if (clippedLine[0].x - clippedLine[1].x == 0) {
                    clippedLine[0].y = clippedLine[1].y;
                } else {
                    clippedLine[0].y = ((line[0].y - line[1].y) / (line[0].x - line[1].x)) * (clippedLine[0].x - line[1].x) + line[1].y;
                }

            } else if (line[0].x > main.width) {
                /* point is to the right of clip window */
                clippedLine[0].x = main.width;
                if (clippedLine[0].x - clippedLine[1].x == 0) {
                    clippedLine[0].y = clippedLine[1].y;
                } else {
                    clippedLine[0].y = ((line[0].y - line[1].y) / (line[0].x - line[1].x)) * (clippedLine[0].x - line[1].x) + line[1].y;
                }
            } else if (line[0].y < 0) {
                /* point is above the clip window */
                clippedLine[0].y = 0;
                if (clippedLine[0].y - clippedLine[1].y == 0) {
                    clippedLine[0].x = clippedLine[1].x;
                } else {
                    clippedLine[0].x = ((line[0].x - line[1].x) / (line[0].y - line[1].y)) * (clippedLine[0].y - line[1].y) + line[1].x;
                }
            } else if (line[0].y > main.width) {
                /* point is below the clip window */
                clippedLine[0].y = main.height;
                if (clippedLine[0].y - clippedLine[1].y == 0) {
                    clippedLine[0].x = clippedLine[1].x;
                } else {
                    clippedLine[0].x = ((line[0].x - line[1].x) / (line[0].y - line[1].y)) * (clippedLine[0].y - line[1].y) + line[1].x;
                }
            }
        }
        if (!pointIsVisible(line[1])) {
            if (line[1].x < 0) {
                /* point is to the left of clip window */
                clippedLine[1].x = 0;
                if (clippedLine[0].x - clippedLine[1].x == 0) {
                    clippedLine[0].y = clippedLine[1].y;
                } else {
                    clippedLine[1].y = ((line[1].y - line[0].y) / (line[1].x - line[0].x)) * (clippedLine[1].x - line[0].x) + line[0].y;
                }
            } else if (line[1].x > main.width) {
                /* point is to the right of clip window */
                clippedLine[1].x = main.width;
                if (clippedLine[0].x - clippedLine[1].x == 0) {
                    clippedLine[0].y = clippedLine[1].y;
                } else {
                    clippedLine[1].y = ((line[0].y - line[0].y) / (line[1].x - line[0].x)) * (clippedLine[1].x - line[0].x) + line[0].y;
                }
            } else if (line[1].y < 0) {
                /* point is above the clip window */
                clippedLine[1].y = 0;
                if (clippedLine[0].y - clippedLine[1].y == 0) {
                    clippedLine[0].x = clippedLine[1].x;
                } else {
                    clippedLine[1].x = ((line[1].x - line[0].x) / (line[1].y - line[0].y)) * (clippedLine[1].y - line[0].y) + line[0].x;
                }
            } else if (line[1].y > main.width) {
                /* point is below the clip window */
                clippedLine[1].y = main.height;
                if (clippedLine[0].y - clippedLine[1].y == 0) {
                    clippedLine[0].x = clippedLine[1].x;
                } else {
                    clippedLine[1].x = ((line[1].x - line[0].x) / (line[1].y - line[0].y)) * (clippedLine[1].y - line[0].y) + line[0].x;
                }
            }
        }
        return clippedLine;
    }

    boolean lineNeedsClipping(PVector[] line) {
        return !(pointIsVisible(line[0]) && pointIsVisible(line[1]));
    }

    boolean lineIsVisible(PVector[] line) {
        return (pointIsVisible(line[0]) || pointIsVisible(line[1]));
    }

    boolean pointIsVisible(PVector point) {
        return (point.x >= 0 && point.y >= 0 && point.x <= main.width && point.y <= main.height);
    }

    void expandBounds(@NotNull PVector point) {
        if (point.x < bounds[0]) bounds[0] = point.x;
        if (point.y < bounds[1]) bounds[1] = point.y;
        if (point.x > bounds[2]) bounds[2] = point.x;
        if (point.y > bounds[3]) bounds[3] = point.y;
    }

    void sortStack() {
        workBuffer.clear();
        workBuffer.smooth(4);
        workBuffer.beginDraw();
        workBuffer.background(0);
        PVector sortPos = new PVector((float) main.width / 2, (float) main.height / 2);
        stackPosition = 0;
        PVector prevShortestPos = null;
        while (stackPosition < strokes.size() - 1) {
            float dist = 999999;
            ArrayList<StrokePoint> shortest = new ArrayList<StrokePoint>();
            boolean inverse = false;
            for (int a = stackPosition; a < strokes.size(); a++) {
                ArrayList<StrokePoint> stroke = strokes.get(a);
                float newDistForward = PVector.dist(stroke.get(0).pos, sortPos);
                if (newDistForward < dist) {
                    shortest = stroke;
                    dist = newDistForward;
                    inverse = false;
                    if (dist < PRECISSION) break;
                }

                float newDistBackward = PVector.dist(stroke.get(stroke.size() - 1).pos, sortPos);
                if (newDistBackward < dist) {
                    shortest = stroke;
                    dist = newDistForward;
                    inverse = true;
                    if (dist < PRECISSION) break;
                }
            }
            if (inverse) Collections.reverse(shortest);

            Collections.swap(strokes, strokes.indexOf(shortest), stackPosition);
            workBuffer.strokeWeight(0.5f);
            workBuffer.stroke(100);
            workBuffer.beginShape();
            for (StrokePoint s : shortest) {
                workBuffer.vertex(s.pos.x, s.pos.y);
            }
            workBuffer.endShape();
            if (stackPosition != 0) {
                workBuffer.stroke(floor(((1 - (float) stackPosition / strokes.size())) * 255), floor(((float) stackPosition / strokes.size()) * 255), 0);
                workBuffer.strokeWeight(0.2f);
                workBuffer.line(sortPos.x, sortPos.y, shortest.get(0).pos.x, shortest.get(0).pos.y);
                workBuffer.text(stackPosition, sortPos.x, sortPos.y);
            }

            sortPos = shortest.get(shortest.size() - 1).pos.copy();
            stackPosition++;
        }
        print("DONE SORTING " + stackPosition + "\n");
        recalculateLengths();
        sortPos.x = (float) main.width / 2;
        sortPos.y = (float) main.height / 2;
    }

    void clearBuffers() {
        activeStroke = null;
        highResBuffer = main.createGraphics(HIGHRES_SIZE, HIGHRES_SIZE);
        highResBuffer.beginDraw();
        highResBuffer.background(main.style.background.getRGB());
        highResBuffer.noFill();

        workBuffer = main.createGraphics(main.width, main.height);
        workBuffer.smooth(4);
        workBuffer.beginDraw();
        workBuffer.background(main.style.background.getRGB());
        workBuffer.noFill();


        strokes = new ArrayList<ArrayList<StrokePoint>>();
        drawBuffer = new ArrayList<ArrayList<StrokePoint>>();

        penPos = new PVector(-99999, -99999);
        penUp = true;
        drawLength = 0;
        travelLength = 0;
        filename = day() + "_" + month() + "_" + year() + "/" + hour() + "_" + minute() + "_" + second() + "_" + floor(main.random(1000, 9999));

        //reset bounds
        bounds[0] = 999999;
        bounds[1] = 999999;
        bounds[2] = -999999;
        bounds[3] = -999999;
    }

    PVector convertScale(PVector OriginalPosition, PVector newSize) {
        float maxAxis = max((bounds[2] - bounds[0]), (bounds[3] - bounds[1]));
        float sf = (main.width / maxAxis * newSize.x / main.width);
        return new PVector((OriginalPosition.x - bounds[0]) * sf,
                (OriginalPosition.y - bounds[1]) * sf);
    }

    PVector convertScaleCentered(PVector OriginalPosition, PVector newSize) {
        float boundsWidth = bounds[2] - bounds[0];
        float boundsHeight = bounds[3] - bounds[1];
        float maxAxis = max(boundsWidth, boundsHeight);
        float xOffset = 0;
        float yOffset = 0;
        float sf = (main.width / maxAxis * newSize.x / main.width);
        if (boundsWidth > boundsHeight) {
            yOffset = (boundsWidth - boundsHeight) * sf / 2;
        } else {
            xOffset = (boundsHeight - boundsWidth) * sf / 2;
        }
        return new PVector((OriginalPosition.x - bounds[0] + xOffset) * sf, (OriginalPosition.y - bounds[1] + yOffset) * sf);
    }

    public PGraphics getHighResBuffer() {
        return highResBuffer;
    }

    void saveGCode() {
        float time = main.millis();
        SaveGCode(filename);
        renderHighRes();
        saveHighRes();
        print("\nDONE SAVING FILE: Took " + (main.millis() - time) + "ms\n");
    }

    void saveHighRes() {
        highResBuffer.save("exportedGCODE/" + filename + ".png");
    }

    void debugMessage(String msg) {
        if (debug) print(msg);
    }

    float numStrokes() {
        return strokes.size();
    }

    public PVector getTravelBuffer() {
        return new PVector(drawLength, travelLength);
    }

    public boolean isSorting() {
        return sorting;
    }

    public void setSorting(boolean b) {
        sorting = b;
    }

    public int getStackPosition() {
        return stackPosition;
    }

    public void drawLastSection() {
        if (!drawBuffer.contains(activeStroke)) drawBuffer.add(activeStroke);
        if (drawBuffer.size() > 0) {
            for (ArrayList<StrokePoint> sa : drawBuffer) {
                if (sa.size() > 1) {
                    workBuffer.beginShape();
                    for (StrokePoint s : sa) {
                        workBuffer.strokeWeight((float) s.weight);
                        workBuffer.stroke(s.col.getRGB());
                        if (overrideColor) workBuffer.stroke(oColor.getRGB());
                        workBuffer.vertex(s.pos.x, s.pos.y);
                    }
                    workBuffer.endShape();
                }
            }
            drawBuffer.clear();
        }
    }


    public int getSeed() {
        if (!lockseed) seed = floor(main.random(0, 999999));
        return seed;
    }

    public void addPoint(PVector pos) {
        addPoint(pos.x, pos.y);
    }

    public void debugPrintStrokes() {
        print("\n\nSTROKE STACK:");
        for (ArrayList<StrokePoint> sa : strokes) {
            print("\nSTROKE " + strokes.indexOf(sa) + ":\n");
            for (StrokePoint s : sa) {
                print("[" + sa.indexOf(s) + " | x:" + s.pos.x + " * y:" + s.pos.y + "]\n");
            }
        }
    }

    public void recalculateLengths() {
        float prevTravel = travelLength;
        travelLength = 0;
        for (ArrayList<StrokePoint> sa : strokes) {
            if (strokes.indexOf(sa) > 0) {
                ArrayList<StrokePoint> sp = strokes.get(strokes.indexOf(sa) - 1);
                travelLength += PVector.dist(sp.get(sp.size() - 1).pos, sa.get(0).pos);
            }

        }
        print("Travel distance before: " + prevTravel / main.width * documentSize.x / 1000 + " meters\nTravel distance now: " + travelLength / main.width * documentSize.x / 1000 + " meters\nThat is a " + (100 - round(travelLength / prevTravel * 100)) + "% improvement!\n");
    }
}

class Style {
    Color background = new Color(0, 0, 0);
    Color foreground = new Color(255, 255, 255);
    double strokeweight = 0.7;
    boolean strokeVariation = true;
    double strokeweightNoise = 0.5;
    int strokeBlendMode = REPLACE;
}