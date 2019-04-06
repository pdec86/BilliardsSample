package pl.pdec.billiards.logic;

import pl.pdec.billiards.components.Ball;
import pl.pdec.billiards.components.Table;
import pl.pdec.billiards.helpers.VectorCalc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GameMechanicMath implements Runnable {
    private final Table table;
    private final Collection<Ball> balls;
    private final CyclicBarrier barrier;
    private final Set<Worker> workers = new HashSet<>();
    private volatile double t = 0, deltaTime = 0;

    public GameMechanicMath(Table table, Collection<Ball> balls) {
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
            if (ball.getVelocity() == 0) {
                return;
            }
            double[] directionVector = ball.getDirection();
            double newX = ball.getX() + directionVector[0] * ball.getVelocity() * deltaTime;
            double newY = ball.getY() + directionVector[1] * ball.getVelocity() * deltaTime;
            double distanceToTravel = VectorCalc.distanceBetweenPoints(new double[]{newX, newY},
                    new double[]{ball.getX(), ball.getY()});

            if (!hitOtherBall(findBallToHit(), distanceToTravel)) {
                double[] newPosition = hitTableBorder(newX, newY, distanceToTravel, 0);
                ball.setPosition(newPosition[0], newPosition[1]);
            }

            double newVelocity = ball.getVelocity() - ball.getVelocity() * 0.9 * deltaTime;
            if (newVelocity <= 1) {
                ball.setVelocity(0);
            } else {
                ball.setVelocity(newVelocity);
            }
        }

        private Ball findBallToHit() {
            // ball path line equation:
            // Ax + By + C = 0
            // y = m*x + q   =>  y = -A/Bx - C/B   =>   m = -A/B, q = -C/B
            //
            // (x2 - x1)y + (y1 - y2)x + (x1 - x2)y1 + (y2 - y1)x1 = 0
//            double a = ball.getY() - newY;
//            double b = newX - ball.getX();
//            double c = (ball.getX() - newX) * ball.getY() + (newY - ball.getY()) * ball.getX();
            // x = x1 + v1*t
            // y = y1 + v2*t
            // (x - x1) / v1 = (y - y1) / v2
            // v2*x - x1*v2 = v1*y - y1*v1
            // v2*x - v1*y + y1*v1 - x1*v2 = 0
            double a = ball.getDirection()[1];
            double b = -ball.getDirection()[0];
            double c = ball.getY() * ball.getDirection()[0] - ball.getX() * ball.getDirection()[1];
            double m = -a / b;
            double q = -c / b;

            // distance from line to point:
            // distance(ax + by + c, (x0, y0)) = |ax0 + by0 + c| / sqrt(a^2 + b^2)
            //*
            Ball ballToHit = null;
            double x0 = 0;
            double y0 = 0;
            double distance = 0;
            double distanceBetweenBalls = 0;
            double distanceBetweenBallsMin = Integer.MAX_VALUE;
            for (Ball otherBall : balls) {
                if (!ball.equals(otherBall)) {
                    x0 = otherBall.getX();
                    y0 = otherBall.getY();
                    distance = Math.abs(a * x0 + b * y0 + c) / Math.sqrt(a * a + b * b);
                    if (distance <= Ball.DIMENSION) {
                        double x1 = 0, y1 = 0;
                        // ball equation: (x - ball.getX)^2 + (y - ball.getY)^2 = Ball.DIMENSION
                        // line equation: a*x + b*y + c = 0    y = mx + q
                        if (b == 0 && a != 0) {
                            x1 = -c / a;
                            // (x - getX)^2 + (y - getY)^2 - Ball.DIMENSION^2 = 0
                            // y^2 - 2*getY*y + getY^2 + (x - getX)^2 - Ball.DIMENSION^2 = 0;
                            double aEq = 1;
                            double bEq = -2 * otherBall.getY();
                            double cEq = otherBall.getY() * otherBall.getY() + (x1 - otherBall.getX())
                                    * (x1 - otherBall.getX()) - Ball.DIMENSION / 2. * Ball.DIMENSION / 2.;

                            double deltaEq = bEq * bEq - 4 * aEq * cEq;
                            if (deltaEq > 0) {
                                y1 = (-bEq - Math.sqrt(deltaEq)) / (2 * aEq);
                                double y2 = (-bEq + Math.sqrt(deltaEq)) / (2 * aEq);
                                double distanceY1 = distanceBetweenPoints(new double[]{ball.getX(), ball.getY()}, new double[]{x1, y1});
                                double distanceY2 = distanceBetweenPoints(new double[]{ball.getX(), ball.getY()}, new double[]{x1, y2});
                                if (distanceY2 < distanceY1) {
                                    y1 = y2;
                                }
                            } else if (deltaEq == 0) {
                                y1 = -bEq / (2 * aEq);
                            } else {
                                continue;
                            }
                        } else if (b != 0) {
                            // y = m*x + q
                            // (x - getX)^2 + ((-a/b*x - c/b) - getY)^2 = Ball.DIMENSION^2
                            // (x - getX)^2 + (m*x + q - getY)^2 - Ball.DIMENSION^2 = 0
                            // (m*x + (q - getY))^2   =>   m^2*x^2 + 2*m*(q - getY)*x + (q - getY)^2
                            // x^2 - 2*getX*x + getX^2 + m^2*x^2 + 2*m*(q - getY)*x + (q - getY)^2 - Ball.DIMENSION^2 = 0
                            double aEq = 1 + m * m;
                            double bEq = -2 * otherBall.getX() + 2 * m * (q - otherBall.getY());
                            double cEq = otherBall.getX() * otherBall.getX() + (q - otherBall.getY()) * (q - otherBall.getY()) - Ball.DIMENSION / 2. * Ball.DIMENSION / .2;

                            if (aEq != 0) {
                                double x2 = 0;
                                double y2;
                                double deltaEq = bEq * bEq - 4 * aEq * cEq;
                                if (deltaEq > 0) {
                                    x1 = (-bEq - Math.sqrt(deltaEq)) / (2 * aEq);
                                    x2 = (-bEq + Math.sqrt(deltaEq)) / (2 * aEq);
                                } else if (deltaEq == 0) {
                                    x1 = -bEq / (2 * aEq);
                                } else {
                                    continue;
                                }
                                y1 = m * x1 + q;
                                if (x2 != 0) {
                                    y2 = m * x2 + q;
                                    double distanceY1 = distanceBetweenPoints(new double[]{ball.getX(), ball.getY()},
                                            new double[]{x1, y1});
                                    double distanceY2 = distanceBetweenPoints(new double[]{ball.getX(), ball.getY()},
                                            new double[]{x2, y2});
                                    if (distanceY2 < distanceY1) {
                                        y1 = y2;
                                        x1 = x2;
                                    }
                                }
                            }
                        }

                        if (x1 != 0 && y1 != 0) {
                            double[] vectorIntersection = new double[]{x1 - ball.getX(), y1 - ball.getY()};
                            if (Math.abs(cross(ball.getDirection(), vectorIntersection)) < 0.01) {
                                distanceBetweenBalls = VectorCalc.distanceBetweenPoints(
                                        new double[]{otherBall.getX(), otherBall.getY()},
                                        new double[]{ball.getX(), ball.getY()});
                                if (distanceBetweenBalls < Ball.DIMENSION
                                        && distanceBetweenBalls <= distanceBetweenBallsMin) {
                                    distanceBetweenBallsMin = distanceBetweenBalls;
                                    ballToHit = otherBall;
                                }
                            }
                        }
                    }
                }
            }

            return ballToHit;
        }

        private boolean hitOtherBall(Ball ballToHit, double distanceToTravel) {
            if (ballToHit != null) {
                double distanceBetweenBalls = VectorCalc.distanceBetweenPoints(new double[]{ballToHit.getX(), ballToHit.getY()},
                        new double[]{ball.getX(), ball.getY()});
                if (distanceBetweenBalls - Ball.DIMENSION <= distanceToTravel) {
                    double[] hitVector = new double[2];
                    hitVector[0] = ball.getX() - ballToHit.getX();
                    hitVector[1] = ball.getY() - ballToHit.getY();
                    hitVector = normalizeVector(hitVector);

                    double[] reflectionVector = reflectVector(ball.getDirection(), hitVector);
                    reflectionVector = normalizeVector(reflectionVector);

                    hitVector[0] = ballToHit.getX() - ball.getX();
                    hitVector[1] = ballToHit.getY() - ball.getY();
                    hitVector = VectorCalc.normalizeVector(hitVector);
                    ballToHit.setDirection(hitVector);
                    ballToHit.setVelocity(ball.getVelocity());

                    double distanceToOtherBall = distanceToTravel - (distanceBetweenBalls - Ball.DIMENSION) - 1;
                    ball.setPosition(ball.getX() + distanceToOtherBall * ball.getDirection()[0],
                            ball.getY() + distanceToOtherBall * ball.getDirection()[1]);
                    ball.setDirection(reflectionVector);

                    double distanceLeft = distanceToTravel - distanceToOtherBall + 1;
                    ball.setPosition(ball.getX() + distanceLeft * ball.getDirection()[0],
                            ball.getY() + distanceLeft * ball.getDirection()[1]);
                    return true;
                }
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
                double distanceToBorder = Math.sqrt((intersectionPoint[0] - ball.getX()) * (intersectionPoint[0] - ball.getX())
                        + (intersectionPoint[1] - ball.getY()) * (intersectionPoint[1] - ball.getY()));
                double distanceAfterHitBorder = distanceToTravel - distanceToBorder;

                double[] reflectionVector = reflectVector(ball.getDirection(), hitVector);
                reflectionVector = normalizeVector(reflectionVector);

                newX = intersectionPoint[0] + distanceAfterHitBorder * reflectionVector[0];
                newY = intersectionPoint[1] + distanceAfterHitBorder * reflectionVector[1];

                ball.setDirection(reflectionVector);
                return hitTableBorder(newX, newY, distanceAfterHitBorder, loop + 1);
            }

            return new double[]{newX, newY};
        }

        private double[] subtractVector(double[] v1, double[] v2) {
            return new double[]{v1[0] - v2[0], v1[1] - v2[1]};
        }

        private double[] multiplyVector(double scalar, double[] v) {
            return new double[]{v[0] * scalar, v[1] * scalar};
        }

        private double[] normalizeVector(double[] vector) {
            double magnitude = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
            return new double[]{vector[0] / magnitude, vector[1] / magnitude};
        }

        private double[] reflectVector(double[] sourceVector, double[] surfaceVector) {
            return subtractVector(sourceVector, multiplyVector(2
                    * this.dot(sourceVector, surfaceVector), surfaceVector));
        }

        private double dot(double[] v1, double[] v2) {
            return v1[0] * v2[0] + v1[1] * v2[1];
        }

        private double cross(double[] v1, double[] v2) {
            return v1[0] * v2[1] - v1[1] * v2[0];
        }

        private double distanceBetweenPoints(double[] point1, double[] point2) {
            return Math.sqrt((point2[0] - point1[0]) * (point2[0] - point1[0])
                    + (point2[1] - point1[1]) * (point2[1] - point1[1]));
        }
    }
}
