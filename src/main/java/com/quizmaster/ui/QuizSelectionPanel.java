package com.quizmaster.ui;

import com.quizmaster.model.UnitQuiz;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.service.StudentQuizService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

/** Student screen that shows live question counts for the five unit quizzes. */
final class QuizSelectionPanel extends JPanel {
    private final QuizMasterFrame frame;
    private final StudentQuizService quizzes;
    private final JPanel quizCards = Ui.panel(Theme.CANVAS, new GridLayout(0, 2, 16, 16));
    private final JLabel status = Ui.label("Choose a syllabus unit to practice.", Theme.BODY, Theme.MUTED);

    QuizSelectionPanel(QuizMasterFrame frame, StudentQuizService quizzes) {
        super(new BorderLayout(0, 14));
        this.frame = frame;
        this.quizzes = quizzes;
        setBackground(Theme.CANVAS);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(28, 34, 26, 34));

        JPanel header = Ui.panel(Theme.CANVAS, new BorderLayout());
        header.add(Ui.header("Choose a practice quiz", "Question counts come from the current teacher-managed question bank"), BorderLayout.CENTER);
        JButton back = Ui.button("← Student dashboard", false);
        back.addActionListener(event -> frame.showStudent());
        header.add(Ui.headerAction(back), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(quizCards);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CANVAS);
        add(scroll, BorderLayout.CENTER);
        status.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 2, 0, 2));
        add(status, BorderLayout.SOUTH);
    }

    void refreshQuizzes() {
        quizCards.removeAll();
        try {
            for (UnitQuiz quiz : quizzes.availableQuizzes()) quizCards.add(createCard(quiz));
            status.setText("All questions available in a unit are included. Question order is kept stable.");
            status.setForeground(Theme.MUTED);
        } catch (Exception ex) {
            status.setText("Could not load the question bank: " + ex.getMessage());
            status.setForeground(new Color(184, 52, 62));
            for (SyllabusUnit unit : SyllabusUnit.values()) quizCards.add(createErrorCard(unit));
        }
        quizCards.revalidate();
        quizCards.repaint();
    }

    private JPanel createCard(UnitQuiz quiz) {
        JPanel card = Ui.card();
        card.add(Ui.wrappedLabel(quiz.getUnit().getTitleForSubject(quiz.getSubjectName()), Theme.HEADING, Theme.TEXT, 400), BorderLayout.NORTH);
        JPanel description = Ui.panel(Color.WHITE, new BorderLayout(0, 7));
        description.add(Ui.label(quiz.getSubjectName(), Theme.BODY_BOLD, Theme.NAVY), BorderLayout.NORTH);
        description.add(Ui.label(quiz.getQuizId(), Theme.SMALL, Theme.BLUE), BorderLayout.CENTER);
        description.add(Ui.label(quiz.getQuestionCount() + (quiz.getQuestionCount() == 1 ? " question available" : " questions available"),
                Theme.BODY, Theme.MUTED), BorderLayout.SOUTH);
        card.add(description, BorderLayout.CENTER);
        JButton start = Ui.button(quiz.getQuestionCount() == 0 ? "No questions yet" : "Start unit quiz  →", quiz.getQuestionCount() > 0);
        start.setEnabled(quiz.getQuestionCount() > 0);
        if (quiz.getQuestionCount() == 0) start.setToolTipText("A teacher must add questions for this syllabus unit first.");
        else start.addActionListener(event -> frame.startQuiz(quiz.getUnit()));
        card.add(start, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createErrorCard(SyllabusUnit unit) {
        JPanel card = Ui.card();
        card.add(Ui.wrappedLabel(unit.getGenericTitle(), Theme.HEADING, Theme.TEXT, 400), BorderLayout.NORTH);
        card.add(Ui.label("Question data could not be loaded.", Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
        JButton disabled = Ui.button("Unavailable", false);
        disabled.setEnabled(false);
        card.add(disabled, BorderLayout.SOUTH);
        return card;
    }
}
