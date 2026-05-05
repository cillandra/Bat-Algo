import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class BatAlgorithmDashboard extends JFrame {

    static final int GRID_SIZE = 25;
    static final int WAYPOINTS = 8;

    static final double COLLISION_PENALTY = 1000.0;
    static final double PATH_WEIGHT = 1.0;
    static final double SAFETY_WEIGHT = 1.0;
    static final double SMOOTHNESS_WEIGHT = 0.10;

    final boolean[][] obstacles = new boolean[GRID_SIZE][GRID_SIZE];

    final Point start = new Point(1, 1);
    final Point goal = new Point(23, 23);

    PathResult baselineResult;
    PathResult improvedResult;

    final MapPanel mapPanel = new MapPanel();
    final MetricPanel metricPanel = new MetricPanel();
    final ResultPanel resultPanel = new ResultPanel();
    final JTextArea logArea = new JTextArea();

    final JSpinner populationInput = new JSpinner(new SpinnerNumberModel(50, 10, 200, 5));
    final JSpinner iterationInput = new JSpinner(new SpinnerNumberModel(100, 20, 500, 10));

    final Random random = new Random();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BatAlgorithmDashboard app = new BatAlgorithmDashboard();
            app.setVisible(true);
        });
    }

    public BatAlgorithmDashboard() {
        setTitle("Bat Algorithm Path Planning Simulator");
        setSize(1200, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createDefaultMap();

        add(createHeader(), BorderLayout.NORTH);
        add(createMainLayout(), BorderLayout.CENTER);
        add(createLogPanel(), BorderLayout.SOUTH);
    }

    JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(25, 35, 50));
        header.setBorder(new EmptyBorder(16, 22, 16, 22));

        JLabel title = new JLabel("Bat Algorithm Path Planning Simulation Dashboard");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));

        JLabel subtitle = new JLabel("Comparison of Baseline Bat Algorithm and Improved Bat Algorithm for Mobile Robot Path Planning");
        subtitle.setForeground(new Color(210, 220, 235));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.WEST);
        return header;
    }

    JPanel createMainLayout() {
        JPanel main = new JPanel(new BorderLayout(14, 14));
        main.setBorder(new EmptyBorder(14, 14, 14, 14));
        main.setBackground(new Color(238, 242, 247));

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setOpaque(false);
        leftPanel.add(mapPanel, BorderLayout.CENTER);
        leftPanel.add(createControlPanel(), BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 12, 12));
        rightPanel.setPreferredSize(new Dimension(430, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(resultPanel);
        rightPanel.add(metricPanel);

        main.add(leftPanel, BorderLayout.CENTER);
        main.add(rightPanel, BorderLayout.EAST);

        return main;
    }

    JPanel createControlPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel title = new JLabel("Experiment Controls");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controls.setOpaque(false);

        JButton runButton = createButton("Run Simulation", new Color(35, 120, 230));
        JButton randomButton = createButton("Randomize Obstacles", new Color(95, 110, 130));
        JButton resetButton = createButton("Reset Map", new Color(95, 110, 130));
        JButton clearButton = createButton("Clear Results", new Color(160, 80, 80));

        runButton.addActionListener(e -> runSimulation());
        randomButton.addActionListener(e -> {
            createRandomMap();
            clearResults();
            addLog("New random obstacle map generated.");
        });
        resetButton.addActionListener(e -> {
            createDefaultMap();
            clearResults();
            addLog("Map reset to default layout.");
        });
        clearButton.addActionListener(e -> {
            clearResults();
            addLog("Results cleared.");
        });

        controls.add(new JLabel("Population:"));
        controls.add(populationInput);
        controls.add(new JLabel("Iterations:"));
        controls.add(iterationInput);
        controls.add(runButton);
        controls.add(randomButton);
        controls.add(resetButton);
        controls.add(clearButton);

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(controls, BorderLayout.CENTER);

        return wrapper;
    }

    JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        return button;
    }

    JScrollPane createLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setRows(5);
        logArea.setBackground(new Color(250, 252, 255));
        logArea.setText("Simulation notes will appear here.\n");

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Experiment Notes"));
        return scrollPane;
    }

    void createDefaultMap() {
        for (boolean[] row : obstacles) {
            Arrays.fill(row, false);
        }

        for (int y = 4; y <= 19; y++) obstacles[7][y] = true;
        for (int x = 7; x <= 18; x++) obstacles[x][10] = true;
        for (int y = 3; y <= 14; y++) obstacles[16][y] = true;
        for (int x = 3; x <= 12; x++) obstacles[x][18] = true;
        for (int x = 13; x <= 21; x++) obstacles[x][5] = true;

        obstacles[7][8] = false;
        obstacles[7][9] = false;
        obstacles[12][10] = false;
        obstacles[13][10] = false;
        obstacles[16][11] = false;
        obstacles[16][12] = false;
        obstacles[9][18] = false;
        obstacles[10][18] = false;
        obstacles[18][5] = false;
        obstacles[19][5] = false;

        obstacles[start.x][start.y] = false;
        obstacles[goal.x][goal.y] = false;
    }

    void createRandomMap() {
        for (boolean[] row : obstacles) {
            Arrays.fill(row, false);
        }

        for (int i = 0; i < 115; i++) {
            int x = random.nextInt(GRID_SIZE);
            int y = random.nextInt(GRID_SIZE);

            boolean nearStart = Math.abs(x - start.x) + Math.abs(y - start.y) < 4;
            boolean nearGoal = Math.abs(x - goal.x) + Math.abs(y - goal.y) < 4;

            if (!nearStart && !nearGoal) {
                obstacles[x][y] = true;
            }
        }

        obstacles[start.x][start.y] = false;
        obstacles[goal.x][goal.y] = false;
    }

    void runSimulation() {
        int population = (Integer) populationInput.getValue();
        int iterations = (Integer) iterationInput.getValue();

        long seed = System.nanoTime();

        baselineResult = runBatAlgorithm(false, population, iterations, seed);
        improvedResult = runBatAlgorithm(true, population, iterations, seed + 999);

        mapPanel.repaint();
        resultPanel.repaint();
        metricPanel.repaint();

        addLog("Simulation completed.");
        addLog("Baseline BA  -> Length: " + format(baselineResult.length)
                + ", Fitness: " + format(baselineResult.fitness)
                + ", Turns: " + baselineResult.turns
                + ", Collisions: " + baselineResult.collisions);

        addLog("Improved BA  -> Length: " + format(improvedResult.length)
                + ", Fitness: " + format(improvedResult.fitness)
                + ", Turns: " + improvedResult.turns
                + ", Collisions: " + improvedResult.collisions);

        if (improvedResult.fitness < baselineResult.fitness) {
            addLog("Interpretation: The Improved Bat Algorithm produced a better path in this simulation.");
        } else {
            addLog("Interpretation: The Baseline Bat Algorithm performed better in this particular run.");
        }
    }

    void clearResults() {
        baselineResult = null;
        improvedResult = null;
        mapPanel.repaint();
        resultPanel.repaint();
        metricPanel.repaint();
    }

    PathResult runBatAlgorithm(boolean improved, int population, int maxIterations, long seed) {
        Random rnd = new Random(seed);
        Bat[] bats = new Bat[population];

        for (int i = 0; i < population; i++) {
            bats[i] = createRandomBat(rnd);
            evaluateBat(bats[i]);
        }

        Bat best = bats[0].copy();

        for (Bat bat : bats) {
            if (bat.fitness < best.fitness) {
                best = bat.copy();
            }
        }

        double frequencyMin = 0.0;
        double frequencyMax = 2.0;
        double loudnessDecay = 0.90;
        double pulseGrowth = 0.90;

        double inertiaMax = 0.90;
        double inertiaMin = 0.35;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            for (Bat bat : bats) {
                double frequency = frequencyMin + (frequencyMax - frequencyMin) * rnd.nextDouble();

                for (int d = 0; d < bat.position.length; d++) {
                    double inertia = 1.0;

                    if (improved) {
                        double progress = Math.log(1.0 + iteration) / Math.log(1.0 + maxIterations);
                        inertia = inertiaMax - (inertiaMax - inertiaMin) * progress;
                        inertia += 0.03 * rnd.nextGaussian();
                    }

                    bat.velocity[d] = inertia * bat.velocity[d] + (best.position[d] - bat.position[d]) * frequency;
                    bat.position[d] = clamp(bat.position[d] + bat.velocity[d], 0, GRID_SIZE - 1);
                }

                if (rnd.nextDouble() > bat.pulseRate) {
                    for (int d = 0; d < bat.position.length; d++) {
                        double localSearch = (rnd.nextDouble() * 2 - 1) * bat.loudness;

                        if (improved) {
                            double cauchyDisturbance = Math.tan(Math.PI * (rnd.nextDouble() - 0.5));
                            double strength = 0.35 * (maxIterations - iteration) / Math.max(1.0, maxIterations);
                            localSearch += strength * cauchyDisturbance * rnd.nextDouble();
                        }

                        bat.position[d] = clamp(best.position[d] + localSearch, 0, GRID_SIZE - 1);
                    }
                }

                evaluateBat(bat);

                if (bat.fitness < best.fitness && rnd.nextDouble() < bat.loudness) {
                    best = bat.copy();
                    bat.loudness *= loudnessDecay;
                    bat.pulseRate = bat.initialPulseRate * (1 - Math.exp(-pulseGrowth * iteration));
                }
            }
        }

        return evaluateBat(best);
    }

    Bat createRandomBat(Random rnd) {
        Bat bat = new Bat(WAYPOINTS * 2);

        for (int i = 0; i < WAYPOINTS; i++) {
            double t = (i + 1.0) / (WAYPOINTS + 1.0);

            double x = start.x + t * (goal.x - start.x) + rnd.nextGaussian() * 4.0;
            double y = start.y + t * (goal.y - start.y) + rnd.nextGaussian() * 4.0;

            bat.position[i * 2] = clamp(x, 0, GRID_SIZE - 1);
            bat.position[i * 2 + 1] = clamp(y, 0, GRID_SIZE - 1);

            bat.velocity[i * 2] = rnd.nextGaussian() * 0.5;
            bat.velocity[i * 2 + 1] = rnd.nextGaussian() * 0.5;
        }

        bat.loudness = 0.90;
        bat.pulseRate = 0.20 + rnd.nextDouble() * 0.20;
        bat.initialPulseRate = bat.pulseRate;

        return bat;
    }

    PathResult evaluateBat(Bat bat) {
        List<Point> controlPoints = new ArrayList<>();
        controlPoints.add(new Point(start));

        for (int i = 0; i < WAYPOINTS; i++) {
            int x = (int) Math.round(clamp(bat.position[i * 2], 0, GRID_SIZE - 1));
            int y = (int) Math.round(clamp(bat.position[i * 2 + 1], 0, GRID_SIZE - 1));

            Point point = new Point(x, y);

            if (!point.equals(controlPoints.get(controlPoints.size() - 1))) {
                controlPoints.add(point);
            }
        }

        controlPoints.add(new Point(goal));

        List<Point> fullPath = new ArrayList<>();
        int collisions = 0;

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            List<Point> segment = createLine(controlPoints.get(i), controlPoints.get(i + 1));

            for (Point point : segment) {
                if (point.x < 0 || point.x >= GRID_SIZE || point.y < 0 || point.y >= GRID_SIZE || obstacles[point.x][point.y]) {
                    collisions++;
                }

                if (fullPath.isEmpty() || !fullPath.get(fullPath.size() - 1).equals(point)) {
                    fullPath.add(point);
                }
            }
        }

        double length = 0;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            length += fullPath.get(i).distance(fullPath.get(i + 1));
        }

        int turns = countTurns(fullPath);
        double smoothnessCost = turns * Math.PI / 4.0;

        double fitness =
                PATH_WEIGHT * length +
                SAFETY_WEIGHT * COLLISION_PENALTY * collisions +
                SMOOTHNESS_WEIGHT * smoothnessCost;

        bat.fitness = fitness;

        return new PathResult(length, fitness, turns, collisions, fullPath, controlPoints);
    }

    int countTurns(List<Point> path) {
        int turns = 0;

        for (int i = 1; i < path.size() - 1; i++) {
            Point previous = path.get(i - 1);
            Point current = path.get(i);
            Point next = path.get(i + 1);

            int dx1 = Integer.compare(current.x - previous.x, 0);
            int dy1 = Integer.compare(current.y - previous.y, 0);

            int dx2 = Integer.compare(next.x - current.x, 0);
            int dy2 = Integer.compare(next.y - current.y, 0);

            if (dx1 != dx2 || dy1 != dy2) {
                turns++;
            }
        }

        return turns;
    }

    List<Point> createLine(Point start, Point end) {
        List<Point> points = new ArrayList<>();

        int x0 = start.x;
        int y0 = start.y;
        int x1 = end.x;
        int y1 = end.y;

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int error = dx - dy;

        while (true) {
            points.add(new Point(x0, y0));

            if (x0 == x1 && y0 == y1) {
                break;
            }

            int error2 = 2 * error;

            if (error2 > -dy) {
                error -= dy;
                x0 += sx;
            }

            if (error2 < dx) {
                error += dx;
                y0 += sy;
            }
        }

        return points;
    }

    double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    void addLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    class MapPanel extends JPanel {
        MapPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 232)),
                    new EmptyBorder(12, 12, 12, 12)
            ));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int cell = getCellSize();
                    int offsetX = getOffsetX(cell);
                    int offsetY = getOffsetY(cell);

                    int x = (e.getX() - offsetX) / cell;
                    int y = (e.getY() - offsetY) / cell;

                    if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
                        if (!new Point(x, y).equals(start) && !new Point(x, y).equals(goal)) {
                            obstacles[x][y] = !obstacles[x][y];
                            clearResults();
                            addLog("Obstacle changed at coordinate (" + x + ", " + y + ").");
                        }
                    }
                }
            });
        }

        int getCellSize() {
            return Math.min((getWidth() - 70) / GRID_SIZE, (getHeight() - 95) / GRID_SIZE);
        }

        int getOffsetX(int cell) {
            return (getWidth() - cell * GRID_SIZE) / 2;
        }

        int getOffsetY(int cell) {
            return 50;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawTitle(g2);
            drawGrid(g2);
            drawPaths(g2);
            drawLegend(g2);
        }

        void drawTitle(Graphics2D g2) {
            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g2.drawString("Simulation Map", 18, 25);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(95, 105, 120));
            g2.drawString("Click on the grid to add or remove obstacles.", 18, 43);
        }

        void drawGrid(Graphics2D g2) {
            int cell = getCellSize();
            int offsetX = getOffsetX(cell);
            int offsetY = getOffsetY(cell);

            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    if (obstacles[x][y]) {
                        g2.setColor(new Color(45, 52, 65));
                    } else {
                        g2.setColor(new Color(246, 248, 252));
                    }

                    g2.fillRect(offsetX + x * cell, offsetY + y * cell, cell, cell);

                    g2.setColor(new Color(215, 222, 232));
                    g2.drawRect(offsetX + x * cell, offsetY + y * cell, cell, cell);
                }
            }

            drawPoint(g2, start, "S", new Color(40, 170, 100));
            drawPoint(g2, goal, "E", new Color(140, 85, 210));
        }

        void drawPoint(Graphics2D g2, Point point, String label, Color color) {
            int cell = getCellSize();
            int offsetX = getOffsetX(cell);
            int offsetY = getOffsetY(cell);

            int cx = offsetX + point.x * cell + cell / 2;
            int cy = offsetY + point.y * cell + cell / 2;

            g2.setColor(color);
            g2.fillOval(cx - cell / 3, cy - cell / 3, 2 * cell / 3, 2 * cell / 3);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(11, cell / 2)));

            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + fm.getAscent() / 3);
        }

        void drawPaths(Graphics2D g2) {
            if (baselineResult != null) {
                drawPath(g2, baselineResult.path, new Color(225, 80, 75), 3.0f);
            }

            if (improvedResult != null) {
                drawPath(g2, improvedResult.path, new Color(35, 125, 235), 4.0f);
                drawControlPoints(g2, improvedResult.controlPoints);
            }
        }

        void drawPath(Graphics2D g2, List<Point> path, Color color, float thickness) {
            if (path == null || path.size() < 2) return;

            int cell = getCellSize();
            int offsetX = getOffsetX(cell);
            int offsetY = getOffsetY(cell);

            Path2D line = new Path2D.Double();

            Point first = path.get(0);
            line.moveTo(offsetX + first.x * cell + cell / 2.0, offsetY + first.y * cell + cell / 2.0);

            for (Point point : path) {
                line.lineTo(offsetX + point.x * cell + cell / 2.0, offsetY + point.y * cell + cell / 2.0);
            }

            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            g2.draw(line);
        }

        void drawControlPoints(Graphics2D g2, List<Point> points) {
            if (points == null) return;

            int cell = getCellSize();
            int offsetX = getOffsetX(cell);
            int offsetY = getOffsetY(cell);

            g2.setColor(new Color(35, 125, 235));

            for (Point point : points) {
                int cx = offsetX + point.x * cell + cell / 2;
                int cy = offsetY + point.y * cell + cell / 2;
                g2.fillOval(cx - 3, cy - 3, 6, 6);
            }
        }

        void drawLegend(Graphics2D g2) {
            int y = getHeight() - 25;
            int x = 22;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            g2.setColor(new Color(225, 80, 75));
            g2.fillRoundRect(x, y - 8, 24, 6, 6, 6);
            g2.setColor(new Color(50, 60, 75));
            g2.drawString("Baseline BA", x + 32, y);

            x += 135;

            g2.setColor(new Color(35, 125, 235));
            g2.fillRoundRect(x, y - 8, 24, 6, 6, 6);
            g2.setColor(new Color(50, 60, 75));
            g2.drawString("Improved BA", x + 32, y);

            x += 145;

            g2.setColor(new Color(45, 52, 65));
            g2.fillRect(x, y - 13, 14, 14);
            g2.setColor(new Color(50, 60, 75));
            g2.drawString("Obstacle", x + 22, y);
        }
    }

    class ResultPanel extends JPanel {
        ResultPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 232)),
                    new EmptyBorder(16, 16, 16, 16)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g2.setColor(new Color(30, 40, 55));
            g2.drawString("Results Summary", 18, 28);

            if (baselineResult == null || improvedResult == null) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(new Color(100, 110, 125));
                g2.drawString("Run a simulation to compare both algorithms.", 18, 62);
                return;
            }

            drawResultCard(g2, "Baseline Bat Algorithm", baselineResult, 18, 50, new Color(225, 80, 75));
            drawResultCard(g2, "Improved Bat Algorithm", improvedResult, 18, 155, new Color(35, 125, 235));

            String verdict;

            if (improvedResult.fitness < baselineResult.fitness) {
                verdict = "Improved BA performed better overall.";
            } else {
                verdict = "Baseline BA performed better in this run.";
            }

            g2.setColor(new Color(245, 248, 252));
            g2.fillRoundRect(18, 265, getWidth() - 36, 48, 18, 18);

            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString("Interpretation:", 34, 287);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2.drawString(verdict, 125, 287);
        }

        void drawResultCard(Graphics2D g2, String title, PathResult result, int x, int y, Color color) {
            int width = getWidth() - 36;
            int height = 88;

            g2.setColor(new Color(248, 250, 253));
            g2.fillRoundRect(x, y, width, height, 18, 18);

            g2.setColor(color);
            g2.fillRoundRect(x, y, 8, height, 8, 8);

            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString(title, x + 18, y + 24);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(75, 85, 100));

            g2.drawString("Path Length: " + format(result.length), x + 18, y + 48);
            g2.drawString("Fitness Score: " + format(result.fitness), x + 170, y + 48);
            g2.drawString("Turns: " + result.turns, x + 18, y + 70);
            g2.drawString("Collisions: " + result.collisions, x + 170, y + 70);
        }
    }

    class MetricPanel extends JPanel {
        MetricPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 232)),
                    new EmptyBorder(16, 16, 16, 16)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g2.drawString("Metric Comparison", 18, 28);

            if (baselineResult == null || improvedResult == null) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(new Color(100, 110, 125));
                g2.drawString("Charts will appear after running the simulation.", 18, 62);
                return;
            }

            String[] labels = {"Length", "Fitness", "Turns", "Hits"};
            double[] baselineValues = {
                    baselineResult.length,
                    baselineResult.fitness,
                    baselineResult.turns,
                    baselineResult.collisions
            };
            double[] improvedValues = {
                    improvedResult.length,
                    improvedResult.fitness,
                    improvedResult.turns,
                    improvedResult.collisions
            };

            int chartX = 58;
            int chartY = 58;
            int chartW = getWidth() - 90;
            int chartH = getHeight() - 120;

            double maxValue = 1;

            for (double value : baselineValues) maxValue = Math.max(maxValue, value);
            for (double value : improvedValues) maxValue = Math.max(maxValue, value);

            g2.setColor(new Color(230, 235, 242));
            g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);

            int groupWidth = chartW / labels.length;
            int barWidth = Math.max(16, groupWidth / 5);

            for (int i = 0; i < labels.length; i++) {
                int baseX = chartX + i * groupWidth + 20;

                int baselineBar = (int) ((baselineValues[i] / maxValue) * chartH);
                int improvedBar = (int) ((improvedValues[i] / maxValue) * chartH);

                g2.setColor(new Color(225, 80, 75));
                g2.fillRoundRect(baseX, chartY + chartH - baselineBar, barWidth, baselineBar, 8, 8);

                g2.setColor(new Color(35, 125, 235));
                g2.fillRoundRect(baseX + barWidth + 8, chartY + chartH - improvedBar, barWidth, improvedBar, 8, 8);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(new Color(75, 85, 100));
                g2.drawString(labels[i], baseX - 5, chartY + chartH + 18);
            }

            drawSmallLegend(g2, chartX, getHeight() - 32);
        }

        void drawSmallLegend(Graphics2D g2, int x, int y) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            g2.setColor(new Color(225, 80, 75));
            g2.fillRoundRect(x, y - 8, 20, 6, 6, 6);
            g2.setColor(new Color(75, 85, 100));
            g2.drawString("Baseline BA", x + 28, y);

            g2.setColor(new Color(35, 125, 235));
            g2.fillRoundRect(x + 130, y - 8, 20, 6, 6, 6);
            g2.setColor(new Color(75, 85, 100));
            g2.drawString("Improved BA", x + 158, y);
        }
    }

    static class Bat {
        double[] position;
        double[] velocity;

        double loudness;
        double pulseRate;
        double initialPulseRate;
        double fitness = Double.POSITIVE_INFINITY;

        Bat(int dimensions) {
            position = new double[dimensions];
            velocity = new double[dimensions];
        }

        Bat copy() {
            Bat copy = new Bat(position.length);
            copy.position = position.clone();
            copy.velocity = velocity.clone();
            copy.loudness = loudness;
            copy.pulseRate = pulseRate;
            copy.initialPulseRate = initialPulseRate;
            copy.fitness = fitness;
            return copy;
        }
    }

    static class PathResult {
        double length;
        double fitness;
        int turns;
        int collisions;

        List<Point> path;
        List<Point> controlPoints;

        PathResult(double length, double fitness, int turns, int collisions, List<Point> path, List<Point> controlPoints) {
            this.length = length;
            this.fitness = fitness;
            this.turns = turns;
            this.collisions = collisions;
            this.path = path;
            this.controlPoints = controlPoints;
        }
    }
}