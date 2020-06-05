package SketchUtils;

import processing.core.PVector;

public class Particle {
    private float age;
    private PVector pos;
    private PVector vel;
    private PVector acc;

    public float getDrag() {
        return drag;
    }

    public void setDrag(float drag) {
        this.drag = drag;
    }

    private float drag;

    public float getMaxForce() {
        return maxForce;
    }

    public void setMaxForce(float maxForce) {
        this.maxForce = maxForce;
    }

    private float maxForce;

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    private float maxSpeed;

    public Particle(PVector position, PVector velocity) {
        pos = position;
        vel = velocity;
        acc = new PVector(0,0);
        maxSpeed = 4;
        maxForce = 0.1f;
        drag = 0.9f;
        age = 0;
    }



    void age(float dt){
        age += dt;
    }
    public void applyForce(PVector force){
        force.limit(maxForce);
        acc.add(force);
    }

    public void update() {
        vel.add(acc);
        vel.limit(maxSpeed);
        vel.mult(drag);
        pos.add(vel);
        acc.mult(0);
    }

    public PVector getPos() {
        return pos;
    }

    public void setPos(PVector pos) {
        this.pos = pos;
    }



    public PVector getVel() {
        return vel;
    }

    public void setVel(PVector vel) {
        this.vel = vel;
    }



    public float getAge() {
        return age;
    }

    public void setAge(float age) {
        this.age = age;
    }
}
