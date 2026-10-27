package com.quizmaster.ui;

import com.quizmaster.model.Question;
import com.quizmaster.model.QuizAttempt;
import com.quizmaster.model.QuizResult;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One-question-at-a-time quiz screen with a Swing countdown timer. */
final class QuizTakingPanel extends JPanel {
    private final QuizMasterFrame frame;
    private final QuizAttempt attempt;
    private final SubmitHandler submitHandler;
    private final Runnable abandonHandler;
    private final JLabel timerLabel = Ui.label("", Theme.HEADING, Theme.BLUE);
    private final JLabel progressLabel = Ui.label("", Theme.BODY_BOLD, Theme.TEXT);
    private final JLabel answeredLabel = Ui.label("", Theme.SMALL, Theme.MUTED);
    private final JProgressBar progress = new JProgressBar();
    private final JLabel topic = Ui.label("", Theme.SMALL, Theme.BLUE);
    private final JLabel prompt = Ui.wrappedLabel("", Theme.HEADING, Theme.TEXT, 760);
    private final JRadioButton[] answerButtons = new JRadioButton[4];
    private final JPanel[] optionCards = new JPanel[4];
    private final ButtonGroup answerGroup = new ButtonGroup();
    private final JPanel navigator = Ui.panel(Color.WHITE, new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JLabel message = Ui.label("", Theme.SMALL, new Color(184, 52, 62));
    private final Timer swingTimer;
    private JButton previousButton;
    private JButton nextButton;
    private boolean submissionStarted;

    QuizTakingPanel(QuizMasterFrame frame, QuizAttempt attempt, SubmitHandler submitHandler, Runnable abandonHandler) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        this.attempt = attempt;
        this.submitHandler = submitHandler;
        this.abandonHandler = abandonHandler;
        setBackground(Theme.CANVAS);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(25, 32, 24, 32));

