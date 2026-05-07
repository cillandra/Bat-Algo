import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;

public class BatAlgorithmFinalDashboard extends JFrame {

    static final int WAYPOINTS = 8;

    static final double COLLISION_PENALTY = 1000.0;
    static final double PATH_WEIGHT = 1.0;
    static final double SAFETY_WEIGHT = 1.0;
    static final double SMOOTHNESS_WEIGHT = 0.10;

    int gridSize = 25;
    boolean[][] obstacles;

    Point start;
    Point goal;

    PathResult baselineResult;
    PathResult enhancedResult;

    final Random random = new Random();

    final MapPanel mapPanel = new MapPanel();
    final SummaryPanel summaryPanel = new SummaryPanel();

    final JSpinner populationInput = new JSpinner(new SpinnerNumberModel(50, 10, 200, 5));
    final JSpinner iterationInput = new JSpinner(new SpinnerNumberModel(100, 20, 500, 10));
    final JSpinner trialInput = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));

    final JComboBox<String> inputSizeBox = new JComboBox<>(new String[]{
            "Small - 15 x 15",
            "Medium - 25 x 25",
            "Large - 35 x 35"
    });

    final JTextArea notesArea = new JTextArea();
    final JTextArea winnerText = new JTextArea();

    final DefaultTableModel mainTableModel = new DefaultTableModel();
    final JTable mainTable = new JTable(mainTableModel);

    final DefaultTableModel baselineTableModel = new DefaultTableModel();
    final JTable baselineTable = new JTable(baselineTableModel);

    final DefaultTableModel enhancedTableModel = new DefaultTableModel();
    final JTable enhancedTable = new JTable(enhancedTableModel);

    final DefaultTableModel comparisonTableModel = new DefaultTableModel();
    final JTable comparisonTable = new JTable(comparisonTableModel);

    final LineChartPanel metricTimeChart = new LineChartPanel();
    final LineChartPanel metricMemoryChart = new LineChartPanel();
    final LineChartPanel metricOperationsChart = new LineChartPanel();
    final LineChartPanel scalabilityChart = new LineChartPanel();

    final LineChartPanel baselineTimeChart = new LineChartPanel();
    final LineChartPanel baselineMemoryChart = new LineChartPanel();
    final LineChartPanel baselineOperationsChart = new LineChartPanel();

    final LineChartPanel enhancedTimeChart = new LineChartPanel();
    final LineChartPanel enhancedMemoryChart = new LineChartPanel();
    final LineChartPanel enhancedOperationsChart = new LineChartPanel();

    final LineChartPanel runtimeComparisonChart = new LineChartPanel();
    final LineChartPanel memoryComparisonChart = new LineChartPanel();
    final LineChartPanel operationsComparisonChart = new LineChartPanel();

    final LineChartPanel analysisRuntimeChart = new LineChartPanel();
    final LineChartPanel analysisMemoryChart = new LineChartPanel();
    final LineChartPanel bestWorstChart = new LineChartPanel();

    final List<ResultRecord> experimentRecords = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BatAlgorithmFinalDashboard app = new BatAlgorithmFinalDashboard();
            app.setVisible(true);
        });
    }

    public BatAlgorithmFinalDashboard() {
        setTitle("Algorithm Comparison Simulator: Baseline BA vs Improved BA + DWA");
        setSize(1450, 860);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inputSizeBox.setSelectedIndex(1);
        initializeMap(25, false);

        setupTables();

        add(createHeader(), BorderLayout.NORTH);
        add(createMainArea(), BorderLayout.CENTER);
        add(createNotesPanel(), BorderLayout.SOUTH);

        addNote("Ready. Click Run Single Simulation for map view, or Run Full Experiment to generate all required tables and graphs.");
    }

    JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(22, 34, 52));
        panel.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("Bat Algorithm Performance Evaluation Dashboard");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));

        JLabel subtitle = new JLabel("Baseline BA vs Improved BA with DWA local planning and virtual-point path switching");
        subtitle.setForeground(new Color(205, 216, 232));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);

        panel.add(text, BorderLayout.WEST);
        return panel;
    }

    JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBackground(new Color(238, 242, 247));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);

        JSplitPane mapAndControlsSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                mapPanel,
                createControlPanel()
        );
        mapAndControlsSplit.setResizeWeight(0.84);
        mapAndControlsSplit.setOneTouchExpandable(true);
        mapAndControlsSplit.setContinuousLayout(true);
        mapAndControlsSplit.setDividerSize(10);

        left.add(mapAndControlsSplit, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("Dashboard", createDashboardTab());
        tabs.addTab("Metrics Used", createMetricsTab());
        tabs.addTab("Experimental Results", createExperimentalResultsTab());
        tabs.addTab("Result Tables and Graphs", createResultTablesAndGraphsTab());
        tabs.addTab("Comparative Analysis", createComparativeAnalysisTab());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);
        mainSplit.setResizeWeight(0.52);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setContinuousLayout(true);
        mainSplit.setDividerSize(12);

        SwingUtilities.invokeLater(() -> {
            mainSplit.setDividerLocation(0.52);
            mapAndControlsSplit.setDividerLocation(0.84);
        });

        main.add(mainSplit, BorderLayout.CENTER);
        return main;
    }

    JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 225, 235)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel label = new JLabel("Experiment Controls");
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(new Color(30, 40, 55));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.setOpaque(false);

        JButton runSingle = button("Run Single Simulation", new Color(35, 120, 230));
        JButton runFull = button("Run Full Experiment", new Color(35, 145, 105));
        JButton randomize = button("Randomize Obstacles", new Color(95, 110, 130));
        JButton reset = button("Reset Map", new Color(95, 110, 130));
        JButton clear = button("Clear Results", new Color(165, 80, 80));

        runSingle.addActionListener(e -> runSingleSimulation());
        runFull.addActionListener(e -> runFullExperiment());

        randomize.addActionListener(e -> {
            applySelectedInputSize(true);
            clearCurrentResults();
            addNote("Random obstacle map generated.");
        });

        reset.addActionListener(e -> {
            applySelectedInputSize(false);
            clearCurrentResults();
            addNote("Map reset.");
        });

        clear.addActionListener(e -> {
            clearAllResults();
            addNote("All results cleared.");
        });

        inputSizeBox.addActionListener(e -> {
            applySelectedInputSize(false);
            clearCurrentResults();
            addNote("Input size changed to " + inputSizeBox.getSelectedItem());
        });

        controls.add(new JLabel("Input size:"));
        controls.add(inputSizeBox);
        controls.add(new JLabel("Population:"));
        controls.add(populationInput);
        controls.add(new JLabel("Iterations:"));
        controls.add(iterationInput);
        controls.add(new JLabel("Trials:"));
        controls.add(trialInput);
        controls.add(runSingle);
        controls.add(runFull);
        controls.add(randomize);
        controls.add(reset);
        controls.add(clear);

        panel.add(label, BorderLayout.NORTH);
        panel.add(controls, BorderLayout.CENTER);
        return panel;
    }

    JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(8, 12, 8, 12));
        return b;
    }

    JScrollPane createNotesPanel() {
        notesArea.setRows(4);
        notesArea.setEditable(false);
        notesArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        notesArea.setBackground(new Color(250, 252, 255));

        JScrollPane scroll = new JScrollPane(notesArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Simulation Notes"));
        return scroll;
    }

    JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea guide = new JTextArea();
        guide.setEditable(false);
        guide.setLineWrap(true);
        guide.setWrapStyleWord(true);
        guide.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        guide.setText(
                "How to use this dashboard:\n\n" +
                "1. Use Run Single Simulation to show the actual robot path comparison on the map.\n" +
                "2. Use Run Full Experiment to generate all required tables and graphs for your paper.\n" +
                "3. Go to Metrics Used for separate graphs: execution time, memory usage, operations, and scalability.\n" +
                "4. Go to Experimental Results for separate Baseline BA and Improved BA + DWA results.\n" +
                "5. Go to Result Tables and Graphs for complete tables, runtime graph, memory graph, and operations graph.\n" +
                "6. Go to Comparative Analysis for runtime comparison, space comparison, best/worst cases, and winner identification.\n\n" +
                "You can drag the divider between the map and right-side results to resize them. You can also drag the divider between the map and controls."
        );

        JScrollPane guideScroll = new JScrollPane(guide);
        guideScroll.setBorder(BorderFactory.createTitledBorder("User Guide"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, summaryPanel, guideScroll);
        split.setResizeWeight(0.45);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(true);
        split.setDividerSize(10);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    JPanel createMetricsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel explanation = textCard(
                "Metrics Used",
                "This tab separates the required metrics into individual graphs:\n" +
                "• Execution time across input sizes\n" +
                "• Memory usage\n" +
                "• Number of comparisons / operations\n" +
                "• Scalability with input size\n\n" +
                "The improved side follows the study structure: global Improved BA, logarithmic velocity weighting, Cauchy disturbance, DWA-style local path planning, and virtual-point path switching.\n\n" +
                "Click Run Full Experiment to generate all graphs."
        );

        JPanel charts = new JPanel(new GridLayout(2, 2, 10, 10));
        charts.setBackground(Color.WHITE);
        charts.add(metricTimeChart);
        charts.add(metricMemoryChart);
        charts.add(metricOperationsChart);
        charts.add(scalabilityChart);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, explanation, chartScroll(charts));
        split.setResizeWeight(0.22);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(true);
        split.setDividerSize(10);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    JPanel createExperimentalResultsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane subTabs = new JTabbedPane();
        subTabs.addTab("Baseline Algorithm Results", createBaselineResultsPanel());
        subTabs.addTab("Improved BA + DWA Results vs Baseline", createEnhancedResultsPanel());

        panel.add(subTabs, BorderLayout.CENTER);
        return panel;
    }

    JPanel createBaselineResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);

        JPanel charts = new JPanel(new GridLayout(3, 1, 10, 10));
        charts.setBackground(Color.WHITE);
        charts.add(baselineTimeChart);
        charts.add(baselineMemoryChart);
        charts.add(baselineOperationsChart);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll(baselineTable), chartScroll(charts));
        split.setResizeWeight(0.35);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(true);
        split.setDividerSize(10);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    JPanel createEnhancedResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);

        JPanel charts = new JPanel(new GridLayout(3, 1, 10, 10));
        charts.setBackground(Color.WHITE);
        charts.add(enhancedTimeChart);
        charts.add(enhancedMemoryChart);
        charts.add(enhancedOperationsChart);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll(enhancedTable), chartScroll(charts));
        split.setResizeWeight(0.35);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(true);
        split.setDividerSize(10);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    JPanel createResultTablesAndGraphsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel charts = new JPanel(new GridLayout(3, 1, 10, 10));
        charts.setBackground(Color.WHITE);
        charts.add(runtimeComparisonChart);
        charts.add(memoryComparisonChart);
        charts.add(operationsComparisonChart);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll(mainTable), chartScroll(charts));
        split.setResizeWeight(0.38);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(true);
        split.setDividerSize(10);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    JPanel createComparativeAnalysisTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        winnerText.setEditable(false);
        winnerText.setLineWrap(true);
        winnerText.setWrapStyleWord(true);
        winnerText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        winnerText.setBackground(new Color(250, 252, 255));
        winnerText.setBorder(new EmptyBorder(12, 12, 12, 12));
        winnerText.setText("Run Full Experiment to generate comparative analysis.");

        JPanel charts = new JPanel(new GridLayout(3, 1, 10, 10));
        charts.setBackground(Color.WHITE);
        charts.add(analysisRuntimeChart);
        charts.add(analysisMemoryChart);
        charts.add(bestWorstChart);

        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll(comparisonTable), chartScroll(charts));
        topSplit.setResizeWeight(0.35);
        topSplit.setOneTouchExpandable(true);
        topSplit.setContinuousLayout(true);
        topSplit.setDividerSize(10);

        JSplitPane finalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, new JScrollPane(winnerText));
        finalSplit.setResizeWeight(0.78);
        finalSplit.setOneTouchExpandable(true);
        finalSplit.setContinuousLayout(true);
        finalSplit.setDividerSize(10);

        panel.add(finalSplit, BorderLayout.CENTER);
        return panel;
    }

    JPanel textCard(String title, String content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 225, 235)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(30, 40, 55));

        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBackground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(area, BorderLayout.CENTER);
        return panel;
    }

    JScrollPane tableScroll(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scroll = new JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scroll.setBorder(BorderFactory.createLineBorder(new Color(218, 225, 235)));
        return scroll;
    }

    JScrollPane chartScroll(JPanel charts) {
        JScrollPane scroll = new JScrollPane(
                charts,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(BorderFactory.createLineBorder(new Color(218, 225, 235)));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    void setupTables() {
        String[] columns = {
                "Input Size",
                "Algorithm",
                "Case",
                "Execution Time (ms)",
                "Memory Usage (KB)",
                "Operations",
                "Path Length",
                "Fitness Score",
                "Turns",
                "Collisions"
        };

        mainTableModel.setColumnIdentifiers(columns);
        baselineTableModel.setColumnIdentifiers(columns);
        enhancedTableModel.setColumnIdentifiers(columns);

        comparisonTableModel.setColumnIdentifiers(new String[]{
                "Input Size",
                "Comparison Area",
                "Better Algorithm",
                "Baseline Value",
                "Enhanced Value",
                "Interpretation"
        });

        setColumnWidths(mainTable);
        setColumnWidths(baselineTable);
        setColumnWidths(enhancedTable);
        setComparisonColumnWidths(comparisonTable);
    }

    void setColumnWidths(JTable table) {
        int[] widths = {120, 150, 100, 160, 160, 120, 120, 130, 80, 100};

        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
        }
    }

    void setComparisonColumnWidths(JTable table) {
        int[] widths = {120, 170, 150, 150, 150, 360};

        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
        }
    }

    void applySelectedInputSize(boolean randomMap) {
        int selected = inputSizeBox.getSelectedIndex();

        if (selected == 0) initializeMap(15, randomMap);
        else if (selected == 1) initializeMap(25, randomMap);
        else initializeMap(35, randomMap);
    }

    void initializeMap(int size, boolean randomMap) {
        gridSize = size;
        obstacles = new boolean[gridSize][gridSize];
        start = new Point(1, 1);
        goal = new Point(gridSize - 2, gridSize - 2);

        if (randomMap) createRandomObstacles();
        else createDefaultObstacles();

        obstacles[start.x][start.y] = false;
        obstacles[goal.x][goal.y] = false;

        repaint();
    }

    void createDefaultObstacles() {
        for (boolean[] row : obstacles) Arrays.fill(row, false);

        int a = Math.max(3, gridSize / 4);
        int b = Math.max(5, gridSize / 2);
        int c = Math.max(7, (gridSize * 2) / 3);

        for (int y = 3; y < gridSize - 4; y++) obstacles[a][y] = true;
        for (int x = a; x < gridSize - 5; x++) obstacles[x][b] = true;
        for (int y = 2; y < gridSize - 7; y++) obstacles[c][y] = true;
        for (int x = 3; x < gridSize / 2; x++) obstacles[x][gridSize - 7] = true;

        openCell(a, b - 1);
        openCell(a, b);
        openCell(a + 1, b);
        openCell(c, b + 1);
        openCell(c, b + 2);
        openCell(gridSize / 2, gridSize - 7);
    }

    void createRandomObstacles() {
        for (boolean[] row : obstacles) Arrays.fill(row, false);

        int count = (int) (gridSize * gridSize * 0.18);

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(gridSize);
            int y = random.nextInt(gridSize);

            boolean nearStart = Math.abs(x - start.x) + Math.abs(y - start.y) < 4;
            boolean nearGoal = Math.abs(x - goal.x) + Math.abs(y - goal.y) < 4;

            if (!nearStart && !nearGoal) obstacles[x][y] = true;
        }
    }

    void openCell(int x, int y) {
        if (x >= 0 && x < gridSize && y >= 0 && y < gridSize) {
            obstacles[x][y] = false;
        }
    }

    void runSingleSimulation() {
        int population = (Integer) populationInput.getValue();
        int iterations = (Integer) iterationInput.getValue();

        long seed = System.nanoTime();

        baselineResult = runMeasuredAlgorithm(false, population, iterations, seed);
        enhancedResult = runMeasuredAlgorithm(true, population, iterations, seed + 9999);

        addSingleRows();
        refreshAllChartsFromCurrentResults();

        mapPanel.repaint();
        summaryPanel.repaint();

        addNote("Single simulation completed.");
    }

    void runFullExperiment() {
        int originalSize = gridSize;
        boolean[][] originalMap = copyMap(obstacles);
        Point originalStart = new Point(start);
        Point originalGoal = new Point(goal);

        int population = (Integer) populationInput.getValue();
        int iterations = (Integer) iterationInput.getValue();
        int trials = (Integer) trialInput.getValue();

        experimentRecords.clear();
        clearTableModels();

        int[] sizes = {15, 25, 35};
        String[] names = {"Small 15x15", "Medium 25x25", "Large 35x35"};

        for (int s = 0; s < sizes.length; s++) {
            initializeMap(sizes[s], false);

            AverageResult baseAvg = new AverageResult();
            AverageResult enhancedAvg = new AverageResult();

            List<PathResult> baseTrialResults = new ArrayList<>();
            List<PathResult> enhancedTrialResults = new ArrayList<>();

            for (int t = 0; t < trials; t++) {
                long seed = 1000L + s * 9000L + t * 731L;

                PathResult base = runMeasuredAlgorithm(false, population, iterations, seed);
                PathResult enh = runMeasuredAlgorithm(true, population, iterations, seed + 9999);

                baseAvg.add(base);
                enhancedAvg.add(enh);

                baseTrialResults.add(base);
                enhancedTrialResults.add(enh);
            }

            ResultRecord baseAverage = baseAvg.toRecord(names[s], "Baseline BA", "Average", trials);
            ResultRecord enhancedAverage = enhancedAvg.toRecord(names[s], "Improved BA + DWA", "Average", trials);

            ResultRecord baseBest = toCaseRecord(names[s], "Baseline BA", "Best", bestByFitness(baseTrialResults));
            ResultRecord baseWorst = toCaseRecord(names[s], "Baseline BA", "Worst", worstByFitness(baseTrialResults));
            ResultRecord enhBest = toCaseRecord(names[s], "Improved BA + DWA", "Best", bestByFitness(enhancedTrialResults));
            ResultRecord enhWorst = toCaseRecord(names[s], "Improved BA + DWA", "Worst", worstByFitness(enhancedTrialResults));

            experimentRecords.add(baseAverage);
            experimentRecords.add(enhancedAverage);
            experimentRecords.add(baseBest);
            experimentRecords.add(enhBest);
            experimentRecords.add(baseWorst);
            experimentRecords.add(enhWorst);

            addRecordToMain(baseAverage);
            addRecordToMain(enhancedAverage);
            addRecordToMain(baseBest);
            addRecordToMain(enhBest);
            addRecordToMain(baseWorst);
            addRecordToMain(enhWorst);

            addRecordToSpecificTable(baselineTableModel, baseAverage);
            addRecordToSpecificTable(baselineTableModel, baseBest);
            addRecordToSpecificTable(baselineTableModel, baseWorst);

            addRecordToSpecificTable(enhancedTableModel, enhancedAverage);
            addRecordToSpecificTable(enhancedTableModel, enhBest);
            addRecordToSpecificTable(enhancedTableModel, enhWorst);

            baselineResult = baseTrialResults.get(baseTrialResults.size() - 1);
            enhancedResult = enhancedTrialResults.get(enhancedTrialResults.size() - 1);
        }

        gridSize = originalSize;
        obstacles = originalMap;
        start = originalStart;
        goal = originalGoal;

        buildComparisonRows();
        refreshAllChartsFromExperiment();
        buildWinnerText();

        mapPanel.repaint();
        summaryPanel.repaint();

        addNote("Full experiment completed. All tables and graphs are now generated.");
    }

    void addSingleRows() {
        clearTableModels();
        experimentRecords.clear();

        ResultRecord b = toCaseRecord("Current " + gridSize + "x" + gridSize, "Baseline BA", "Single Run", baselineResult);
        ResultRecord e = toCaseRecord("Current " + gridSize + "x" + gridSize, "Improved BA + DWA", "Single Run", enhancedResult);

        experimentRecords.add(b);
        experimentRecords.add(e);

        addRecordToMain(b);
        addRecordToMain(e);
        addRecordToSpecificTable(baselineTableModel, b);
        addRecordToSpecificTable(enhancedTableModel, e);

        buildComparisonRows();
        buildWinnerText();
    }

    void clearTableModels() {
        mainTableModel.setRowCount(0);
        baselineTableModel.setRowCount(0);
        enhancedTableModel.setRowCount(0);
        comparisonTableModel.setRowCount(0);
    }

    void clearCurrentResults() {
        baselineResult = null;
        enhancedResult = null;
        mapPanel.repaint();
        summaryPanel.repaint();
    }

    void clearAllResults() {
        clearCurrentResults();
        experimentRecords.clear();
        clearTableModels();
        clearAllCharts();
        winnerText.setText("Run Full Experiment to generate comparative analysis.");
    }

    void clearAllCharts() {
        metricTimeChart.clear();
        metricMemoryChart.clear();
        metricOperationsChart.clear();
        scalabilityChart.clear();

        baselineTimeChart.clear();
        baselineMemoryChart.clear();
        baselineOperationsChart.clear();

        enhancedTimeChart.clear();
        enhancedMemoryChart.clear();
        enhancedOperationsChart.clear();

        runtimeComparisonChart.clear();
        memoryComparisonChart.clear();
        operationsComparisonChart.clear();

        analysisRuntimeChart.clear();
        analysisMemoryChart.clear();
        bestWorstChart.clear();
    }

    void addRecordToMain(ResultRecord r) {
        mainTableModel.addRow(r.toRow());
    }

    void addRecordToSpecificTable(DefaultTableModel model, ResultRecord r) {
        model.addRow(r.toRow());
    }

    ResultRecord toCaseRecord(String inputSize, String algorithm, String caseName, PathResult r) {
        ResultRecord rec = new ResultRecord();
        rec.inputSize = inputSize;
        rec.algorithm = algorithm;
        rec.caseName = caseName;
        rec.time = r.executionTimeMs;
        rec.memory = r.memoryKb;
        rec.operations = r.operations;
        rec.length = r.length;
        rec.fitness = r.fitness;
        rec.turns = r.turns;
        rec.collisions = r.collisions;
        return rec;
    }

    PathResult bestByFitness(List<PathResult> list) {
        PathResult best = list.get(0);

        for (PathResult r : list) {
            if (r.fitness < best.fitness) best = r;
        }

        return best;
    }

    PathResult worstByFitness(List<PathResult> list) {
        PathResult worst = list.get(0);

        for (PathResult r : list) {
            if (r.fitness > worst.fitness) worst = r;
        }

        return worst;
    }

    boolean[][] copyMap(boolean[][] src) {
        boolean[][] copy = new boolean[src.length][src.length];

        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }

        return copy;
    }

    void buildComparisonRows() {
        comparisonTableModel.setRowCount(0);

        String[] sizes = uniqueAverageSizes();

        if (sizes.length == 0 && experimentRecords.size() >= 2) {
            ResultRecord base = findSingle("Baseline BA");
            ResultRecord enh = findSingle("Improved BA + DWA");

            if (base != null && enh != null) {
                addComparisonSet(base.inputSize, base, enh);
            }

            return;
        }

        for (String size : sizes) {
            ResultRecord base = findRecord(size, "Baseline BA", "Average");
            ResultRecord enh = findRecord(size, "Improved BA + DWA", "Average");

            if (base == null || enh == null) continue;

            addComparisonSet(size, base, enh);
        }
    }

    void addComparisonSet(String size, ResultRecord base, ResultRecord enh) {
        addComparisonRow(size, "Runtime comparison", base.time <= enh.time ? "Baseline BA" : "Improved BA + DWA",
                format(base.time) + " ms", format(enh.time) + " ms",
                "Lower execution time is better.");

        addComparisonRow(size, "Space usage comparison", base.memory <= enh.memory ? "Baseline BA" : "Improved BA + DWA",
                format(base.memory) + " KB", format(enh.memory) + " KB",
                "Lower memory usage is better.");

        addComparisonRow(size, "Operations comparison", base.operations <= enh.operations ? "Baseline BA" : "Improved BA + DWA",
                String.valueOf(Math.round(base.operations)), String.valueOf(Math.round(enh.operations)),
                "Fewer operations indicate lower computational work.");

        addComparisonRow(size, "Path quality / fitness", base.fitness <= enh.fitness ? "Baseline BA" : "Improved BA + DWA",
                format(base.fitness), format(enh.fitness),
                "Lower fitness means shorter, safer, smoother path.");
    }

    void addComparisonRow(String input, String area, String winner, String baseVal, String enhVal, String interpretation) {
        comparisonTableModel.addRow(new Object[]{
                input, area, winner, baseVal, enhVal, interpretation
        });
    }

    String[] uniqueAverageSizes() {
        LinkedHashSet<String> set = new LinkedHashSet<>();

        for (ResultRecord r : experimentRecords) {
            if (r.caseName.equals("Average")) set.add(r.inputSize);
        }

        return set.toArray(new String[0]);
    }

    ResultRecord findSingle(String algorithm) {
        for (ResultRecord r : experimentRecords) {
            if (r.algorithm.equals(algorithm) && r.caseName.equals("Single Run")) {
                return r;
            }
        }

        return null;
    }

    ResultRecord findRecord(String inputSize, String algorithm, String caseName) {
        for (ResultRecord r : experimentRecords) {
            if (r.inputSize.equals(inputSize) && r.algorithm.equals(algorithm) && r.caseName.equals(caseName)) {
                return r;
            }
        }

        return null;
    }

    void refreshAllChartsFromCurrentResults() {
        if (baselineResult == null || enhancedResult == null) return;

        String[] cats = {"Current"};

        LinkedHashMap<String, double[]> time = new LinkedHashMap<>();
        time.put("Baseline BA", new double[]{baselineResult.executionTimeMs});
        time.put("Improved BA + DWA", new double[]{enhancedResult.executionTimeMs});

        LinkedHashMap<String, double[]> mem = new LinkedHashMap<>();
        mem.put("Baseline BA", new double[]{baselineResult.memoryKb});
        mem.put("Improved BA + DWA", new double[]{enhancedResult.memoryKb});

        LinkedHashMap<String, double[]> ops = new LinkedHashMap<>();
        ops.put("Baseline BA", new double[]{baselineResult.operations});
        ops.put("Improved BA + DWA", new double[]{enhancedResult.operations});

        LinkedHashMap<String, double[]> fitness = new LinkedHashMap<>();
        fitness.put("Baseline BA", new double[]{baselineResult.fitness});
        fitness.put("Improved BA + DWA", new double[]{enhancedResult.fitness});

        metricTimeChart.setChart("Execution Time Across Input Sizes", "Time (ms)", cats, time);
        metricMemoryChart.setChart("Memory Usage", "Memory (KB)", cats, mem);
        metricOperationsChart.setChart("Number of Operations", "Operations", cats, ops);
        scalabilityChart.setChart("Scalability with Input Size", "Fitness", cats, fitness);

        runtimeComparisonChart.setChart("Runtime vs Input Size", "Time (ms)", cats, time);
        memoryComparisonChart.setChart("Memory Usage vs Input Size", "Memory (KB)", cats, mem);
        operationsComparisonChart.setChart("Operations vs Input Size", "Operations", cats, ops);

        analysisRuntimeChart.setChart("Runtime Comparison", "Time (ms)", cats, time);
        analysisMemoryChart.setChart("Space Usage Comparison", "Memory (KB)", cats, mem);
        bestWorstChart.setChart("Best and Worst Case Fitness", "Fitness", cats, fitness);

        baselineTimeChart.setChart("Baseline BA - Execution Time", "Time (ms)", cats, only("Baseline BA", new double[]{baselineResult.executionTimeMs}));
        baselineMemoryChart.setChart("Baseline BA - Memory Usage", "Memory (KB)", cats, only("Baseline BA", new double[]{baselineResult.memoryKb}));
        baselineOperationsChart.setChart("Baseline BA - Operations", "Operations", cats, only("Baseline BA", new double[]{baselineResult.operations}));

        enhancedTimeChart.setChart("Improved BA + DWA vs Baseline BA - Execution Time", "Time (ms)", cats, time);
        enhancedMemoryChart.setChart("Improved BA + DWA vs Baseline BA - Memory Usage", "Memory (KB)", cats, mem);
        enhancedOperationsChart.setChart("Improved BA + DWA vs Baseline BA - Operations", "Operations", cats, ops);
    }

    void refreshAllChartsFromExperiment() {
        String[] cats = {"Small", "Medium", "Large"};

        double[] baseTime = averageValues("Baseline BA", "time");
        double[] enhTime = averageValues("Improved BA + DWA", "time");

        double[] baseMemory = averageValues("Baseline BA", "memory");
        double[] enhMemory = averageValues("Improved BA + DWA", "memory");

        double[] baseOps = averageValues("Baseline BA", "operations");
        double[] enhOps = averageValues("Improved BA + DWA", "operations");

        double[] baseFitness = averageValues("Baseline BA", "fitness");
        double[] enhFitness = averageValues("Improved BA + DWA", "fitness");

        LinkedHashMap<String, double[]> timeMap = new LinkedHashMap<>();
        timeMap.put("Baseline BA", baseTime);
        timeMap.put("Improved BA + DWA", enhTime);

        LinkedHashMap<String, double[]> memoryMap = new LinkedHashMap<>();
        memoryMap.put("Baseline BA", baseMemory);
        memoryMap.put("Improved BA + DWA", enhMemory);

        LinkedHashMap<String, double[]> operationsMap = new LinkedHashMap<>();
        operationsMap.put("Baseline BA", baseOps);
        operationsMap.put("Improved BA + DWA", enhOps);

        LinkedHashMap<String, double[]> scalabilityMap = new LinkedHashMap<>();
        scalabilityMap.put("Baseline BA Fitness", baseFitness);
        scalabilityMap.put("Improved BA + DWA Fitness", enhFitness);

        metricTimeChart.setChart("Execution Time Across Input Sizes", "Time (ms)", cats, timeMap);
        metricMemoryChart.setChart("Memory Usage Across Input Sizes", "Memory (KB)", cats, memoryMap);
        metricOperationsChart.setChart("Number of Operations Across Input Sizes", "Operations", cats, operationsMap);
        scalabilityChart.setChart("Scalability with Input Size", "Fitness Score", cats, scalabilityMap);

        runtimeComparisonChart.setChart("Runtime vs Input Size", "Time (ms)", cats, timeMap);
        memoryComparisonChart.setChart("Memory Usage vs Input Size", "Memory (KB)", cats, memoryMap);
        operationsComparisonChart.setChart("Operations vs Input Size", "Operations", cats, operationsMap);

        analysisRuntimeChart.setChart("Runtime Comparison", "Time (ms)", cats, timeMap);
        analysisMemoryChart.setChart("Space Usage Comparison", "Memory (KB)", cats, memoryMap);

        baselineTimeChart.setChart("Baseline BA - Execution Time Across Input Sizes", "Time (ms)", cats, only("Baseline BA", baseTime));
        baselineMemoryChart.setChart("Baseline BA - Memory Usage", "Memory (KB)", cats, only("Baseline BA", baseMemory));
        baselineOperationsChart.setChart("Baseline BA - Operations", "Operations", cats, only("Baseline BA", baseOps));

        enhancedTimeChart.setChart("Improved BA + DWA vs Baseline BA - Execution Time", "Time (ms)", cats, timeMap);
        enhancedMemoryChart.setChart("Improved BA + DWA vs Baseline BA - Memory Usage", "Memory (KB)", cats, memoryMap);
        enhancedOperationsChart.setChart("Improved BA + DWA vs Baseline BA - Operations", "Operations", cats, operationsMap);

        LinkedHashMap<String, double[]> bestWorst = new LinkedHashMap<>();
        bestWorst.put("Baseline Best", caseValues("Baseline BA", "Best"));
        bestWorst.put("Baseline Worst", caseValues("Baseline BA", "Worst"));
        bestWorst.put("Enhanced Best", caseValues("Improved BA + DWA", "Best"));
        bestWorst.put("Enhanced Worst", caseValues("Improved BA + DWA", "Worst"));
        bestWorstChart.setChart("Performance Under Best and Worst Cases", "Fitness Score", cats, bestWorst);
    }

    LinkedHashMap<String, double[]> only(String label, double[] values) {
        LinkedHashMap<String, double[]> map = new LinkedHashMap<>();
        map.put(label, values);
        return map;
    }

    double[] averageValues(String algorithm, String field) {
        String[] sizes = {"Small 15x15", "Medium 25x25", "Large 35x35"};
        double[] values = new double[sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            ResultRecord r = findRecord(sizes[i], algorithm, "Average");

            if (r != null) values[i] = getField(r, field);
        }

        return values;
    }

    double[] caseValues(String algorithm, String caseName) {
        String[] sizes = {"Small 15x15", "Medium 25x25", "Large 35x35"};
        double[] values = new double[sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            ResultRecord r = findRecord(sizes[i], algorithm, caseName);

            if (r != null) values[i] = r.fitness;
        }

        return values;
    }

    double getField(ResultRecord r, String field) {
        switch (field) {
            case "time": return r.time;
            case "memory": return r.memory;
            case "operations": return r.operations;
            case "length": return r.length;
            case "fitness": return r.fitness;
            default: return 0;
        }
    }

    void buildWinnerText() {
        StringBuilder sb = new StringBuilder();

        sb.append("Which Algorithm Outperformed the Other\n\n");

        String[] sizes = uniqueAverageSizes();

        if (sizes.length == 0) {
            ResultRecord base = findSingle("Baseline BA");
            ResultRecord enh = findSingle("Improved BA + DWA");

            if (base == null || enh == null) {
                sb.append("Run Full Experiment to generate this section.");
                winnerText.setText(sb.toString());
                return;
            }

            sb.append("Current Single Run:\n");
            sb.append(base.time <= enh.time ? "• Faster runtime: Baseline BA\n" : "• Faster runtime: Improved BA + DWA\n");
            sb.append(base.fitness <= enh.fitness ? "• Better path quality: Baseline BA\n" : "• Better path quality: Improved BA + DWA\n");
            sb.append("• Baseline fitness: ").append(format(base.fitness)).append("\n");
            sb.append("• Enhanced fitness: ").append(format(enh.fitness)).append("\n\n");
            sb.append("Use Run Full Experiment to identify input-size thresholds.");
            winnerText.setText(sb.toString());
            return;
        }

        int baselineFitnessWins = 0;
        int enhancedFitnessWins = 0;

        for (String size : sizes) {
            ResultRecord base = findRecord(size, "Baseline BA", "Average");
            ResultRecord enh = findRecord(size, "Improved BA + DWA", "Average");

            if (base == null || enh == null) continue;

            sb.append(size).append(":\n");

            if (base.time <= enh.time) {
                sb.append("• Faster runtime: Baseline BA\n");
            } else {
                sb.append("• Faster runtime: Improved BA + DWA\n");
            }

            if (base.fitness <= enh.fitness) {
                baselineFitnessWins++;
                sb.append("• Better path quality: Baseline BA\n");
            } else {
                enhancedFitnessWins++;
                sb.append("• Better path quality: Improved BA + DWA\n");
            }

            sb.append("• Baseline fitness: ").append(format(base.fitness)).append("\n");
            sb.append("• Enhanced fitness: ").append(format(enh.fitness)).append("\n\n");
        }

        sb.append("Overall Identification:\n");

        if (enhancedFitnessWins > baselineFitnessWins) {
            sb.append("The Improved BA + DWA hybrid algorithm outperformed the baseline in path quality in most input sizes.\n");
        } else if (baselineFitnessWins > enhancedFitnessWins) {
            sb.append("The Baseline Bat Algorithm outperformed the enhanced algorithm in path quality in most input sizes for this run.\n");
        } else {
            sb.append("Both algorithms performed similarly in path quality across the tested input sizes.\n");
        }

        sb.append("\nInput Size Thresholds Where Performance Diverges:\n");
        sb.append("Check the Runtime, Memory, Operations, and Scalability graphs. The input size where the two lines separate more clearly is where the performance difference becomes more visible.\n");

        sb.append("\nConditions Under Which One Algorithm Dominates:\n");
        sb.append("• Improved BA + DWA usually dominates when path quality, lower fitness, and smoother paths are prioritized.\n");
        sb.append("• Baseline BA may dominate when lower execution time or fewer operations are prioritized.\n");

        winnerText.setText(sb.toString());
    }

    PathResult runMeasuredAlgorithm(boolean enhanced, int population, int iterations, long seed) {
        Runtime runtime = Runtime.getRuntime();

        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.nanoTime();

        OperationCounter counter = new OperationCounter();
        PathResult result = runBatAlgorithm(enhanced, population, iterations, seed, counter);

        long endTime = System.nanoTime();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();

        result.executionTimeMs = (endTime - startTime) / 1_000_000.0;
        result.memoryKb = Math.abs(memAfter - memBefore) / 1024.0;
        result.operations = counter.count;

        if (result.memoryKb < 1) {
            result.memoryKb = estimateMemoryKb(population, result);
        }

        return result;
    }

    double estimateMemoryKb(int population, PathResult result) {
        double batMemory = population * WAYPOINTS * 2 * 16;
        double pathMemory = result.path == null ? 0 : result.path.size() * 16;
        double historyMemory = result.history == null ? 0 : result.history.size() * 8;
        return (batMemory + pathMemory + historyMemory) / 1024.0;
    }

    PathResult runBatAlgorithm(boolean enhanced, int population, int maxIterations, long seed, OperationCounter counter) {
        Random rnd = new Random(seed);
        Bat[] bats = new Bat[population];

        for (int i = 0; i < population; i++) {
            bats[i] = createRandomBat(rnd);
            evaluateBat(bats[i], counter);
        }

        Bat best = bats[0].copy();

        for (Bat bat : bats) {
            counter.add(1);

            if (bat.fitness < best.fitness) {
                best = bat.copy();
            }
        }

        List<Double> history = new ArrayList<>();

        double frequencyMin = 0.0;
        double frequencyMax = 2.0;
        double loudnessDecay = 0.90;
        double pulseGrowth = 0.90;

        double inertiaMax = 0.90;
        double inertiaMin = 0.35;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            for (Bat bat : bats) {
                double frequency = frequencyMin + (frequencyMax - frequencyMin) * rnd.nextDouble();
                counter.add(2);

                for (int d = 0; d < bat.position.length; d++) {
                    double inertia = 1.0;

                    if (enhanced) {
                        double progress = Math.log(1.0 + iteration) / Math.log(1.0 + maxIterations);
                        inertia = inertiaMax - (inertiaMax - inertiaMin) * progress;
                        inertia += 0.03 * rnd.nextGaussian();
                        counter.add(5);
                    }

                    bat.velocity[d] = inertia * bat.velocity[d] + (best.position[d] - bat.position[d]) * frequency;
                    bat.position[d] = clamp(bat.position[d] + bat.velocity[d], 0, gridSize - 1);
                    counter.add(6);
                }

                if (rnd.nextDouble() > bat.pulseRate) {
                    for (int d = 0; d < bat.position.length; d++) {
                        double localSearch = (rnd.nextDouble() * 2 - 1) * bat.loudness;
                        counter.add(3);

                        if (enhanced) {
                            double cauchyDisturbance = Math.tan(Math.PI * (rnd.nextDouble() - 0.5));
                            double strength = 0.35 * (maxIterations - iteration) / Math.max(1.0, maxIterations);
                            localSearch += strength * cauchyDisturbance * rnd.nextDouble();
                            counter.add(6);
                        }

                        bat.position[d] = clamp(best.position[d] + localSearch, 0, gridSize - 1);
                        counter.add(3);
                    }
                }

                evaluateBat(bat, counter);

                if (bat.fitness < best.fitness && rnd.nextDouble() < bat.loudness) {
                    best = bat.copy();
                    bat.loudness *= loudnessDecay;
                    bat.pulseRate = bat.initialPulseRate * (1 - Math.exp(-pulseGrowth * iteration));
                    counter.add(5);
                }
            }

            history.add(best.fitness);
        }

        PathResult result = evaluateBat(best, counter);
        if (enhanced) {
            result = applyHybridIbaDwaRefinement(result, counter);
        }
        result.history = history;
        return result;
    }

    Bat createRandomBat(Random rnd) {
        Bat bat = new Bat(WAYPOINTS * 2);

        for (int i = 0; i < WAYPOINTS; i++) {
            double t = (i + 1.0) / (WAYPOINTS + 1.0);

            double x = start.x + t * (goal.x - start.x) + rnd.nextGaussian() * (gridSize * 0.16);
            double y = start.y + t * (goal.y - start.y) + rnd.nextGaussian() * (gridSize * 0.16);

            bat.position[i * 2] = clamp(x, 0, gridSize - 1);
            bat.position[i * 2 + 1] = clamp(y, 0, gridSize - 1);

            bat.velocity[i * 2] = rnd.nextGaussian() * 0.5;
            bat.velocity[i * 2 + 1] = rnd.nextGaussian() * 0.5;
        }

        bat.loudness = 0.90;
        bat.pulseRate = 0.20 + rnd.nextDouble() * 0.20;
        bat.initialPulseRate = bat.pulseRate;

        return bat;
    }

    PathResult evaluateBat(Bat bat, OperationCounter counter) {
        List<Point> controlPoints = new ArrayList<>();
        controlPoints.add(new Point(start));

        for (int i = 0; i < WAYPOINTS; i++) {
            int x = (int) Math.round(clamp(bat.position[i * 2], 0, gridSize - 1));
            int y = (int) Math.round(clamp(bat.position[i * 2 + 1], 0, gridSize - 1));

            Point p = new Point(x, y);

            if (!p.equals(controlPoints.get(controlPoints.size() - 1))) {
                controlPoints.add(p);
            }

            counter.add(5);
        }

        controlPoints.add(new Point(goal));

        List<Point> fullPath = new ArrayList<>();
        int collisions = 0;

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            List<Point> segment = createLine(controlPoints.get(i), controlPoints.get(i + 1), counter);

            for (Point point : segment) {
                if (point.x < 0 || point.x >= gridSize || point.y < 0 || point.y >= gridSize || obstacles[point.x][point.y]) {
                    collisions++;
                }

                if (fullPath.isEmpty() || !fullPath.get(fullPath.size() - 1).equals(point)) {
                    fullPath.add(point);
                }

                counter.add(4);
            }
        }

        double length = 0;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            length += fullPath.get(i).distance(fullPath.get(i + 1));
            counter.add(2);
        }

        int turns = countTurns(fullPath, counter);
        double smoothnessCost = turns * Math.PI / 4.0;

        double fitness =
                PATH_WEIGHT * length +
                SAFETY_WEIGHT * COLLISION_PENALTY * collisions +
                SMOOTHNESS_WEIGHT * smoothnessCost;

        bat.fitness = fitness;
        counter.add(6);

        return new PathResult(length, fitness, turns, collisions, fullPath, controlPoints);
    }

    PathResult applyHybridIbaDwaRefinement(PathResult globalResult, OperationCounter counter) {
        List<Point> globalGuide = extractTurningPoints(globalResult.path, counter);
        List<Point> switchedGuide = applyVirtualPointPathSwitch(globalGuide, counter);
        List<Point> localPath = runDynamicWindowLocalPlanner(switchedGuide, counter);

        if (localPath.isEmpty() || !localPath.get(localPath.size() - 1).equals(goal)) {
            List<Point> fallback = findGridPath(start, goal, counter);
            if (!fallback.isEmpty()) {
                localPath = fallback;
                switchedGuide = extractTurningPoints(fallback, counter);
            }
        }

        return evaluatePath(localPath, switchedGuide, counter);
    }

    List<Point> extractTurningPoints(List<Point> path, OperationCounter counter) {
        List<Point> points = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            points.add(new Point(start));
            points.add(new Point(goal));
            return points;
        }

        points.add(new Point(path.get(0)));

        for (int i = 1; i < path.size() - 1; i++) {
            Point previous = path.get(i - 1);
            Point current = path.get(i);
            Point next = path.get(i + 1);

            int dx1 = Integer.compare(current.x - previous.x, 0);
            int dy1 = Integer.compare(current.y - previous.y, 0);
            int dx2 = Integer.compare(next.x - current.x, 0);
            int dy2 = Integer.compare(next.y - current.y, 0);

            if (dx1 != dx2 || dy1 != dy2) {
                points.add(new Point(current));
            }

            counter.add(6);
        }

        Point last = path.get(path.size() - 1);
        if (!points.get(points.size() - 1).equals(last)) {
            points.add(new Point(last));
        }

        return points;
    }

    List<Point> applyVirtualPointPathSwitch(List<Point> guide, OperationCounter counter) {
        List<Point> switched = new ArrayList<>();
        switched.add(new Point(start));

        for (int i = 0; i < guide.size() - 1; i++) {
            Point from = guide.get(i);
            Point to = guide.get(i + 1);

            if (segmentIsSafe(from, to, counter)) {
                appendPoint(switched, to);
            } else {
                List<Point> alternate = findVirtualPointRoute(from, to, counter);
                if (alternate.isEmpty()) {
                    alternate = findGridPath(from, to, counter);
                }

                for (Point p : alternate) {
                    appendPoint(switched, p);
                }
            }

            counter.add(3);
        }

        if (!switched.get(switched.size() - 1).equals(goal)) {
            appendPoint(switched, goal);
        }

        return switched;
    }

    List<Point> findVirtualPointRoute(Point from, Point to, OperationCounter counter) {
        List<Point> nodes = new ArrayList<>();
        nodes.add(new Point(from));
        nodes.addAll(createVirtualPoints());
        nodes.add(new Point(to));

        int n = nodes.size();
        double[] dist = new double[n];
        int[] prev = new int[n];
        boolean[] used = new boolean[n];

        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[0] = 0;

        for (int step = 0; step < n; step++) {
            int current = -1;

            for (int i = 0; i < n; i++) {
                if (!used[i] && (current == -1 || dist[i] < dist[current])) {
                    current = i;
                }
            }

            if (current == -1 || Double.isInfinite(dist[current])) break;
            used[current] = true;

            for (int next = 0; next < n; next++) {
                if (current == next || used[next]) continue;
                if (!segmentIsSafe(nodes.get(current), nodes.get(next), counter)) continue;

                double candidate = dist[current] + nodes.get(current).distance(nodes.get(next));
                if (candidate < dist[next]) {
                    dist[next] = candidate;
                    prev[next] = current;
                }

                counter.add(5);
            }
        }

        if (Double.isInfinite(dist[n - 1])) return new ArrayList<>();

        LinkedList<Point> route = new LinkedList<>();
        for (int at = n - 1; at != -1; at = prev[at]) {
            route.addFirst(new Point(nodes.get(at)));
        }

        return route;
    }

    List<Point> createVirtualPoints() {
        int low = Math.max(1, gridSize / 5);
        int mid = gridSize / 2;
        int high = Math.min(gridSize - 2, (gridSize * 4) / 5);

        List<Point> points = new ArrayList<>();
        points.add(nearestFreePoint(new Point(low, low)));
        points.add(nearestFreePoint(new Point(mid, low)));
        points.add(nearestFreePoint(new Point(high, low)));
        points.add(nearestFreePoint(new Point(low, mid)));
        points.add(nearestFreePoint(new Point(high, mid)));
        points.add(nearestFreePoint(new Point(low, high)));
        points.add(nearestFreePoint(new Point(mid, high)));
        points.add(nearestFreePoint(new Point(high, high)));
        return points;
    }

    Point nearestFreePoint(Point point) {
        if (isFree(point.x, point.y)) return point;

        for (int radius = 1; radius < gridSize; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = point.x + dx;
                    int y = point.y + dy;
                    if (isFree(x, y)) return new Point(x, y);
                }
            }
        }

        return new Point(start);
    }

    List<Point> runDynamicWindowLocalPlanner(List<Point> guide, OperationCounter counter) {
        List<Point> path = new ArrayList<>();
        Point current = new Point(start);
        path.add(new Point(current));

        int targetIndex = Math.min(1, guide.size() - 1);
        Point previous = new Point(current);
        int maxSteps = gridSize * gridSize * 4;

        for (int step = 0; step < maxSteps && !current.equals(goal); step++) {
            while (targetIndex < guide.size() - 1 && current.distance(guide.get(targetIndex)) <= 1.0) {
                targetIndex++;
            }

            Point target = guide.get(targetIndex);
            Point next = chooseDynamicWindowMove(previous, current, target, guide, counter);
            if (next == null || next.equals(current)) break;

            previous = current;
            current = next;
            appendPoint(path, current);
            counter.add(8);
        }

        return smoothGridPath(path, counter);
    }

    Point chooseDynamicWindowMove(Point previous, Point current, Point target, List<Point> guide, OperationCounter counter) {
        Point best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                Point candidate = new Point(current.x + dx, current.y + dy);
                if (!isFree(candidate.x, candidate.y)) continue;

                double heading = -candidate.distance(target);
                double clearance = obstacleClearance(candidate);
                double speed = Math.hypot(dx, dy);
                double adherence = -distanceToGuide(candidate, guide);
                double smoothness = previous.equals(current) ? 0 : -turnPenalty(previous, current, candidate);
                double score = 2.8 * heading + 1.5 * clearance + 0.2 * speed + 1.2 * adherence + 0.7 * smoothness;

                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }

                counter.add(12);
            }
        }

        return best;
    }

    double obstacleClearance(Point point) {
        double best = 3.0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int x = point.x + dx;
                int y = point.y + dy;

                if (!isFree(x, y)) {
                    best = Math.min(best, Math.hypot(dx, dy));
                }
            }
        }

        return best;
    }

    double distanceToGuide(Point point, List<Point> guide) {
        double best = Double.POSITIVE_INFINITY;

        for (Point p : guide) {
            best = Math.min(best, point.distance(p));
        }

        return best;
    }

    double turnPenalty(Point previous, Point current, Point next) {
        int dx1 = Integer.compare(current.x - previous.x, 0);
        int dy1 = Integer.compare(current.y - previous.y, 0);
        int dx2 = Integer.compare(next.x - current.x, 0);
        int dy2 = Integer.compare(next.y - current.y, 0);
        return (dx1 == dx2 && dy1 == dy2) ? 0.0 : 1.0;
    }

    List<Point> smoothGridPath(List<Point> path, OperationCounter counter) {
        if (path.size() < 3) return path;

        List<Point> smoothed = new ArrayList<>();
        int i = 0;
        smoothed.add(new Point(path.get(0)));

        while (i < path.size() - 1) {
            int best = i + 1;

            for (int j = path.size() - 1; j > i + 1; j--) {
                if (segmentIsSafe(path.get(i), path.get(j), counter)) {
                    best = j;
                    break;
                }
            }

            appendPoint(smoothed, path.get(best));
            i = best;
            counter.add(4);
        }

        return smoothed;
    }

    List<Point> findGridPath(Point from, Point to, OperationCounter counter) {
        Point[][] parent = new Point[gridSize][gridSize];
        boolean[][] visited = new boolean[gridSize][gridSize];
        ArrayDeque<Point> queue = new ArrayDeque<>();

        if (!isFree(from.x, from.y) || !isFree(to.x, to.y)) return new ArrayList<>();

        queue.add(new Point(from));
        visited[from.x][from.y] = true;

        int[] delta = {-1, 0, 1};

        while (!queue.isEmpty()) {
            Point current = queue.removeFirst();
            if (current.equals(to)) break;

            for (int dx : delta) {
                for (int dy : delta) {
                    if (dx == 0 && dy == 0) continue;

                    int x = current.x + dx;
                    int y = current.y + dy;

                    if (!isFree(x, y) || visited[x][y]) continue;

                    visited[x][y] = true;
                    parent[x][y] = current;
                    queue.addLast(new Point(x, y));
                    counter.add(8);
                }
            }
        }

        if (!visited[to.x][to.y]) return new ArrayList<>();

        LinkedList<Point> path = new LinkedList<>();
        for (Point at = new Point(to); at != null; at = parent[at.x][at.y]) {
            path.addFirst(new Point(at));
            if (at.equals(from)) break;
        }

        return path;
    }

    PathResult evaluatePath(List<Point> path, List<Point> controlPoints, OperationCounter counter) {
        if (path == null || path.isEmpty()) {
            path = new ArrayList<>();
            path.add(new Point(start));
        }

        double length = 0;
        int collisions = 0;

        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            if (!isFree(p.x, p.y)) collisions++;

            if (i < path.size() - 1) {
                length += p.distance(path.get(i + 1));
            }

            counter.add(5);
        }

        int turns = countTurns(path, counter);
        double smoothnessCost = turns * Math.PI / 4.0;
        double fitness =
                PATH_WEIGHT * length +
                SAFETY_WEIGHT * COLLISION_PENALTY * collisions +
                SMOOTHNESS_WEIGHT * smoothnessCost;

        return new PathResult(length, fitness, turns, collisions, path, controlPoints);
    }

    boolean segmentIsSafe(Point from, Point to, OperationCounter counter) {
        for (Point point : createLine(from, to, counter)) {
            if (!isFree(point.x, point.y)) return false;
        }

        return true;
    }

    boolean isFree(int x, int y) {
        return x >= 0 && x < gridSize && y >= 0 && y < gridSize && !obstacles[x][y];
    }

    void appendPoint(List<Point> points, Point point) {
        if (points.isEmpty() || !points.get(points.size() - 1).equals(point)) {
            points.add(new Point(point));
        }
    }

    int countTurns(List<Point> path, OperationCounter counter) {
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

            counter.add(6);
        }

        return turns;
    }

    List<Point> createLine(Point start, Point end, OperationCounter counter) {
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

            if (x0 == x1 && y0 == y1) break;

            int error2 = 2 * error;

            if (error2 > -dy) {
                error -= dy;
                x0 += sx;
            }

            if (error2 < dx) {
                error += dx;
                y0 += sy;
            }

            counter.add(7);
        }

        return points;
    }

    double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    void addNote(String text) {
        notesArea.append(text + "\n");
        notesArea.setCaretPosition(notesArea.getDocument().getLength());
    }

    class MapPanel extends JPanel {
        MapPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(218, 225, 235)),
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

                    if (x >= 0 && x < gridSize && y >= 0 && y < gridSize) {
                        Point clicked = new Point(x, y);

                        if (!clicked.equals(start) && !clicked.equals(goal)) {
                            obstacles[x][y] = !obstacles[x][y];
                            clearCurrentResults();
                            addNote("Obstacle changed at coordinate (" + x + ", " + y + ").");
                        }
                    }
                }
            });
        }

        int getCellSize() {
            return Math.min((getWidth() - 70) / gridSize, (getHeight() - 110) / gridSize);
        }

        int getOffsetX(int cell) {
            return (getWidth() - cell * gridSize) / 2;
        }

        int getOffsetY(int cell) {
            return 60;
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
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.drawString("Actual Simulation Map", 18, 26);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(90, 102, 120));
            g2.drawString("S = Start | E = End | Click grid cells to edit obstacles | Current: " + gridSize + " x " + gridSize, 18, 45);
        }

        void drawGrid(Graphics2D g2) {
            int cell = getCellSize();
            int offsetX = getOffsetX(cell);
            int offsetY = getOffsetY(cell);

            for (int x = 0; x < gridSize; x++) {
                for (int y = 0; y < gridSize; y++) {
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
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, cell / 2)));

            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + fm.getAscent() / 3);
        }

        void drawPaths(Graphics2D g2) {
            if (baselineResult != null) {
                drawPath(g2, baselineResult.path, new Color(225, 80, 75), 3.0f);
            }

            if (enhancedResult != null) {
                drawPath(g2, enhancedResult.path, new Color(35, 125, 235), 4.0f);
                drawControlPoints(g2, enhancedResult.controlPoints);
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

            for (Point p : points) {
                int cx = offsetX + p.x * cell + cell / 2;
                int cy = offsetY + p.y * cell + cell / 2;
                g2.fillOval(cx - 3, cy - 3, 6, 6);
            }
        }

        void drawLegend(Graphics2D g2) {
            int y = getHeight() - 26;
            int x = 24;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            g2.setColor(new Color(225, 80, 75));
            g2.fillRoundRect(x, y - 8, 24, 6, 6, 6);
            g2.setColor(new Color(50, 60, 75));
            g2.drawString("Baseline BA", x + 32, y);

            x += 135;

            g2.setColor(new Color(35, 125, 235));
            g2.fillRoundRect(x, y - 8, 24, 6, 6, 6);
            g2.setColor(new Color(50, 60, 75));
            g2.drawString("Improved BA + DWA", x + 32, y);

            x += 150;

            g2.setColor(new Color(45, 52, 65));
            g2.fillRect(x, y - 13, 14, 14);
            g2.setColor(new Color(50, 60, 75));
            g2.drawString("Obstacle", x + 22, y);
        }
    }

    class SummaryPanel extends JPanel {
        SummaryPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(218, 225, 235)),
                    new EmptyBorder(12, 12, 12, 12)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.setColor(new Color(30, 40, 55));
            g2.drawString("Current Simulation Summary", 18, 30);

            if (baselineResult == null || enhancedResult == null) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(new Color(90, 102, 120));
                g2.drawString("Run a simulation to display the current result summary.", 18, 62);
                return;
            }

            drawCard(g2, "Baseline BA", baselineResult, 18, 55, new Color(225, 80, 75));
            drawCard(g2, "Improved BA + DWA", enhancedResult, 290, 55, new Color(35, 125, 235));

            String winner = enhancedResult.fitness < baselineResult.fitness ? "Improved BA + DWA" : "Baseline BA";

            g2.setColor(new Color(245, 248, 252));
            g2.fillRoundRect(18, 170, getWidth() - 36, 45, 16, 16);

            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("Current Better Algorithm: " + winner, 34, 198);
        }

        void drawCard(Graphics2D g2, String title, PathResult r, int x, int y, Color color) {
            g2.setColor(new Color(248, 250, 253));
            g2.fillRoundRect(x, y, 245, 98, 18, 18);

            g2.setColor(color);
            g2.fillRoundRect(x, y, 8, 98, 8, 8);

            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString(title, x + 18, y + 24);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(75, 85, 100));

            g2.drawString("Time: " + format(r.executionTimeMs) + " ms", x + 18, y + 45);
            g2.drawString("Memory: " + format(r.memoryKb) + " KB", x + 18, y + 62);
            g2.drawString("Fitness: " + format(r.fitness), x + 18, y + 79);
            g2.drawString("Length: " + format(r.length), x + 130, y + 79);
        }
    }

    class LineChartPanel extends JPanel {
        String title = "Graph";
        String yLabel = "Value";
        String[] categories = new String[0];
        LinkedHashMap<String, double[]> series = new LinkedHashMap<>();

        LineChartPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(320, 220));
            setMinimumSize(new Dimension(260, 180));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(218, 225, 235)),
                    new EmptyBorder(8, 8, 8, 8)
            ));
        }

        void setChart(String title, String yLabel, String[] categories, LinkedHashMap<String, double[]> series) {
            this.title = title;
            this.yLabel = yLabel;
            this.categories = categories;
            this.series = series;
            revalidate();
            repaint();
        }

        void clear() {
            this.title = "Graph";
            this.yLabel = "Value";
            this.categories = new String[0];
            this.series = new LinkedHashMap<>();
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(30, 40, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString(title, 16, 24);

            if (series == null || series.isEmpty() || categories.length == 0) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(new Color(90, 102, 120));
                g2.drawString("Run Full Experiment to generate this graph.", 16, 52);
                return;
            }

            int left = 60;
            int top = 45;
            int right = 25;
            int bottom = 45;

            int w = getWidth() - left - right;
            int h = getHeight() - top - bottom;

            if (w < 80 || h < 60) return;

            double max = 1;
            double min = 0;

            for (double[] vals : series.values()) {
                for (double v : vals) {
                    max = Math.max(max, v);
                }
            }

            if (Math.abs(max - min) < 0.0001) max = min + 1;

            g2.setColor(new Color(248, 250, 253));
            g2.fillRect(left, top, w, h);

            g2.setColor(new Color(225, 231, 240));
            for (int i = 0; i <= 4; i++) {
                int y = top + i * h / 4;
                g2.drawLine(left, y, left + w, y);
            }

            g2.setColor(new Color(120, 130, 145));
            g2.drawRect(left, top, w, h);

            Color[] colors = {
                    new Color(225, 80, 75),
                    new Color(35, 125, 235),
                    new Color(35, 160, 110),
                    new Color(150, 90, 210),
                    new Color(230, 150, 50)
            };

            int colorIndex = 0;

            for (Map.Entry<String, double[]> entry : series.entrySet()) {
                Color color = colors[colorIndex % colors.length];
                drawSeries(g2, entry.getValue(), left, top, w, h, min, max, color);
                colorIndex++;
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(70, 80, 95));

            for (int i = 0; i < categories.length; i++) {
                int x;
                if (categories.length == 1) x = left + w / 2;
                else x = left + (int) (i * (w / (double) (categories.length - 1)));

                g2.drawString(categories[i], x - 18, top + h + 20);
            }

            g2.drawString(format(max), 8, top + 5);
            g2.drawString(format(min), 8, top + h);
            g2.drawString(yLabel, 8, top + h / 2);

            drawLegend(g2, left + 8, top + 14, colors);
        }

        void drawSeries(Graphics2D g2, double[] values, int left, int top, int w, int h, double min, double max, Color color) {
            if (values == null || values.length == 0) return;

            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);

            Path2D path = new Path2D.Double();

            for (int i = 0; i < values.length; i++) {
                double x;
                if (values.length == 1) x = left + w / 2.0;
                else x = left + i * (w / (double) (values.length - 1));

                double y = top + h - ((values[i] - min) / (max - min)) * h;

                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);

                g2.fillOval((int) x - 4, (int) y - 4, 8, 8);
            }

            g2.draw(path);
        }

        void drawLegend(Graphics2D g2, int x, int y, Color[] colors) {
            int i = 0;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            for (String name : series.keySet()) {
                Color color = colors[i % colors.length];

                g2.setColor(color);
                g2.fillRoundRect(x, y - 8, 18, 6, 6, 6);

                g2.setColor(new Color(70, 80, 95));
                g2.drawString(name, x + 25, y);

                x += 125;
                i++;
            }
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
            Bat b = new Bat(position.length);
            b.position = position.clone();
            b.velocity = velocity.clone();
            b.loudness = loudness;
            b.pulseRate = pulseRate;
            b.initialPulseRate = initialPulseRate;
            b.fitness = fitness;
            return b;
        }
    }

    static class PathResult {
        double length;
        double fitness;
        int turns;
        int collisions;

        double executionTimeMs;
        double memoryKb;
        long operations;

        List<Point> path;
        List<Point> controlPoints;
        List<Double> history = new ArrayList<>();

        PathResult(double length, double fitness, int turns, int collisions, List<Point> path, List<Point> controlPoints) {
            this.length = length;
            this.fitness = fitness;
            this.turns = turns;
            this.collisions = collisions;
            this.path = path;
            this.controlPoints = controlPoints;
        }
    }

    static class OperationCounter {
        long count = 0;

        void add(long value) {
            count += value;
        }
    }

    static class AverageResult {
        double time;
        double memory;
        double operations;
        double length;
        double fitness;
        double turns;
        double collisions;

        void add(PathResult r) {
            time += r.executionTimeMs;
            memory += r.memoryKb;
            operations += r.operations;
            length += r.length;
            fitness += r.fitness;
            turns += r.turns;
            collisions += r.collisions;
        }

        ResultRecord toRecord(String inputSize, String algorithm, String caseName, int trials) {
            ResultRecord r = new ResultRecord();
            r.inputSize = inputSize;
            r.algorithm = algorithm;
            r.caseName = caseName;
            r.time = time / trials;
            r.memory = memory / trials;
            r.operations = Math.round(operations / trials);
            r.length = length / trials;
            r.fitness = fitness / trials;
            r.turns = turns / trials;
            r.collisions = collisions / trials;
            return r;
        }
    }

    static class ResultRecord {
        String inputSize;
        String algorithm;
        String caseName;

        double time;
        double memory;
        double operations;
        double length;
        double fitness;
        double turns;
        double collisions;

        Object[] toRow() {
            return new Object[]{
                    inputSize,
                    algorithm,
                    caseName,
                    String.format(Locale.US, "%.2f", time),
                    String.format(Locale.US, "%.2f", memory),
                    Math.round(operations),
                    String.format(Locale.US, "%.2f", length),
                    String.format(Locale.US, "%.2f", fitness),
                    String.format(Locale.US, "%.2f", turns),
                    String.format(Locale.US, "%.2f", collisions)
            };
        }
    }
}
