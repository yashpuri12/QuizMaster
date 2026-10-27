package com.quizmaster.ui;

import com.quizmaster.model.QuizResult;
import com.quizmaster.service.ResultService;
import com.quizmaster.storage.ResultLoadResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Saved attempt history for the authenticated student. */
final class ResultHistoryPanel extends JPanel {
    private final QuizMasterFrame frame;
    private final ResultService results;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Quiz", "Syllabus unit", "Score", "Percentage", "Completed"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = ResultUi.table(model);
    private final CardLayout centerCards = new CardLayout();
    private final JPanel center = Ui.panel(Theme.CANVAS, centerCards);
    private final JButton details = Ui.button("View saved details", true);
    private final List<QuizResult> visible = new ArrayList<>();
    private final JLabel status = Ui.label(" ", Theme.SMALL, Theme.MUTED);

    ResultHistoryPanel(QuizMasterFrame frame, ResultService results) {
        super(new BorderLayout()); this.frame = frame; this.results = results;
        setBackground(Theme.CANVAS); setBorder(BorderFactory.createEmptyBorder(28, 34, 26, 34));
        JPanel header = Ui.panel(Theme.CANVAS, new BorderLayout());
        header.add(Ui.header("My attempts", "Your saved quiz history, newest first"), BorderLayout.CENTER);
        JButton back = Ui.button("← Student overview", false); back.addActionListener(e -> frame.showStudent()); header.add(Ui.headerAction(back), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JPanel body = Ui.panel(Theme.CANVAS, new BorderLayout(0, 12));
        center.add(ResultUi.scroll(table), "table");
        JPanel emptyCard = Ui.card();
        emptyCard.add(Ui.centeredLabel("No saved attempts yet", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        emptyCard.add(Ui.centeredLabel("Complete a quiz to build your personal history. Saved summaries will appear here, newest first.", Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
        JButton startQuiz = Ui.button("Choose a practice quiz", true);
        startQuiz.addActionListener(e -> frame.showQuizSelection());
        emptyCard.add(startQuiz, BorderLayout.SOUTH);
        center.add(Ui.centered(emptyCard), "empty");
        body.add(center, BorderLayout.CENTER);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {220, 220, 100, 110, 190};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JPanel footer = Ui.panel(Theme.CANVAS, new BorderLayout(10, 0));
        footer.add(status, BorderLayout.CENTER);
        details.setEnabled(false); details.addActionListener(e -> showSelected()); footer.add(details, BorderLayout.EAST);
        body.add(footer, BorderLayout.SOUTH); add(body, BorderLayout.CENTER);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) showSelected(); }
        });
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) details.setEnabled(table.getSelectedRow() >= 0); });
    }

    void refresh() {
        try {
            ResultLoadResult loaded = results.studentHistory(); visible.clear(); visible.addAll(loaded.getResults()); model.setRowCount(0);
            for (QuizResult r : visible) model.addRow(new Object[]{r.getQuizName(), r.getUnit().getTitleForSubject(r.getSubjectId().equals(com.quizmaster.model.Subject.OOP_ID)?com.quizmaster.model.Subject.OOP_NAME:""), ResultUi.score(r), ResultUi.percentage(r), ResultUi.DATE.format(r.getCompletedAt())});
            centerCards.show(center, visible.isEmpty() ? "empty" : "table");
            if (visible.isEmpty()) status.setText("No submitted attempts yet. Complete a quiz to see its saved result here.");
            else status.setText(visible.size() + " saved attempt(s). Select a row or double-click to view its saved summary.");
            if (!loaded.getIssues().isEmpty()) status.setText(status.getText() + " " + loaded.getIssues().size() + " malformed row(s) were skipped.");
        } catch (SecurityException ex) { status.setText("Access denied: " + ex.getMessage()); }
        catch (Exception ex) { status.setText("Could not read saved history: " + ex.getMessage()); }
    }

    private void showSelected() {
        int row = table.getSelectedRow(); if (row < 0 || row >= visible.size()) { JOptionPane.showMessageDialog(this, "Select an attempt first.", "No attempt selected", JOptionPane.INFORMATION_MESSAGE); return; }
        ResultUi.showDetails(this, visible.get(row));
    }
}