        JPanel header = Ui.panel(Theme.CANVAS, new BorderLayout(12, 4));
        JPanel titleBlock = Ui.panel(Theme.CANVAS, new BorderLayout(0, 4));
        titleBlock.add(Ui.label(attempt.getQuiz().getQuizName(), Theme.TITLE, Theme.NAVY), BorderLayout.NORTH);
        titleBlock.add(Ui.label(attempt.getQuiz().getQuizId(), Theme.SMALL, Theme.MUTED), BorderLayout.CENTER);
        header.add(titleBlock, BorderLayout.CENTER);
        timerLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Theme.BORDER),
                javax.swing.BorderFactory.createEmptyBorder(9, 14, 9, 14)));
        header.add(timerLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = Ui.panel(Theme.CANVAS, new BorderLayout(0, 14));
        JPanel progressArea = Ui.panel(Theme.CANVAS, new BorderLayout(0, 7));
        JPanel progressHead = Ui.panel(Theme.CANVAS, new BorderLayout());
        progressHead.add(progressLabel, BorderLayout.WEST);
        progressHead.add(answeredLabel, BorderLayout.EAST);
        progressArea.add(progressHead, BorderLayout.NORTH);
        progress.setMinimum(0);
        progress.setMaximum(attempt.getQuestionCount());
        progress.setStringPainted(false);
        progress.setForeground(Theme.BLUE);
        progress.setBackground(Theme.BORDER);
        progress.setBorderPainted(false);
        progress.setUI(new BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return Theme.BLUE; }
            @Override protected Color getSelectionForeground() { return Theme.BLUE; }
        });
        progressArea.add(progress, BorderLayout.CENTER);
        body.add(progressArea, BorderLayout.NORTH);

        JPanel questionCard = Ui.card();
        questionCard.add(topic, BorderLayout.NORTH);
        questionCard.add(prompt, BorderLayout.CENTER);
        JPanel options = Ui.panel(Color.WHITE, new GridLayout(4, 1, 0, 8));
        for (int i = 0; i < answerButtons.length; i++) {
            final int optionIndex = i;
            JRadioButton button = new JRadioButton();
            button.setFont(Theme.BODY);
            button.setForeground(Theme.TEXT);
            button.setBackground(Color.WHITE);
            button.setFocusPainted(true);
            button.setOpaque(false);
            button.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10));
            button.addActionListener(event -> {
                attempt.selectAnswer(attempt.getCurrentQuestion().getId(), optionIndex);
                refreshOptionCards(optionIndex);
                refreshNavigator();
                updateAnsweredLabel();
                message.setText(" ");
            });
            answerButtons[i] = button;
            answerGroup.add(button);
            JPanel optionCard = new JPanel(new BorderLayout());
            optionCard.setBackground(Color.WHITE);
            optionCard.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(Theme.BORDER),
                    javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5)));
            optionCard.add(button, BorderLayout.CENTER);
            optionCard.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent event) { button.doClick(); }
            });
            optionCards[i] = optionCard;
            options.add(optionCard);
        }
        JPanel questionContent = Ui.panel(Color.WHITE, new BorderLayout(0, 17));
        questionContent.add(questionCard, BorderLayout.NORTH);
        questionContent.add(options, BorderLayout.CENTER);
        JScrollPane questionScroll = new JScrollPane(questionContent);
        questionScroll.setBorder(null);
        questionScroll.getViewport().setBackground(Theme.CANVAS);
        body.add(questionScroll, BorderLayout.CENTER);

        JPanel bottom = Ui.panel(Theme.CANVAS, new BorderLayout(0, 9));
        JScrollPane navScroll = new JScrollPane(navigator);
        navScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Question navigator • filled numbers are answered"));
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        navScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        navScroll.setPreferredSize(new java.awt.Dimension(0, 74));
        bottom.add(navScroll, BorderLayout.NORTH);
        JPanel actions = Ui.panel(Theme.CANVAS, new BorderLayout());
        actions.add(message, BorderLayout.CENTER);
        JPanel actionButtons = Ui.panel(Theme.CANVAS, new GridLayout(1, 4, 7, 0));
        JButton abandon = Ui.button("Exit quiz", false);
        previousButton = Ui.button("← Previous", false);
        nextButton = Ui.button("Next →", false);
        JButton submit = Ui.button("Submit quiz", true);
        abandon.addActionListener(event -> confirmAbandon());
        previousButton.addActionListener(event -> { attempt.previousQuestion(); refreshQuestion(); });
        nextButton.addActionListener(event -> { attempt.nextQuestion(); refreshQuestion(); });
        submit.addActionListener(event -> submit(false));
        actionButtons.add(abandon); actionButtons.add(previousButton); actionButtons.add(nextButton); actionButtons.add(submit);
        actions.add(actionButtons, BorderLayout.EAST);
        bottom.add(actions, BorderLayout.SOUTH);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        swingTimer = new Timer(1000, event -> updateTimer());
        swingTimer.setCoalesce(true);
        refreshQuestion();
        updateTimer();
        swingTimer.start();
    }

    boolean hasUnsubmittedAttempt() { return !attempt.isSubmitted(); }
    QuizAttempt getAttempt() { return attempt; }
    void stopTimer() { swingTimer.stop(); }

    private void refreshQuestion() {
        Question question = attempt.getCurrentQuestion();
        progressLabel.setText("Question " + (attempt.getCurrentIndex() + 1) + " of " + attempt.getQuestionCount());
        progress.setValue(attempt.getCurrentIndex() + 1);
        topic.setText(question.getUnit().getTitleForSubject(attempt.getQuiz().getSubjectName()) + "  •  " + question.getTopic());
        prompt.setText(asHtml(question.getText()));
        answerGroup.clearSelection();
        Integer selected = attempt.getSelectedAnswer(question.getId());
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(asHtml((char) ('A' + i) + ".  " + question.getOptions().get(i)));
            boolean isSelected = selected != null && selected == i;
            answerButtons[i].setSelected(isSelected);
            optionCards[i].setBackground(isSelected ? Theme.PALE_BLUE : Color.WHITE);
            optionCards[i].setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(isSelected ? Theme.BLUE : Theme.BORDER, isSelected ? 2 : 1),
                    javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        }
        previousButton.setEnabled(attempt.getCurrentIndex() > 0);
        nextButton.setEnabled(attempt.getCurrentIndex() < attempt.getQuestionCount() - 1);
        updateAnsweredLabel();
        refreshNavigator();
    }

    private void updateAnsweredLabel() {
        answeredLabel.setText(attempt.getSelectedAnswers().size() + " of " + attempt.getQuestionCount() + " answered");
    }

    private void refreshNavigator() {
        navigator.removeAll();
        for (int i = 0; i < attempt.getQuestionCount(); i++) {
            final int target = i;
            Question question = attempt.getQuestions().get(i);
            JButton number = new JButton(Integer.toString(i + 1));
            number.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            number.setFont(Theme.SMALL);
            number.setFocusPainted(true);
            boolean answered = attempt.getSelectedAnswer(question.getId()) != null;
            boolean current = i == attempt.getCurrentIndex();
            number.setBackground(current ? Theme.BLUE : (answered ? Theme.PALE_BLUE : Color.WHITE));
            number.setForeground(current ? Color.WHITE : Theme.TEXT);
            number.setOpaque(true);
            number.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(current ? Theme.BLUE : Theme.BORDER),
                    javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            number.setPreferredSize(new java.awt.Dimension(38, 34));
            number.addActionListener(event -> { attempt.goToQuestion(target); refreshQuestion(); });
            navigator.add(number);
        }
        navigator.revalidate();
        navigator.repaint();
    }

    private void refreshOptionCards(int selectedIndex) {
        for (int i = 0; i < optionCards.length; i++) {
            boolean selected = i == selectedIndex;
            optionCards[i].setBackground(selected ? Theme.PALE_BLUE : Color.WHITE);
            optionCards[i].setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(selected ? Theme.BLUE : Theme.BORDER, selected ? 2 : 1),
                    javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        }
    }

    private void updateTimer() {
        long seconds = attempt.secondsRemaining(Instant.now());
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        timerLabel.setText(String.format(java.util.Locale.ROOT, "%02d:%02d remaining", minutes, remainder));
        timerLabel.setForeground(seconds <= 30 ? new Color(184, 52, 62) : Theme.BLUE);
        if (seconds == 0 && !submissionStarted && !attempt.isSubmitted()) submit(true);
    }

    private void submit(boolean automatic) {
        if (submissionStarted || attempt.isSubmitted()) return;
        if (!automatic) {
            Object[] choices = {"Continue quiz", "Submit quiz"};
            int answer = JOptionPane.showOptionDialog(frame,
                    "Submit this quiz now? Unanswered questions will score zero.",
                    "Confirm submission", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
            if (answer != 1) return;
        }
        submissionStarted = true;
        swingTimer.stop();
        try {
            submitHandler.submit(attempt);
        } catch (Exception ex) {
            submissionStarted = false;
            message.setText("Could not save the submitted quiz: " + ex.getMessage());
            if (attempt.secondsRemaining(Instant.now()) > 0) swingTimer.start();
        }
    }

    private void confirmAbandon() {
        Object[] choices = {"Stay in quiz", "Exit quiz"};
        int answer = JOptionPane.showOptionDialog(frame,
                "Exit this quiz? The unfinished attempt will not be saved.",
                "Leave quiz", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[0]);
        if (answer != 1) return;
        swingTimer.stop();
        try { abandonHandler.run(); }
        catch (RuntimeException ex) { JOptionPane.showMessageDialog(frame, ex.getMessage(), "Quiz", JOptionPane.ERROR_MESSAGE); }
    }

    private static String asHtml(String text) {
        int limit = 64;
        StringBuilder html = new StringBuilder("<html>");
        int length = 0;
        for (String word : text.split("\\s+")) {
            if (length > 0 && length + word.length() + 1 > limit) {
                html.append("<br>");
                length = 0;
            } else if (length > 0) {
                html.append(' ');
                length++;
            }
            html.append(word.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
            length += word.length();
        }
        return html.append("</html>").toString();
    }

    @FunctionalInterface interface SubmitHandler { void submit(QuizAttempt attempt) throws Exception; }
}
