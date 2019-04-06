package pl.pdec.billiards;

import pl.pdec.billiards.components.Ball;
import pl.pdec.billiards.components.Stick;
import pl.pdec.billiards.components.Table;
import pl.pdec.billiards.logic.GameMechanic;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Game extends JPanel {
    private GameMechanic gameMechanic = null;
    private Table table = null;
    private Set<Ball> balls = new HashSet<>();
    private Stick stick = null;
    private Ball whiteBall = null;
    private volatile boolean gameEnded = false;

    public Game() {
        setBackground(Color.LIGHT_GRAY);
    }

    public Collection<Ball> getBalls() {
        return balls;
    }

    public Ball getWhiteBall() {
        return whiteBall;
    }

    public void reset() {
        if (gameMechanic != null) {
            gameMechanic.shutdown();
        }
        clearBalls();
        add9BallsGame();
        gameEnded = false;
        gameMechanic = new GameMechanic(table, balls);
    }

    public void setStick(Stick stick) {
        this.stick = stick;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    private void clearBalls() {
        this.balls.clear();
    }

    private void add9BallsGame() {
        int leadBallX = (int) (table.getX() + table.getWidth() / 2);
        int leadBallY = (int) (table.getY() + table.getHeight() / 4);
        Ball leadBall = new Ball(1, false, Color.RED, leadBallX, leadBallY);
        this.balls.add(leadBall);

        this.balls.add(new Ball(2, false, Color.RED,
                leadBallX - Ball.DIMENSION / 2,
                leadBallY - Ball.DIMENSION));
        this.balls.add(new Ball(3, false, Color.RED,
                leadBallX + Ball.DIMENSION / 2,
                leadBallY - Ball.DIMENSION));

        Ball middleBall = new Ball(4, false, Color.RED,
                leadBallX,
                leadBallY - Ball.DIMENSION * 2);
        this.balls.add(middleBall);
        this.balls.add(new Ball(5, false, Color.RED,
                (int) (middleBall.getX() - Ball.DIMENSION),
                (int) middleBall.getY()));
        this.balls.add(new Ball(6, false, Color.RED,
                (int) (middleBall.getX() + Ball.DIMENSION),
                (int) middleBall.getY()));

        this.balls.add(new Ball(7, false, Color.RED,
                leadBallX - Ball.DIMENSION / 2,
                (int) (middleBall.getY() - Ball.DIMENSION)));
        this.balls.add(new Ball(8, false, Color.RED,
                leadBallX + Ball.DIMENSION / 2,
                (int) (middleBall.getY() - Ball.DIMENSION)));

        this.balls.add(new Ball(9, false, Color.RED,
                leadBallX,
                (int) (middleBall.getY() - Ball.DIMENSION * 2)));

        whiteBall = new Ball(0, true, Color.WHITE,
                (int) (table.getX() + table.getWidth() / 2),
                (int) (table.getY() + table.getHeight() - table.getHeight() / 4));
        this.balls.add(whiteBall);
    }

    public void integrate(double t, double deltaTime) {
        gameMechanic.integrate(t, deltaTime);
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            if (ball.isInPocket()) {
                it.remove();
                if (ball.isWhite()) {
                    gameEnded = true;
                }
            }
        }
        if (balls.size() == 1) {
            gameEnded = true;
        }
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (table != null) {
            table.draw(g);
        }

        for (DrawableInterface item : balls) {
            item.draw(g);
        }

        if (stick != null) {
            stick.draw(g);
        }
    }
}
