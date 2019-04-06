package pl.pdec.billiards.components;

import pl.pdec.billiards.DrawableInterface;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Objects;

public class Ball implements DrawableInterface {
    public static final int DIMENSION = 20;
    private final int number;
    private final boolean isWhite;
    private final Color color;
    private double x, y;
    private boolean isInPocket = false;
    private volatile double velocity = 0;
    private double[] directionVector = new double[2];

    public Ball(int number, boolean isWhite, Color color, int x, int y) {
        this.number = number;
        this.isWhite = isWhite;
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public double[] getDirection() {
        return directionVector;
    }

    public void setDirection(double[] directionVector) {
        this.directionVector = directionVector;
    }

    public void setDirection(double x, double y) {
        directionVector[0] = x;
        directionVector[1] = y;
    }

    synchronized public double getVelocity() {
        return velocity;
    }

    synchronized public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    synchronized public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    synchronized public double getX() {
        return x;
    }

    synchronized public double getY() {
        return y;
    }

    synchronized public boolean isInPocket() {
        return isInPocket;
    }

    synchronized public void setIsInPocket() {
        isInPocket = true;
    }

    public int getNumber() {
        return number;
    }

    public boolean isWhite() {
        return isWhite;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Ellipse2D.Double circle = new Ellipse2D.Double(x - DIMENSION / 2., y - DIMENSION / 2., DIMENSION, DIMENSION);

        g2d.setColor(color);
        g2d.fill(circle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ball ball = (Ball) o;
        return number == ball.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}
