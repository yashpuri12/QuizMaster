package com.quizmaster.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.quizmaster.model.Role;
import com.quizmaster.model.QuizAttempt;
import com.quizmaster.model.QuizResult;
import com.quizmaster.model.StudentProfile;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.security.SessionManager;
import com.quizmaster.security.TeacherCredentialStore;
import com.quizmaster.service.StudentProfileService;
import com.quizmaster.service.TeacherService;
import com.quizmaster.service.QuestionService;
import com.quizmaster.service.StudentQuizService;
import com.quizmaster.service.ResultService;
import com.quizmaster.storage.StudentProfileStore;
import com.quizmaster.storage.QuestionStore;
import com.quizmaster.storage.ResultStore;
import com.quizmaster.storage.SubjectStore;
import com.quizmaster.storage.TeacherAccountStore;
import com.quizmaster.service.SubjectService;
import com.quizmaster.ui.QuestionManagementPanel;

/** Main window, application services, and guarded screen navigation. */
public final class QuizMasterFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel screenHost = new JPanel(cards);
    private final SessionManager sessions = new SessionManager();
    private final StudentProfileStore studentStore = new StudentProfileStore(Path.of("data"));
    private final StudentProfileService studentService = new StudentProfileService(studentStore, sessions);
    private final TeacherService teacherService = new TeacherService(
            new TeacherAccountStore(Path.of("data")), studentStore, sessions);
    private final SubjectService subjectService = new SubjectService(new SubjectStore(Path.of("data")), sessions);
    private final QuestionService questionService = new QuestionService(new QuestionStore(Path.of("data")), sessions, subjectService);
    private final StudentQuizService studentQuizService = new StudentQuizService(
            new QuestionStore(Path.of("data")), new ResultStore(Path.of("data")), sessions, java.time.Clock.systemUTC(),subjectService);
    private final ResultService resultService = new ResultService(new ResultStore(Path.of("data")), sessions,subjectService);
    private final WelcomePanel welcome = new WelcomePanel(this, studentService, teacherService);
    private final SubjectSelectionPanel studentSubjects=new SubjectSelectionPanel(this,subjectService,Role.STUDENT);
    private final SubjectSelectionPanel teacherSubjects=new SubjectSelectionPanel(this,subjectService,Role.TEACHER);
    private final QuizSelectionPanel quizSelection = new QuizSelectionPanel(this, studentQuizService);
    private DashboardPanel studentDashboard;
    private DashboardPanel teacherDashboard;
    private final QuestionManagementPanel questionManager = new QuestionManagementPanel(this, questionService);
    private QuizTakingPanel activeQuizPanel;
    private QuizResultPanel quizResultPanel;
    private ResultHistoryPanel resultHistoryPanel;
    private LeaderboardPanel leaderboardPanel;
    private TeacherResultsPanel teacherResultsPanel;

    public QuizMasterFrame() {
        super("QuizMaster — SPPU Practice System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        screenHost.setBackground(Theme.CANVAS);
        screenHost.add(welcome, "welcome");
        screenHost.add(studentSubjects,"student-subjects"); screenHost.add(teacherSubjects,"teacher-subjects");
        studentDashboard = new DashboardPanel(this, Role.STUDENT, null, resultService, questionService);
        teacherDashboard = new DashboardPanel(this, Role.TEACHER, null, resultService, questionService);
        screenHost.add(studentDashboard, "student");
        screenHost.add(teacherDashboard, "teacher");
        screenHost.add(questionManager, "question-bank");
        screenHost.add(quizSelection, "quiz-selection");
        resultHistoryPanel = new ResultHistoryPanel(this, resultService);
        leaderboardPanel = new LeaderboardPanel(this, resultService);
        teacherResultsPanel = new TeacherResultsPanel(this, resultService);
        screenHost.add(resultHistoryPanel, "result-history");
        screenHost.add(leaderboardPanel, "leaderboard");
        screenHost.add(teacherResultsPanel, "teacher-results");
        setContentPane(screenHost);
        showWelcome();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { closeApplication(); }
        });
    }

    public void showWelcome() {
        sessions.clear();
        getRootPane().setDefaultButton(null);
        cards.show(screenHost, "welcome");
    }
    void changePassword(){
        Role role=sessions.current()==null?null:sessions.current().getRole();if(role==null){deny(new SecurityException("Sign in first."));return;}
        javax.swing.JPasswordField old=new javax.swing.JPasswordField(),next=new javax.swing.JPasswordField(),confirm=new javax.swing.JPasswordField();
        javax.swing.JPanel p=new javax.swing.JPanel(new java.awt.GridLayout(0,1,5,5));p.add(new javax.swing.JLabel("Current password"));p.add(old);p.add(new javax.swing.JLabel("New password (10+ characters, letter and number)"));p.add(next);p.add(new javax.swing.JLabel("Confirm new password"));p.add(confirm);
        if(javax.swing.JOptionPane.showConfirmDialog(this,p,"Change password",javax.swing.JOptionPane.OK_CANCEL_OPTION,javax.swing.JOptionPane.PLAIN_MESSAGE)==javax.swing.JOptionPane.OK_OPTION){char[] a=old.getPassword(),b=next.getPassword(),c=confirm.getPassword();try{if(role==Role.STUDENT)studentService.changePassword(a,b,c);else teacherService.changePassword(a,b,c);javax.swing.JOptionPane.showMessageDialog(this,"Password updated.","Account",javax.swing.JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(this,ex.getMessage(),"Password not changed",javax.swing.JOptionPane.ERROR_MESSAGE);}finally{java.util.Arrays.fill(a,'\0');java.util.Arrays.fill(b,'\0');java.util.Arrays.fill(c,'\0');}}
    }
    void registerTeacherAccount(){
        try{
            sessions.require(Role.TEACHER);
            String code=teacherService.createTeacherRegistrationCode();
            javax.swing.JPanel panel=new javax.swing.JPanel(new java.awt.BorderLayout(0,10));
            panel.add(new javax.swing.JLabel("Share this one-time code with the new teacher. It remains valid until used."),java.awt.BorderLayout.NORTH);
            javax.swing.JTextField codeField=new javax.swing.JTextField(code,36);
            codeField.setEditable(false);
            codeField.setFont(new java.awt.Font(java.awt.Font.MONOSPACED,java.awt.Font.BOLD,15));
            codeField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
            codeField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(Theme.BORDER),
                    javax.swing.BorderFactory.createEmptyBorder(8,10,8,10)));
            panel.add(codeField,java.awt.BorderLayout.CENTER);
            javax.swing.JButton copy=Ui.button("Copy code",true);
            javax.swing.JLabel feedback=Ui.label("",Theme.SMALL,Theme.MUTED);
            copy.addActionListener(event->{
                try{java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(code),null);feedback.setText("Invitation code copied.");}
                catch(java.awt.HeadlessException|IllegalStateException ex){feedback.setText("Clipboard is unavailable. Select and copy the code above.");}
            });
            javax.swing.JPanel actions=Ui.panel(Theme.CANVAS,new java.awt.BorderLayout(10,0));
            actions.add(feedback,java.awt.BorderLayout.CENTER);actions.add(copy,java.awt.BorderLayout.EAST);
            panel.add(actions,java.awt.BorderLayout.SOUTH);
            javax.swing.JOptionPane.showMessageDialog(this,panel,"Generate Teacher Invitation Code",javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(this,ex.getMessage(),"Teacher invitation",javax.swing.JOptionPane.ERROR_MESSAGE);}
    }

    public void enterStudent() {
        try { var session=sessions.require(Role.STUDENT);session.clearSubject();studentSubjects.refresh();cards.show(screenHost,NavigationPolicy.afterLogin(Role.STUDENT)); }
        catch(SecurityException ex){deny(ex);}
    }
    public void enterStudent(StudentProfile profile) {
        // Compatibility entry point: only an already authenticated matching identity is accepted.
        if(sessions.current()==null||sessions.current().getRole()!=Role.STUDENT||!sessions.current().getStudent().getStudentId().equalsIgnoreCase(profile.getStudentId())){deny(new SecurityException("Sign in with the student password first."));return;}
        enterStudent();
    }

    public void enterTeacher() {
        try {
            // A protected service call keeps role enforcement outside the UI layer.
            teacherService.listStudentProfiles();sessions.require(Role.TEACHER).clearSubject();
            teacherSubjects.refresh(); cards.show(screenHost,NavigationPolicy.afterLogin(Role.TEACHER));
        } catch (SecurityException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Access denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            showWelcome();
        } catch (java.io.IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not load local student data: " + ex.getMessage(), "Storage error", javax.swing.JOptionPane.ERROR_MESSAGE);
            showWelcome();
        }
    }

    public void showStudent() {
        try { var session=sessions.require(Role.STUDENT);var subject=subjectService.requireSelectedOwnedByActiveSession();screenHost.remove(studentDashboard);studentDashboard=new DashboardPanel(this,Role.STUDENT,session.getStudent(),resultService,questionService,subject.getName());screenHost.add(studentDashboard,"student");cards.show(screenHost, "student"); }
        catch (SecurityException ex) { deny(ex); }
        catch (java.io.IOException ex){deny(new SecurityException("Could not load the selected subject."));}
    }

    public void showTeacher() {
        try { teacherService.listStudentProfiles();var subject=subjectService.requireSelectedOwnedByActiveSession();screenHost.remove(teacherDashboard);teacherDashboard=new DashboardPanel(this,Role.TEACHER,null,resultService,questionService,subject.getName());screenHost.add(teacherDashboard,"teacher");cards.show(screenHost, "teacher"); }
        catch (SecurityException ex) { deny(ex); }
        catch (java.io.IOException ex) { deny(new SecurityException("Could not access teacher workspace data.")); }
    }

    public void showQuestionManager() {
        try {
            sessions.require(Role.TEACHER);
            subjectService.requireSelectedOwnedByActiveSession();
            questionManager.refreshQuestions();
            cards.show(screenHost, "question-bank");
        } catch (SecurityException ex) {
            deny(ex);
        } catch (java.io.IOException ex) { deny(new SecurityException("Could not load the selected subject.")); }
    }

    public void showQuizSelection() {
        try {
            sessions.require(Role.STUDENT);
            subjectService.requireSelectedOwnedByActiveSession();
            quizSelection.refreshQuizzes();
            cards.show(screenHost, "quiz-selection");
        } catch (SecurityException ex) {
            deny(ex);
        } catch (java.io.IOException ex) { deny(new SecurityException("Could not load the selected subject.")); }
    }

    public void showResultHistory() {
        try { sessions.require(Role.STUDENT);subjectService.requireSelectedOwnedByActiveSession(); resultHistoryPanel.refresh(); cards.show(screenHost, "result-history"); }
        catch (SecurityException ex) { deny(ex); }
        catch (java.io.IOException ex){deny(new SecurityException("Could not load the selected subject."));}
    }

    public void showLeaderboard() {
        try { sessions.require(Role.STUDENT);subjectService.requireSelectedOwnedByActiveSession(); leaderboardPanel.refresh(); cards.show(screenHost, "leaderboard"); }
        catch (SecurityException ex) { deny(ex); }
        catch (java.io.IOException ex){deny(new SecurityException("Could not load the selected subject."));}
    }

    public void showTeacherResults() {
        try { subjectService.requireSelectedOwnedByActiveSession();resultService.teacherResults(); teacherResultsPanel.refresh(); cards.show(screenHost, "teacher-results"); }
        catch (SecurityException ex) { deny(ex); }
        catch (java.io.IOException ex) { javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Results unavailable", javax.swing.JOptionPane.ERROR_MESSAGE); }
    }

    public void startQuiz(SyllabusUnit unit) {
        try {
            sessions.require(Role.STUDENT);
            QuizAttempt attempt = studentQuizService.startQuiz(unit);
            if (activeQuizPanel != null) {
                activeQuizPanel.stopTimer();
                screenHost.remove(activeQuizPanel);
            }
            activeQuizPanel = new QuizTakingPanel(this, attempt, this::completeQuiz, this::abandonQuiz);
            screenHost.add(activeQuizPanel, "quiz-taking");
            cards.show(screenHost, "quiz-taking");
        } catch (SecurityException ex) {
            deny(ex);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Could not start quiz", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completeQuiz(QuizAttempt attempt) throws Exception {
        QuizResult result = studentQuizService.submitAttempt(attempt);
        if (activeQuizPanel != null) activeQuizPanel.stopTimer();
        if (activeQuizPanel != null) screenHost.remove(activeQuizPanel);
        activeQuizPanel = null;
        if (quizResultPanel != null) screenHost.remove(quizResultPanel);
        quizResultPanel = new QuizResultPanel(this, attempt, result);
        screenHost.add(quizResultPanel, "quiz-review");
        if (resultHistoryPanel != null) resultHistoryPanel.refresh();
        if (studentDashboard != null) {
            screenHost.remove(studentDashboard);
            studentDashboard = new DashboardPanel(this, Role.STUDENT, sessions.current().getStudent(), resultService, questionService,subjectService.requireSelectedOwnedByActiveSession().getName());
            screenHost.add(studentDashboard, "student");
        }
        cards.show(screenHost, "quiz-review");
    }

    private void abandonQuiz() {
        if (activeQuizPanel == null) return;
        studentQuizService.abandonAttempt(activeQuizPanel.getAttempt());
        activeQuizPanel.stopTimer();
        screenHost.remove(activeQuizPanel);
        activeQuizPanel = null;
        showQuizSelection();
    }

    private void closeApplication() {
        QuizTakingPanel closingQuiz = activeQuizPanel;
        if (closingQuiz != null && closingQuiz.hasUnsubmittedAttempt()) {
            int answer = javax.swing.JOptionPane.showConfirmDialog(this,
                    "An unfinished quiz will be abandoned and no result will be saved. Close QuizMaster?",
                    "Close during a quiz", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
            if (answer != javax.swing.JOptionPane.YES_OPTION) return;
            if (activeQuizPanel == closingQuiz) {
                closingQuiz.stopTimer();
                activeQuizPanel = null;
            }
        }
        sessions.clear();
        dispose();
    }

    private void deny(SecurityException ex) {
        javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Access denied", javax.swing.JOptionPane.ERROR_MESSAGE);
        showWelcome();
    }

    public void showPlaceholder(String feature) {
        javax.swing.JOptionPane.showMessageDialog(this,
                feature + " will be available in a later development phase.",
                "Coming in a later phase", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
}
