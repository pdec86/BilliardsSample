package pl.pdec.billiards.logic;

import pl.pdec.billiards.components.Ball;
import pl.pdec.billiards.components.Table;
import pl.pdec.billiards.helpers.VectorCalc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GameMechanic implements Runnable {
    private final Table table;
    private final Collection<Ball> balls;
    private final CyclicBarrier barrier;
    private final Set<Worker> workers = new HashSet<>();
    private volatile double t = 0, deltaTime = 0;

    public GameMechanic(Table table, Collection<Ball> balls) {
        this.table = table;
        this.balls = balls;
        if (this.table == null) {
            throw new RuntimeException("Table object cannot be null");
        }
        if (this.balls == null) {
            throw new RuntimeException("Balls collection cannot be null");
        }

        barrier = new CyclicBarrier(balls.size(), this);
        for (Ball ball : balls) {
            Worker w = new Worker(ball);
            workers.add(w);
            new Thread(w).start();
        }
    }

    public void shutdown() {
        for (Worker worker : workers) {
            worker.shutdown();
        }
    }

    public void integrate(double t, double deltaTime) {
        synchronized (this) {
            this.t = t;
            this.deltaTime = deltaTime;
            notify();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class Worker implements Runnable {
        private final Ball ball;
        private volatile boolean shutdown = false;

        private Worker(Ball ball) {
            this.ball = ball;
            if (this.ball == null) {
                throw new RuntimeException("Ball object cannot be null");
            }
        }

        public void shutdown() {
            this.shutdown = true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Worker worker = (Worker) o;
            return ball.equals(worker.ball);
        }

        @Override
        public int hashCode() {
            return ball.hashCode();
        }

        @Override
        public void run() {
            while (!shutdown) {
                moveBall(t, deltaTime);

                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (BrokenBarrierException e) {
                    return;
                }
            }
        }

        private void moveBall(double t, double deltaTime) {
            if (ball.getVelocity() <= 0.01) {
                return;
            }
            double[] directionVector = ball.getDirection();
            double newX = ball.getX() + directionVector[0] * ball.getVelocity() * deltaTime;
            double newY = ball.getY() + directionVector[1] * ball.getVelocity() * deltaTime;
            double distanceToTravel = VectorCalc.distanceBetweenPoints(new double[]{newX, newY},
                    new double[]{ball.getX(), ball.getY()});

            if (!hitOtherBall(findBallToHit(newX, newY, distanceToTravel), newX, newY)) {
                double[] newPosition = hitTableBorder(newX, newY, distanceToTravel, 0);
                ball.setPosition(newPosition[0], newPosition[1]);
            }

            if (table.hitPocket(ball.getX(), ball.getY())) {
                ball.setIsInPocket();
            }

            double newVelocity = ball.getVelocity() - ball.getVelocity() * 0.9 * deltaTime;
            if (newVelocity <= 1) {
                ball.setVelocity(0);
            } else {
                ball.setVelocity(newVelocity);
            }
        }

        private Ball findBallToHit(double newX, double newY, double distanceToTravel) {
            // ball path line equation:
            // Ax + By + C = 0
            // y = m*x + q   =>  y = -A/Bx - C/B   =>   m = -A/B, q = -C/B
            double a = ball.getDirection()[1];
            double b = -ball.getDirection()[0];
            double c = ball.getY() * ball.getDirection()[0] - ball.getX() * ball.getDirection()[1];
            // distance from line to point:
            // distance(ax + by + c, (x0, y0)) = |ax0 + by0 + c| / sqrt(a^2 + b^2)
            double x0, y0;
            double x1, y1;
            double distance;
            double distanceTmp;
            double distanceBetweenBalls;

            for (Ball otherBall : balls) {
                if (!ball.equals(otherBall)) {
                    distanceBetweenBalls = VectorCalc.distanceBetweenPoints(new double[]{newX, newY},
                            new double[]{otherBall.getX(), otherBall.getY()});
                    if (distanceBetweenBalls <= Ball.DIMENSION) {
                        return otherBall;
                    } else {
                        double[] otherBallPosition = new double[]{otherBall.getX(), otherBall.getY()};
                        x0 = otherBall.getX();
                        y0 = otherBall.getY();
                        distance = Math.abs(a * x0 + b * y0 + c) / Math.sqrt(a * a + b * b);
                        if (distance < Ball.DIMENSION / 2.) {
                            for (distanceTmp = 0; distanceTmp < distanceToTravel; distanceTmp += 0.1) {
                                x1 = ball.getX() * distanceTmp * ball.getDirection()[0];
                                y1 = ball.getY() * distanceTmp * ball.getDirection()[1];
                                distanceBetweenBalls = VectorCalc.distanceBetweenPoints(new double[]{x1, y1},
                                        otherBallPosition);
                                if (distanceBetweenBalls <= Ball.DIMENSION) {
                                    return otherBall;
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }

        private boolean hitOtherBall(Ball ballToHit, double newX, double newY) {
            if (ballToHit != null) {
                double[] hitVector = new double[2];
                hitVector[0] = ball.getX() - ballToHit.getX();
                hitVector[1] = ball.getY() - ballToHit.getY();
                hitVector = VectorCalc.normalizeVector(hitVector);

                double[] reflectionVector = VectorCalc.reflectVector(ball.getDirection(), hitVector);
                reflectionVector = VectorCalc.normalizeVector(reflectionVector);
                ball.setDirection(reflectionVector);

                hitVector[0] = ballToHit.getX() - ball.getX();
                hitVector[1] = ballToHit.getY() - ball.getY();
                hitVector = VectorCalc.normalizeVector(hitVector);
                ballToHit.setDirection(hitVector);
                ballToHit.setVelocity(ball.getVelocity());
                return true;
            }

            return false;
        }

        private double[] hitTableBorder(double newX, double newY, double distanceToTravel, int loop) {
            if (loop > 10) {
                return new double[]{newX, newY};
            }
            /*  y1 = a*x1 + b
                y2 = a*x2 + b

                b = y1 - a*x1
                y2 = a*x2 + (y1 - a*x1)
                a = (y2 - y1) / (x2 - x1)
             */
            boolean doReflection = false;
            double aDirectionLine = (newY - ball.getY()) / (newX - ball.getX());
            double bDirectionLine = ball.getY() - aDirectionLine * ball.getX();
            double[] intersectionPoint = new double[2];
            double[] hitVector = new double[2];
            if (newX < table.getX()) {
                doReflection = true;

                intersectionPoint[0] = table.getX();
                intersectionPoint[1] = aDirectionLine * table.getX() + bDirectionLine;

                hitVector[0] = 1;
                hitVector[1] = 0;
            } else if (newX > table.getX() + table.getWidth()) {
                doReflection = true;

                double ballX = table.getX() + table.getWidth();
                intersectionPoint[0] = ballX;
                intersectionPoint[1] = aDirectionLine * ballX + bDirectionLine;

                hitVector[0] = -1;
                hitVector[1] = 0;
            } else if (newY < table.getY()) {
                doReflection = true;

                intersectionPoint[0] = (table.getY() - bDirectionLine) / aDirectionLine;
                intersectionPoint[1] = table.getY();

                hitVector[0] = 0;
                hitVector[1] = 1;
            } else if (newY > table.getY() + table.getHeight()) {
                doReflection = true;

                intersectionPoint[0] = (table.getY() + table.getHeight() - bDirectionLine) / aDirectionLine;
                intersectionPoint[1] = table.getY() + table.getHeight();

                hitVector[0] = 0;
                hitVector[1] = -1;
            }

            if (doReflection) {
                double distanceToBorder = VectorCalc.distanceBetweenPoints(intersectionPoint,
                        new double[]{ball.getX(), ball.getY()});
                double distanceAfterHitBorder = distanceToTravel - distanceToBorder;

                double[] reflectionVector = VectorCalc.reflectVector(ball.getDirection(), hitVector);
                reflectionVector = VectorCalc.normalizeVector(reflectionVector);

                newX = intersectionPoint[0] + distanceAfterHitBorder * reflectionVector[0];
                newY = intersectionPoint[1] + distanceAfterHitBorder * reflectionVector[1];

                ball.setDirection(reflectionVector);
                return hitTableBorder(newX, newY, distanceAfterHitBorder, loop + 1);
            }

            return new double[]{newX, newY};
        }
    }
}
