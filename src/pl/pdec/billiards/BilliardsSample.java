package pl.pdec.billiards;

import pl.pdec.billiards.components.Stick;
import pl.pdec.billiards.components.Table;
import pl.pdec.billiards.logic.StickControllerRunnable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BilliardsSample {

    public static void main(String... args) {
        int frameWidth = 800;
        int frameHeight = 800;

        JFrame frame = new JFrame("Sample Billiards");

        Game game = new Game();
        frame.add(game);

        double tableWidth = 450;
        double tableHeight = 600;
        Table table = new Table(
                (frameWidth - tableWidth) / 2,
                (frameHeight - tableHeight) / 2,
                tableWidth,
                tableHeight);
        game.setTable(table);

        Stick stick = new Stick();
        game.setStick(stick);

        GameResetActionListener gameResetListener = new GameResetActionListener(game, stick);
        gameResetListener.reset();

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menuMain = new JMenu("Billiards");
        menuBar.add(menuMain);

        JMenuItem menuItemRestart = new JMenuItem("Restart");
        menuItemRestart.addActionListener(gameResetListener);
        menuMain.add(menuItemRestart);

        JMenuItem menuItemExit = new JMenuItem("Exit");
        menuItemExit.addActionListener(e -> System.exit(0));
        menuMain.add(menuItemExit);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Color.BLACK);
        frame.setSize(frameWidth, frameHeight);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        GameRunnable gameRunnable = new GameRunnable(game);
        Thread gameThread = new Thread(gameRunnable);
        gameThread.start();
    }

    private static final class GameRunnable implements Runnable {
        private final Game game;

        private GameRunnable(Game game) {
            this.game = game;
        }

        @Override
        public void run() {
            double t = 0.0;
            double dt = 1 / 60.0;
            long currentTime = System.currentTimeMillis();
            long newTime = 0;
            long frameTime = 0;
            while (true) {
                if (!game.isGameEnded()) {
                    newTime = System.currentTimeMillis();
                    frameTime = newTime - currentTime;
                    currentTime = newTime;

                    while (frameTime > 0.0) {
                        double deltaTime = Math.min(frameTime, dt);
                        game.integrate(t, deltaTime);
                        frameTime -= deltaTime;
                        t += deltaTime;
                    }

                    game.repaint();

                    try {
                        Thread.sleep(1000/60 - (newTime - System.currentTimeMillis()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private static final class GameResetActionListener implements ActionListener {
        private Game game;
        private Stick stick;
        private StickControllerRunnable stickController = null;

        public GameResetActionListener(Game game, Stick stick) {
            this.game = game;
            this.stick = stick;
        }

        public void reset() {
            if (stickController != null) {
                stickController.shutdown();
            }

            game.reset();

            stickController = new StickControllerRunnable(game, stick, game.getWhiteBall());
            Thread stickThread = new Thread(stickController);
            stickThread.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            reset();
        }
    }
}
