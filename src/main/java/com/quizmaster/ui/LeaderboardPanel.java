package com.quizmaster.ui;

import com.quizmaster.model.LeaderboardEntry;
import com.quizmaster.model.QuizResult;
import com.quizmaster.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Real saved-attempt rankings filtered to one stable quiz identity. */
final class LeaderboardPanel extends JPanel {
    private final QuizMasterFrame frame;
    private final ResultService results;
    private final JComboBox<QuizChoice> quiz = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Rank", "Student", "Score", "Percentage", "Quiz", "Attempt date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel status = Ui.label("", Theme.SMALL, Theme.MUTED);
    private final CardLayout centerCards = new CardLayout();
    private final JPanel center = Ui.panel(Theme.CANVAS, centerCards);
    private final JTable table = ResultUi.table(model);

    LeaderboardPanel(QuizMasterFrame frame, ResultService results) {
        super(new BorderLayout()); this.frame = frame; this.results = results;
        setBackground(Theme.CANVAS); setBorder(BorderFactory.createEmptyBorder(28, 34, 26, 34));
        JPanel header = Ui.panel(Theme.CANVAS, new BorderLayout());
        header.add(Ui.header("Leaderboard", "Ranked saved attempts within the selected quiz"), BorderLayout.CENTER);
        JButton back = Ui.button("← Student overview", false); back.addActionListener(e -> frame.showStudent()); header.add(Ui.headerAction(back), BorderLayout.EAST); add(header, BorderLayout.NORTH);
        JPanel body = Ui.panel(Theme.CANVAS, new BorderLayout(0, 12));
        JPanel filters = Ui.panel(Theme.CANVAS, new FlowLayout(FlowLayout.LEFT, 10, 0)); filters.add(Ui.label("Quiz category", Theme.BODY_BOLD, Theme.TEXT));
        Ui.styleInput(quiz); quiz.setPreferredSize(new java.awt.Dimension(300, 40)); filters.add(quiz); body.add(filters, BorderLayout.NORTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {72, 180, 100, 110, 200, 190};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        center.add(ResultUi.scroll(table), "table");
        JPanel emptyCard = Ui.card();
        emptyCard.add(Ui.centeredLabel("No leaderboard entries yet", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        emptyCard.add(Ui.centeredLabel("Rankings appear when students submit quizzes. No sample scores are displayed.", Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
        center.add(Ui.centered(emptyCard), "empty");
        body.add(center, BorderLayout.CENTER);
        JPanel footer = Ui.panel(Theme.CANVAS, new BorderLayout()); footer.add(status, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH); add(body, BorderLayout.CENTER);
        quiz.addActionListener(e -> refreshRows());
    }

    void refresh() {
        try {
            Map<String, String> choices = results.availableStudentQuizCategories();
            String keep = quiz.getSelectedItem() instanceof QuizChoice current ? current.id : null;
            quiz.removeAllItems(); for (var entry : choices.entrySet()) quiz.addItem(new QuizChoice(entry.getKey(), entry.getValue()));
            if (keep != null) for (int i = 0; i < quiz.getItemCount(); i++) if (quiz.getItemAt(i).id.equals(keep)) quiz.setSelectedIndex(i);
            refreshRows();
        } catch (Exception ex) { model.setRowCount(0); status.setText("Could not load leaderboard: " + ex.getMessage()); }
    }

    private void refreshRows() {
        model.setRowCount(0); Object item = quiz.getSelectedItem();
        if (!(item instanceof QuizChoice choice)) { centerCards.show(center, "empty"); status.setText("No saved attempts are available for ranking yet."); return; }
        try {
            List<LeaderboardEntry> ranked = results.studentLeaderboard(choice.id);
            centerCards.show(center, ranked.isEmpty() ? "empty" : "table");
            for (LeaderboardEntry entry : ranked) {
                QuizResult r = entry.getResult();
                model.addRow(new Object[]{entry.getRank(), r.getStudentName(), ResultUi.score(r), ResultUi.percentage(r), r.getQuizName(), ResultUi.DATE.format(r.getCompletedAt())});
            }
            String message = ranked.isEmpty() ? "No saved attempts for this quiz."
                    : "Each saved attempt is ranked; repeat attempts by a student appear separately. Exact ties share a competition rank.";
            int issues = results.studentHistory().getIssues().size();
            if (issues > 0) message += " " + issues + " malformed result row(s) were skipped.";
            status.setText(message);
        } catch (Exception ex) { status.setText("Could not load leaderboard: " + ex.getMessage()); }
    }

    private static final class QuizChoice {
        private final String id; private final String name;
        private QuizChoice(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name + "  ·  " + id; }
    }
}

