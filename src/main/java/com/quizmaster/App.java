package com.quizmaster;

import com.quizmaster.ui.QuizMasterFrame;
import com.quizmaster.ui.Ui;
import com.quizmaster.service.StarterQuestionBank;
import com.quizmaster.storage.QuestionStore;
import com.quizmaster.storage.StudentProfileStore;
import com.quizmaster.storage.TeacherAccountStore;
import com.quizmaster.storage.SubjectStore;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;

/** Starts the QuizMaster desktop application. */
public final class App {
    private App() { }

    public static void main(String[] args) {
        try {
            new StudentProfileStore(Path.of("data")).ensureDemoAccount("STU1001","OOP Demo Student","Student@123".toCharArray());
            new TeacherAccountStore(Path.of("data")).ensureDemoAccount("TCH1001","OOP Demo Teacher","Teacher@123".toCharArray());
            new SubjectStore(Path.of("data")).ensureOopSubject("TCH1001");
            new QuestionStore(Path.of("data")).initializeIfEmpty(StarterQuestionBank.questions());
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Question bank initialization skipped: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
            Ui.installLookAndFeel();
            new QuizMasterFrame().setVisible(true);
        });
    }
}
