package com.quizmaster.ui;

import com.quizmaster.model.QuizResult;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Shared formatting and read-only result table helpers. */
final class ResultUi {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());
    private ResultUi() { }

    static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return scroll;
    }

    static String score(QuizResult result) {
        return result.getCorrectCount() + " / " + result.getQuestionCount();
    }

    static String percentage(QuizResult result) { return result.getPercentage().toPlainString() + "%"; }

    static void showDetails(java.awt.Component parent, QuizResult result) {
        String message = "Quiz: " + result.getQuizName() + "\n"
                + "Unit: " + result.getUnit().getTitleForSubject(result.getSubjectId().equals(com.quizmaster.model.Subject.OOP_ID)?com.quizmaster.model.Subject.OOP_NAME:"") + "\n"
                + "Score: " + score(result) + " correct\n"
                + "Incorrect: " + result.getIncorrectCount() + "\n"
                + "Unanswered: " + result.getUnansweredCount() + "\n"
                + "Percentage: " + percentage(result) + "\n"
                + "Completed: " + DATE.format(result.getCompletedAt()) + "\n\n"
                + "Answer selections and explanations were not saved with this attempt, so question-by-question review is unavailable. "
                + "This summary uses the values saved when the attempt was submitted.";
        JTextArea details = new JTextArea(message, 11, 58);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(Theme.BODY);
        details.setBackground(Theme.CANVAS);
        details.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JOptionPane.showMessageDialog(parent, new JScrollPane(details), "Saved attempt details", JOptionPane.INFORMATION_MESSAGE);
    }
}
