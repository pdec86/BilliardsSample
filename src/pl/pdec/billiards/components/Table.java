package pl.pdec.billiards.components;

import pl.pdec.billiards.DrawableInterface;
import pl.pdec.billiards.helpers.VectorCalc;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

public class Table implements DrawableInterface {

    private final double x, y;
    private final double width, height;
    private final int holeDim = 20;

    public Table(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean hitPocket(double posX, double posY) {
        double[] ltPocket = new double[]{x, y};
        double[] rtPocket = new double[]{x + width, y};
        double[] rbPocket = new double[]{x + width, y + height};
        double[] lbPocket = new double[]{x, y + height};
        double[] toCheck = new double[]{posX, posY};

        return VectorCalc.distanceBetweenPoints(ltPocket, toCheck) < holeDim
                || VectorCalc.distanceBetweenPoints(rtPocket, toCheck) < holeDim
                || VectorCalc.distanceBetweenPoints(rbPocket, toCheck) < holeDim
                || VectorCalc.distanceBetweenPoints(lbPocket, toCheck) < holeDim;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Rectangle2D table = new Rectangle2D.Double(x, y, width, height);

        g2d.setColor(Color.GREEN);
        g2d.fill(table);

        g2d.setColor(Color.BLACK);
        g2d.drawLine((int) x, (int) (y + height - height / 3), (int) (x + width), (int) (y + height - height / 3));

        GeneralPath hole = new GeneralPath();
        this.drawHoleLT(hole, holeDim, g2d);
        this.drawHoleLB(hole, holeDim, g2d);
        this.drawHoleRT(hole, holeDim, g2d);
        this.drawHoleRB(hole, holeDim, g2d);
    }

    private void drawHoleLT(GeneralPath hole, int holeDim, Graphics2D g2d) {
        hole.moveTo(x, y);
        hole.lineTo(x + holeDim, y);
        hole.curveTo(x + holeDim, y + holeDim / 2.,
                x + holeDim / 2., y + holeDim,
                x, y + holeDim);
        hole.closePath();
        g2d.setColor(Color.BLACK);
        g2d.fill(hole);
    }

    private void drawHoleLB(GeneralPath hole, int holeDim, Graphics2D g2d) {
        hole.moveTo(x, y + height);
        hole.lineTo(x + holeDim, y + height);
        hole.curveTo(x + holeDim, y + height - holeDim / 2.,
                x + holeDim / 2., y + height - holeDim,
                x, y + height - holeDim);
        hole.closePath();
        g2d.setColor(Color.BLACK);
        g2d.fill(hole);
    }

    private void drawHoleRT(GeneralPath hole, int holeDim, Graphics2D g2d) {
        hole.moveTo(x + width, y);
        hole.lineTo(x + width - holeDim, y);
        hole.curveTo(x + width - holeDim, y + holeDim / 2.,
                x + width - holeDim / 2., y + holeDim,
                x + width, y + holeDim);
        hole.closePath();
        g2d.setColor(Color.BLACK);
        g2d.fill(hole);
    }

    private void drawHoleRB(GeneralPath hole, int holeDim, Graphics2D g2d) {
        hole.moveTo(x + width, y + height);
        hole.lineTo(x + width - holeDim, y + height);
        hole.curveTo(x + width - holeDim, y + height - holeDim / 2.,
                x + width - holeDim / 2., y + height - holeDim,
                x + width, y + height - holeDim);
        hole.closePath();
        g2d.setColor(Color.BLACK);
        g2d.fill(hole);
    }
}
