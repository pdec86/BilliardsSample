package pl.pdec.billiards.components;

import pl.pdec.billiards.DrawableInterface;

import java.awt.*;

public class Stick implements DrawableInterface {
    private double pointX, pointY;
    private double pointingX, pointingY;
    private int length = 100;

    private double[] stickVector = new double[2];
    private double magnitude = 1, normX, normY;
    private int[] linePoints = new int[4];

    public void setPoint(double pointX, double pointY, double pointingX, double pointingY) {
        synchronized (this) {
            this.pointX = pointX;
            this.pointY = pointY;
            this.pointingX = pointingX;
            this.pointingY = pointingY;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        synchronized (this) {
            stickVector[0] = pointX - pointingX;
            stickVector[1] = pointY - pointingY;
            magnitude = Math.sqrt(Math.pow(pointX - pointingX, 2) + Math.pow(pointY - pointingY, 2));

            normX = stickVector[0] / magnitude;
            normY = stickVector[1] / magnitude;

            linePoints[0] = (int) pointX;
            linePoints[1] = (int) pointY;
            linePoints[2] = (int) ((int) pointX + normX * length);
            linePoints[3] = (int) ((int) pointY + normY * length);
        }

        g2d.setPaint(Color.BLUE);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(linePoints[0], linePoints[1], linePoints[2], linePoints[3]);
    }

    public double getStrikePower() {
        return Math.sqrt(Math.pow(pointX - pointingX, 2) + Math.pow(pointY - pointingY, 2)) / 2;
    }

    public double[] getStrikeDirection() {
        double[] vector = new double[2];
        vector[0] = pointingX - pointX;
        vector[1] = pointingY - pointY;
        magnitude = Math.sqrt(Math.pow(pointingX - pointX, 2) + Math.pow(pointingY - pointY, 2));

        normX = vector[0] / magnitude;
        normY = vector[1] / magnitude;

        return new double[]{normX, normY};
    }
}
