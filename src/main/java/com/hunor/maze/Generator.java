package com.hunor.maze;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedList;


public class Generator extends Application {
    private Stage stage;
    private AnimationTimer builder, solver;
    private GraphicsContext gc;
    private EventHandler<KeyEvent> startSolve, rebuildMaze, skipEvent, fpsEvent;

    private int rows, cols;
    private double s = 20, fps = 15;
    private boolean building, solving;

    private ArrayList<Cell> grid = new ArrayList<>();
    private LinkedList<Cell> buildWay = new LinkedList<>();
    private LinkedList<Cell> solveWay = new LinkedList<>();
    private Cell current;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
            //<editor-fold desc="Events">
            skipEvent   = keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    if (building) {
                        builder.stop();
                        while (!buildWay.isEmpty()) {
                            buildUpdate();
                        }
                        buildUpdate();
                        paint();
                        building = false;
                        stage.addEventHandler(KeyEvent.KEY_PRESSED, startSolve);
                        stage.addEventHandler(KeyEvent.KEY_PRESSED, rebuildMaze);
                    }
                    else if (solving) {
                        solver.stop();
                        while (!(current.x == cols-1 && current.y == rows-1)) {
                            solveUpdate();
                        }
                        paint();
                        solving = false;
                        stage.addEventHandler(KeyEvent.KEY_PRESSED, rebuildMaze);
                    }
                }
            };
            rebuildMaze = keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.R)) {
                    stage.removeEventHandler(KeyEvent.KEY_PRESSED, startSolve);
                    stage.addEventHandler(KeyEvent.KEY_PRESSED, skipEvent);

                    builder.stop();

                    solveWay.clear();
                    grid.clear();
                    for (int y = 0; y < rows; y++) {
                        for (int x = 0; x < cols; x++) {
                            grid.add(new Cell(x, y));
                        }
                    }

                    current = grid.get(0);
                    building = true;
                    builder.start();
                }
            };
            startSolve  = keyEvent -> {
                if (keyEvent.getCode() == KeyCode.SPACE) {
                    stage.removeEventHandler(KeyEvent.KEY_PRESSED, startSolve);
                    stage.removeEventHandler(KeyEvent.KEY_PRESSED, rebuildMaze);
                    stage.addEventHandler(KeyEvent.KEY_PRESSED, skipEvent);
                    solving = true;
                    solver.start();
                }
            };
            fpsEvent = keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.RIGHT)) {
                    if (fps < 60)
                        fps += 2;
                    else if (fps < 3600) fps *= 2;
                }
                if (keyEvent.getCode().equals(KeyCode.LEFT)) {
                    if (fps > 2)
                        if (fps <= 60)
                            fps -= 2;
                        else fps /= 2;
                }
            };
            //</editor-fold>

            //<editor-fold desc=".....">
            this.stage = stage;
            Group root = new Group();
            Canvas canvas = new Canvas(1200, 740);
            root.getChildren().add(canvas);
            gc = canvas.getGraphicsContext2D();
            gc.setLineCap(StrokeLineCap.ROUND);
            //</editor-fold>

            //<editor-fold desc="Timers">
            builder = new AnimationTimer() {
                long lastTime = System.nanoTime();
                double nsPerTick = 1000000000 / fps;

                @Override
                public void handle(long curr) {
                    if (curr - lastTime >= nsPerTick) {
                        lastTime = curr;
                        nsPerTick = 1000000000 / fps;

                        buildUpdate();
                        for (int i = 1; i < fps / 60; i++) {
                            buildUpdate();
                        }
                        paint();
                    }
                }
            };
            solver = new AnimationTimer() {
                long lastTime = System.nanoTime();
                double nsPerTick = 1000000000 / fps;

                @Override
                public void handle(long curr) {
                    if (curr - lastTime >= nsPerTick) {
                        lastTime = curr;
                        nsPerTick = 1000000000 / fps;

                        if (!(current.x == cols-1 && current.y == rows-1)) {
                            solveUpdate();
                            for (int i = 1; i < fps/60 && !(current.x == cols-1 && current.y == rows-1); i++) {
                                solveUpdate();
                            }
                            paint();
                        }
                        else {
                            paint();
                            solving = false;
                            solver.stop();
                            stage.removeEventHandler(KeyEvent.KEY_PRESSED, skipEvent);
                            stage.addEventHandler(KeyEvent.KEY_PRESSED, rebuildMaze);
                        }
                    }
                }
            };
            //</editor-fold>

            //<editor-fold desc="Setup">
            rows = (int) (canvas.getHeight()/s);
            cols = (int) (canvas.getWidth()/s);

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    Cell cell = new Cell(x, y);
                    grid.add(cell);
                }
            }
            grid.get(0).walls[0] = false;
            grid.get(0).walls[3] = false;
            grid.get(grid.size()-1).walls[1] = false;
            grid.get(grid.size()-1).walls[2] = false;

            current = grid.get(0);
            //</editor-fold>

            //<editor-fold desc=".....">
            stage.setScene(new Scene(root, canvas.getWidth(), canvas.getHeight()));
            stage.setResizable(false);
            stage.setTitle("Labirintus generáló - megoldó");
            stage.addEventHandler(KeyEvent.KEY_PRESSED, fpsEvent);
            stage.addEventHandler(KeyEvent.KEY_PRESSED, skipEvent);
            stage.addEventHandler(KeyEvent.KEY_PRESSED, rebuildMaze);
            stage.show();
            stage.centerOnScreen();

            building = true;
            builder.start();
            //</editor-fold>
    }

    void solveUpdate() {
        Cell next = current.checkSolveNeighbors();
        if (next != null) {
            solveWay.addLast(current);

            current = next;
        }
        else {
            if (!solveWay.isEmpty() && current.checkSolveNeighbors() == null) {
                current.freeToMove = false;

                grid.set(index(current.x, current.y), current);
                current = solveWay.removeLast();
            }

            paint();
        }
    }

    void buildUpdate() {
        current.visited = true;

        Cell next = current.checkNeighbors();
        if (next != null && !(current.x == cols-1 && current.y == rows-1)) {
            next.visited = true;
            grid.set(index(next.x, next.y), next);

            buildWay.addLast(current);


            removeWallsBetween(current, next);

            current = next;
        }
        else if (!buildWay.isEmpty())
            current = buildWay.removeLast();
        else {
            builder.stop();
            stage.addEventHandler(KeyEvent.KEY_PRESSED, startSolve);
            stage.addEventHandler(KeyEvent.KEY_PRESSED, rebuildMaze);
        }
    }

    void paint() {
        //<editor-fold desc="Background">
        gc.setFill(Color.rgb(75, 75, 75));
        gc.fillRect(0, 0, cols*s, rows*s);
        //</editor-fold>

        grid.forEach(Cell::show);

        if (solving && !solveWay.isEmpty()) {
            gc.setLineWidth(s/7);
            gc.setStroke(Color.rgb(255, 255, 0));
            for (int i = 1; i < solveWay.size(); i++) {
                Cell prev = solveWay.get(i-1);
                Cell next = solveWay.get(i);

                gc.strokeLine(prev.x*s + s/2, prev.y * s + s/2, next.x*s + s/2, next.y*s + s/2);
            }
            gc.strokeLine(solveWay.getLast().x*s + s/2, solveWay.getLast().y*s + s/2, current.x*s +s/2, current.y*s + s/2);
        }

        current.highlight();
    }

    void removeWallsBetween(Cell current, Cell next) {
        int dx = current.x - next.x;
        if (dx == -1) {
            current.walls[1] = false;
            next.walls[3] = false;
        } else if(dx == 1) {
            current.walls[3] = false;
            next.walls[1] = false;
        }

        int dy = current.y - next.y;
        if (dy == -1) {
            current.walls[2] = false;
            next.walls[0] = false;
        } else if(dy == 1) {
            current.walls[0] = false;
            next.walls[2] = false;
        }
    }

    boolean wallsFreeBetween(Cell current, Cell next) {
        int dx = current.x - next.x;
        if (dx == -1) {
            return !current.walls[1] && !next.walls[3];
        } else if(dx == 1) {
            return !current.walls[3] && !next.walls[1];
        }

        int dy = current.y - next.y;
        if (dy == -1) {
            return !current.walls[2] && !next.walls[0];
        } else if(dy == 1) {
            return !current.walls[0] && !next.walls[2];
        }
        return false;
    }

    class Cell {
        int x, y;
        boolean[] walls;
        boolean visited;
        boolean freeToMove;

        Cell(int x, int y) {
            this.x = x;
            this.y = y;
            walls = new boolean[]{true, true, true, true};
            visited = false;
            freeToMove = true;
        }

        void show() {
            int realX = (int) (x*s);
            int realY = (int) (y*s);

            if (this.visited) {
                gc.setFill(Color.rgb(125, 125, 150));
                gc.fillRect(realX, realY, s, s);

                gc.setLineWidth(s/10);
                gc.setStroke(Color.rgb(215, 215, 235));
                if (walls[0])
                    gc.strokeLine(realX    , realY    , realX + s, realY);
                if (walls[1])
                    gc.strokeLine(realX + s, realY    , realX + s, realY + s);
                if (walls[2])
                    gc.strokeLine(realX + s, realY + s, realX    , realY + s);
                if (walls[3])
                    gc.strokeLine(realX    , realY + s, realX    , realY);
            }

            if (!this.freeToMove) {
                gc.setFill(Color.rgb(255, 0, 0, 0.6));
                gc.fillRect(realX, realY, s, s);
            }
        }

        void highlight() {
            int realX = (int) (x*s);
            int realY = (int) (y*s);

            gc.setFill(Color.rgb(255, 110, 0));
            gc.fillRoundRect(realX, realY, s, s, s/3, s/3);
        }

        Cell checkNeighbors() {
            ArrayList<Cell> neighbors = new ArrayList<>();

            Cell top = null, right = null, bottom = null, left = null;

            if (index(x  , y-1) >= 0)
                top    = grid.get(index(x  , y-1));
            if (index(x+1, y) >= 0)
                right  = grid.get(index(x+1, y));
            if (index(x  , y+1) >= 0)
                bottom = grid.get(index(x  , y+1));
            if (index(x-1, y) >= 0)
                left   = grid.get(index(x-1, y));

            if (top != null && !top.visited)
                neighbors.add(top);
            if (right != null && !right.visited)
                neighbors.add(right);
            if (bottom != null && !bottom.visited)
                neighbors.add(bottom);
            if (left != null && !left.visited)
                neighbors.add(left);

            if (!neighbors.isEmpty()) {
                int randI = (int) (Math.random() * neighbors.size());
                return neighbors.get(randI);
            } else return null;
        }

        Cell checkSolveNeighbors() {
            ArrayList<Cell> neighbors = new ArrayList<>();

            Cell top = null, right = null, bottom = null, left = null;

            if (index(x  , y-1) >= 0)
                top    = grid.get(index(x  , y-1));
            if (index(x+1, y) >= 0)
                right  = grid.get(index(x+1, y));
            if (index(x  , y+1) >= 0)
                bottom = grid.get(index(x  , y+1));
            if (index(x-1, y) >= 0)
                left   = grid.get(index(x-1, y));


            if (top != null && wallsFreeBetween(current, top) && !solveWay.contains(top) && top.freeToMove)
                neighbors.add(top);
            if (right != null && wallsFreeBetween(current, right) && !solveWay.contains(right) && right.freeToMove)
                neighbors.add(right);
            if (bottom != null && wallsFreeBetween(current, bottom) && !solveWay.contains(bottom) && bottom.freeToMove)
                neighbors.add(bottom);
            if (left != null && wallsFreeBetween(current, left) && !solveWay.contains(left) && left.freeToMove)
                neighbors.add(left);

            if (!neighbors.isEmpty()) {
                int randI = (int) (Math.random() * neighbors.size());
                return neighbors.get(randI);
            } else return null;
        }

        boolean hasNotBranching() {
            Cell top = null, right = null, bottom = null, left = null;

            if (index(x  , y-1) >= 0)
                top    = grid.get(index(x  , y-1));
            if (index(x+1, y) >= 0)
                right  = grid.get(index(x+1, y));
            if (index(x  , y+1) >= 0)
                bottom = grid.get(index(x  , y+1));
            if (index(x-1, y) >= 0)
                left   = grid.get(index(x-1, y));

            if (top != null) {
                if (!wallsFreeBetween(this, top) || solveWay.contains(top) || !top.freeToMove)
                    top = null;
            }
            if (right != null) {
                if (!wallsFreeBetween(this, right) || solveWay.contains(right) || !right.freeToMove)
                    right = null;
            }
            if (bottom != null) {
                if (!wallsFreeBetween(this, bottom) || solveWay.contains(bottom) || !bottom.freeToMove)
                    bottom = null;
            }
            if (left != null) {
                if (!wallsFreeBetween(this, left) || solveWay.contains(left) || !left.freeToMove)
                    left = null;
            }

            return top == null && right == null && bottom == null && left == null;
        }
    }

    int index(int x, int y) {
        if (x < 0 || y < 0 || x > cols-1 || y > rows-1)
            return -1;

        else return x + y * cols;
    }

}
