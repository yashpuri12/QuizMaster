package com.quizmaster.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.quizmaster.model.Role;
import com.quizmaster.model.StudentProfile;
import com.quizmaster.model.QuizResult;
import com.quizmaster.service.ResultService;
import com.quizmaster.service.QuestionService;
import com.quizmaster.storage.ResultLoadResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/** Role-specific dashboard shell with clear Phase 1 placeholders. */
final class DashboardPanel extends JPanel {
    DashboardPanel(QuizMasterFrame frame, Role role, StudentProfile profile, ResultService resultService, QuestionService questionService) {
        this(frame,role,profile,resultService,questionService,role==Role.STUDENT?"OOP & Computer Graphics":"Teaching workspace");
    }
    DashboardPanel(QuizMasterFrame frame, Role role, StudentProfile profile, ResultService resultService, QuestionService questionService,String subjectName) {
        super(new BorderLayout());
        setBackground(Theme.CANVAS);
        JPanel sidebar = Ui.panel(Theme.NAVY, new BorderLayout());
        sidebar.setPreferredSize(new Dimension(224, 0));
        JPanel sideContents = Ui.panel(Theme.NAVY, new BorderLayout());
        sideContents.add(Ui.sidebarBrand(subjectName), BorderLayout.NORTH);
        JPanel nav = Ui.panel(Theme.NAVY, new java.awt.FlowLayout());
        nav.setLayout(new javax.swing.BoxLayout(nav, javax.swing.BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        String[] items = role == Role.STUDENT
                ? new String[]{"Overview", "Practice quizzes", "My attempts", "Leaderboard", "Switch subject"}
                : new String[]{"Overview", "Question bank", "Student results", "My subjects"};
        for (String item : items) {
            JButton navButton = Ui.sidebarButton("  " + item, item.equals("Overview"));
            navButton.addActionListener(e -> {
                if (role == Role.TEACHER && item.equals("Question bank")) frame.showQuestionManager();
                else if (role == Role.STUDENT && item.equals("Practice quizzes")) frame.showQuizSelection();
                else if (role == Role.STUDENT && item.equals("My attempts")) frame.showResultHistory();
                else if (role == Role.STUDENT && item.equals("Leaderboard")) frame.showLeaderboard();
                else if (role == Role.TEACHER && item.equals("Student results")) frame.showTeacherResults();
                else if (role == Role.STUDENT && item.equals("Switch subject")) frame.enterStudent();
                else if (role == Role.TEACHER && item.equals("My subjects")) frame.enterTeacher();
                else frame.showPlaceholder(item);
            });
            nav.add(navButton);
            nav.add(javax.swing.Box.createVerticalStrut(8));
        }
        nav.add(javax.swing.Box.createVerticalGlue());
        sideContents.add(nav, BorderLayout.CENTER);
        JButton logout = Ui.sidebarLogoutButton("←  Log out");
        logout.addActionListener(e -> frame.showWelcome());
        sideContents.add(logout, BorderLayout.SOUTH);
        sidebar.add(sideContents, BorderLayout.CENTER);
        add(sidebar, BorderLayout.WEST);

        JPanel content = Ui.panel(Theme.CANVAS, new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));
        String roleName = role == Role.STUDENT ? "Student" : "Teacher";
        JPanel top = Ui.panel(Theme.CANVAS, new BorderLayout());
        String heading = role == Role.STUDENT && profile != null ? "Welcome, " + profile.getDisplayName() : roleName + " overview";
        top.add(Ui.header(heading, role == Role.STUDENT
                ? (profile == null ? "Your practice space · "+subjectName : "Student ID: " + profile.getStudentId() + "  •  "+subjectName)
                : "Subject: "+subjectName+" · Manage learning content and review student progress"), BorderLayout.CENTER);
        JLabel roleChip = Ui.label(roleName.toUpperCase(), Theme.SMALL, Theme.BLUE);
        roleChip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JPanel roleHolder = Ui.panel(Theme.CANVAS, new BorderLayout());
        roleHolder.add(roleChip, BorderLayout.NORTH);
        top.add(roleHolder, BorderLayout.EAST);
        content.add(top, BorderLayout.NORTH);

        JPanel body = Ui.panel(Theme.CANVAS, new BorderLayout(0, 18));
        JPanel welcome = Ui.card();
        welcome.add(Ui.label(role == Role.STUDENT ? "Ready for your next practice session?" : "Welcome to your teaching workspace", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        welcome.add(Ui.label(role == Role.STUDENT
                ? "Your local profile is ready. Choose a syllabus unit to start a timed practice quiz."
                : "Teacher access is active. Review the saved question bank and completed student attempts.", Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
        body.add(welcome, BorderLayout.NORTH);

        JPanel cards = Ui.panel(Theme.CANVAS, new GridLayout(1, role == Role.STUDENT ? 3 : 2, 16, 0));
        ResultLoadResult loaded = null;
        try { loaded = role == Role.STUDENT ? resultService.studentHistory() : resultService.teacherResults(); }
        catch (Exception ignored) { }
        List<QuizResult> saved = loaded == null ? List.of() : loaded.getResults();
        if (role == Role.STUDENT) {
            cards.add(summaryCard("01—05", "Syllabus units", "Available units in this subject"));
            cards.add(summaryCard(Integer.toString(saved.size()), "Quizzes completed", "Submitted attempts saved locally"));
            QuizResult best = saved.stream().max(java.util.Comparator.comparing(QuizResult::getPercentage)
                    .thenComparing(QuizResult::getCompletedAt)).orElse(null);
            cards.add(summaryCard(best == null ? "—" : best.getPercentage().toPlainString() + "%", "Best percentage",
                    best == null ? "Complete a quiz to set your record" : best.getQuizName()));
        } else {
            int questionCount = 0;
            try { questionCount = questionService.listQuestions().getQuestions().size(); } catch (Exception ignored) { }
            cards.add(summaryCard(Integer.toString(questionCount), "Questions", "Saved syllabus-aligned questions"));
            cards.add(summaryCard(Integer.toString(saved.size()), "Student attempts", "" + saved.stream().map(QuizResult::getStudentId).distinct().count() + " students with saved attempts"));
        }
        JPanel centerContent = Ui.panel(Theme.CANVAS, new BorderLayout(0, 16));
        centerContent.add(cards, BorderLayout.NORTH);
        JPanel recent = Ui.card();
        if (role == Role.STUDENT) {
            recent.add(Ui.label("Recent attempts", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
            String recentText = saved.isEmpty() ? "No saved attempts yet. Your completed quizzes will appear here."
                    : saved.stream().limit(3).map(r -> r.getQuizName() + "  ·  " + r.getCorrectCount() + "/" + r.getQuestionCount() + "  ·  " + r.getPercentage().toPlainString() + "%")
                    .collect(java.util.stream.Collectors.joining("     |     "));
            recent.add(Ui.wrappedLabel(recentText, Theme.BODY, Theme.MUTED, 950), BorderLayout.CENTER);
            JButton leaderboard = Ui.button("Open leaderboard →", true); leaderboard.addActionListener(e -> frame.showLeaderboard());
            recent.add(leaderboard, BorderLayout.SOUTH);
        } else {
            recent.add(Ui.label("Saved result overview", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
            recent.add(Ui.label(saved.isEmpty() ? "No completed attempts have been saved yet." : "Latest saved attempt: " + saved.get(0).getStudentName() + " · " + saved.get(0).getQuizName() + " · " + saved.get(0).getPercentage().toPlainString() + "%", Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
            JButton resultsButton = Ui.button("View student results →", true); resultsButton.addActionListener(e -> frame.showTeacherResults()); recent.add(resultsButton, BorderLayout.SOUTH);
        }
        centerContent.add(recent, BorderLayout.CENTER);
        body.add(centerContent, BorderLayout.CENTER);

        JPanel note = Ui.panel(Theme.PALE_BLUE, new BorderLayout());
        note.setBorder(BorderFactory.createEmptyBorder(14, 17, 14, 17));
        note.add(Ui.label(role == Role.STUDENT ? "History is based on your saved attempts. Leaderboards compare attempts within one quiz." : "Results are read from saved submitted attempts. Local CSV records may be reviewed here.", Theme.SMALL, Theme.BLUE_DARK), BorderLayout.CENTER);
        body.add(note, BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel summaryCard(String value, String title, String detail) {
        JPanel card = Ui.card();
        card.add(Ui.label(value, new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 27), Theme.BLUE), BorderLayout.NORTH);
        JPanel info = Ui.panel(Color.WHITE, new BorderLayout(0, 7));
        info.add(Ui.label(title, Theme.BODY_BOLD, Theme.TEXT), BorderLayout.NORTH);
        info.add(Ui.label(detail, Theme.SMALL, Theme.MUTED), BorderLayout.CENTER);
        card.add(info, BorderLayout.CENTER);
        return card;
    }
}
