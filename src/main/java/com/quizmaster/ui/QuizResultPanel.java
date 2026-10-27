package com.quizmaster.ui;

import com.quizmaster.model.Question;
import com.quizmaster.model.QuizAttempt;
import com.quizmaster.model.QuizResult;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

/** Submission summary and answer-by-answer review. Correct answers appear only here. */
final class QuizResultPanel extends JPanel {
    QuizResultPanel(QuizMasterFrame frame, QuizAttempt attempt, QuizResult result) {
        super(new BorderLayout(0, 14));
        setBackground(Theme.CANVAS);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(25, 32, 22, 32));

        JPanel header = Ui.panel(Theme.CANVAS, new BorderLayout());
        header.add(Ui.header("Quiz complete", result.getQuizName() + "  •  " + result.getStudentName()), BorderLayout.CENTER);
        JPanel buttons = Ui.buttonRow();
        JButton another = Ui.button("Choose another quiz", true);
        JButton dashboard = Ui.button("Student dashboard", false);
        another.addActionListener(event -> frame.showQuizSelection());
        dashboard.addActionListener(event -> frame.showStudent());
        buttons.add(another); buttons.add(dashboard);
        header.add(buttons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = Ui.panel(Theme.CANVAS, new BorderLayout(0, 13));
        JPanel summary = Ui.panel(Theme.CANVAS, new GridLayout(1, 4, 12, 0));
        summary.add(summaryCard(result.getCorrectCount() + " / " + result.getQuestionCount(), "Score", Theme.BLUE));
        summary.add(summaryCard(result.getPercentage().toPlainString() + "%", "Percentage", Theme.BLUE));
        summary.add(summaryCard(Integer.toString(result.getIncorrectCount()), "Incorrect", new Color(184, 52, 62)));
        summary.add(summaryCard(Integer.toString(result.getUnansweredCount()), "Unanswered • zero points", Theme.MUTED));
        content.add(summary, BorderLayout.NORTH);

        JPanel reviewRows = Ui.panel(Theme.CANVAS, new GridLayout(0, 1, 0, 11));
        for (int i = 0; i < attempt.getQuestions().size(); i++) {
            Question question = attempt.getQuestions().get(i);
            Integer selected = attempt.getSelectedAnswer(question.getId());
            JPanel card = Ui.card();
            card.add(Ui.wrappedLabel("Question " + (i + 1) + "  •  " + question.getText(), Theme.BODY_BOLD, Theme.TEXT, 900), BorderLayout.NORTH);
            JPanel details = Ui.panel(Color.WHITE, new GridLayout(0, 1, 0, 6));
            String selectedText = selected == null ? "Not answered" : (char) ('A' + selected) + ". " + question.getOptions().get(selected);
            JLabel yourAnswer = Ui.label("Your answer: " + selectedText, Theme.BODY,
                    selected == null ? Theme.MUTED : (selected == question.getCorrectAnswerIndex() ? new Color(33, 134, 87) : new Color(184, 52, 62)));
            JLabel rightAnswer = Ui.label("Correct answer: " + (char) ('A' + question.getCorrectAnswerIndex()) + ". "
                    + question.getOptions().get(question.getCorrectAnswerIndex()), Theme.BODY_BOLD, new Color(33, 134, 87));
            details.add(yourAnswer);
            details.add(rightAnswer);
            if (!question.getExplanation().isBlank()) {
                details.add(Ui.wrappedLabel("Explanation: " + question.getExplanation(), Theme.SMALL, Theme.MUTED, 880));
            }
            card.add(details, BorderLayout.CENTER);
            reviewRows.add(card);
        }
        JScrollPane scroll = new JScrollPane(reviewRows);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CANVAS);
        content.add(scroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel summaryCard(String value, String label, Color color) {
        JPanel card = Ui.card();
        card.add(Ui.label(value, new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 23), color), BorderLayout.NORTH);
        card.add(Ui.label(label, Theme.SMALL, Theme.MUTED), BorderLayout.CENTER);
        return card;
    }
}
