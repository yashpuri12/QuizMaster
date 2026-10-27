package com.quizmaster.ui;

import com.quizmaster.model.QuizResult;
import com.quizmaster.service.ResultService;
import com.quizmaster.storage.ResultLoadResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/** Teacher-only overview of persisted student attempts. */
final class TeacherResultsPanel extends JPanel {
    private final QuizMasterFrame frame; private final ResultService results;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Student", "Student ID", "Quiz", "Unit", "Score", "Percentage", "Completed"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel status = Ui.label("", Theme.SMALL, Theme.MUTED);
    private final CardLayout centerCards=new CardLayout();
    private final JPanel center=Ui.panel(Theme.CANVAS,centerCards);
    TeacherResultsPanel(QuizMasterFrame frame, ResultService results) {
        super(new BorderLayout()); this.frame=frame; this.results=results; setBackground(Theme.CANVAS); setBorder(BorderFactory.createEmptyBorder(28,34,26,34));
        JPanel header=Ui.panel(Theme.CANVAS,new BorderLayout()); header.add(Ui.header("Student results","Overview of completed, saved quiz attempts"),BorderLayout.CENTER);
        JButton back=Ui.button("← Teacher overview",false); back.addActionListener(e->frame.showTeacher()); header.add(Ui.headerAction(back),BorderLayout.EAST); add(header,BorderLayout.NORTH);
        JPanel body=Ui.panel(Theme.CANVAS,new BorderLayout(0,12));
        JTable table=ResultUi.table(model); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths={170,145,210,210,100,110,190}; for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        center.add(ResultUi.scroll(table),"table");
        JPanel emptyCard=Ui.card(); emptyCard.add(Ui.centeredLabel("No submitted results yet",Theme.HEADING,Theme.TEXT),BorderLayout.NORTH);
        emptyCard.add(Ui.centeredLabel("Completed student attempts will appear here when saved.",Theme.BODY,Theme.MUTED),BorderLayout.CENTER);
        center.add(Ui.centered(emptyCard),"empty");
        body.add(center,BorderLayout.CENTER); body.add(status,BorderLayout.SOUTH); add(body,BorderLayout.CENTER);
    }
    void refresh() {
        try { ResultLoadResult loaded=results.teacherResults(); model.setRowCount(0);
            for (QuizResult r:loaded.getResults()) model.addRow(new Object[]{r.getStudentName(),r.getStudentId(),r.getQuizName(),r.getUnit().getTitleForSubject(r.getSubjectId().equals(com.quizmaster.model.Subject.OOP_ID)?com.quizmaster.model.Subject.OOP_NAME:""),ResultUi.score(r),ResultUi.percentage(r),ResultUi.DATE.format(r.getCompletedAt())});
            centerCards.show(center,loaded.getResults().isEmpty()?"empty":"table");
            status.setText(loaded.getResults().size()+" saved attempt(s) from "+loaded.getResults().stream().map(QuizResult::getStudentId).distinct().count()+" student(s)."+(loaded.getIssues().isEmpty()?"":" "+loaded.getIssues().size()+" malformed row(s) skipped."));
        } catch(Exception ex){ model.setRowCount(0); status.setText("Could not load student results: "+ex.getMessage()); }
    }
}
