package pl.pdec.billiards.logic;

import pl.pdec.billiards.components.Ball;
import pl.pdec.billiards.components.Stick;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class StickControllerRunnable implements Runnable, MouseListener {
    private volatile boolean shutdown = false;
    private final Container container;
    private final Stick stick;
    private final Ball whiteBall;
    private final double baseVelocity = 10;

    public StickControllerRunnable(Container container, Stick stick, Ball whiteBall) {
        this.container = container;
        this.stick = stick;
        this.whiteBall = whiteBall;

        this.container.addMouseListener(this);
    }

    public void shutdown() {
        shutdown = true;
    }

    @Override
    public void run() {
        while (!shutdown) {
            Point mousePos = container.getMousePosition();
            if (mousePos != null && stick != null && whiteBall != null) {
                stick.setPoint(mousePos.x, mousePos.y, whiteBall.getX(), whiteBall.getY());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        double[] strikeDirection = stick.getStrikeDirection();
        whiteBall.setDirection(strikeDirection);
        whiteBall.setVelocity(baseVelocity * stick.getStrikePower());
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
