package com.quizmaster.ui;

import com.quizmaster.model.Role;
import com.quizmaster.model.Subject;
import com.quizmaster.service.SubjectService;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Student subject picker and teacher-owned subject workspace entry point. */
final class SubjectSelectionPanel extends JPanel {
    private final QuizMasterFrame frame;private final SubjectService service;private final Role role;
    private final JPanel cards=new JPanel(new GridLayout(0,2,14,14));private final JLabel status=Ui.label(" ",Theme.SMALL,Theme.MUTED);
    SubjectSelectionPanel(QuizMasterFrame frame,SubjectService service,Role role){
        super(new BorderLayout(0,18));this.frame=frame;this.service=service;this.role=role;setBackground(Theme.CANVAS);setBorder(BorderFactory.createEmptyBorder(30,36,30,36));
        JPanel head=Ui.panel(Theme.CANVAS,new BorderLayout(0,7));head.add(Ui.label(role==Role.STUDENT?"Choose a subject":"Your teaching subjects",Theme.TITLE,Theme.NAVY),BorderLayout.NORTH);
        head.add(Ui.label(role==Role.STUDENT?"Select an active subject to continue. You can switch subjects later.":"Select a subject you own, or create one to begin.",Theme.BODY,Theme.MUTED),BorderLayout.CENTER);add(head,BorderLayout.NORTH);
        cards.setOpaque(false);JScrollPane scroll=new JScrollPane(cards);scroll.setBorder(null);scroll.getViewport().setBackground(Theme.CANVAS);add(scroll,BorderLayout.CENTER);
        JPanel foot=Ui.panel(Theme.CANVAS,new BorderLayout(10,0));foot.add(status,BorderLayout.CENTER);JPanel actions=Ui.buttonRow();
        JButton password=Ui.button("Change password",false);password.addActionListener(e->frame.changePassword());actions.add(password);
        if(role==Role.TEACHER){JButton invite=Ui.button("Generate Teacher Invitation Code",false);invite.setToolTipText("Create a one-time code for a new teacher account");invite.addActionListener(e->frame.registerTeacherAccount());JButton create=Ui.button("＋ Create subject",true);create.addActionListener(e->createSubject());actions.add(invite);actions.add(create);}
        JButton logout=Ui.button("Log out",false);logout.addActionListener(e->frame.showWelcome());actions.add(logout);foot.add(actions,BorderLayout.EAST);add(foot,BorderLayout.SOUTH);
    }
    void refresh(){cards.removeAll();try{List<Subject> list=role==Role.STUDENT?service.activeForStudent():service.ownedByTeacher();for(Subject subject:list)cards.add(subjectCard(subject));if(list.isEmpty())status.setText(role==Role.TEACHER?"No subjects yet. Create your first subject to start teaching.":"No active subjects are available yet. Please check again later.");else status.setText(" ");}catch(Exception ex){status.setText("Subjects could not be loaded: "+ex.getMessage());}cards.revalidate();cards.repaint();}
    private JPanel subjectCard(Subject subject){JPanel card=Ui.card();card.setPreferredSize(new Dimension(350,175));card.add(Ui.label(subject.getName(),Theme.HEADING,Theme.NAVY),BorderLayout.NORTH);card.add(Ui.wrappedLabel(subject.getDescription().isBlank()?"No description provided.":subject.getDescription(),Theme.BODY,Theme.MUTED,300),BorderLayout.CENTER);JButton open=Ui.button(role==Role.STUDENT?"Select subject →":"Open teaching workspace →",true);open.addActionListener(e->{try{service.select(subject.getId());if(role==Role.STUDENT)frame.showStudent();else frame.showTeacher();}catch(Exception ex){error(ex.getMessage());}});card.add(open,BorderLayout.SOUTH);return card;}
    private void createSubject(){JTextField name=new JTextField(),description=new JTextField();JPanel p=new JPanel(new GridLayout(0,1,6,6));p.add(new JLabel("Subject name"));p.add(name);p.add(new JLabel("Optional description"));p.add(description);if(JOptionPane.showConfirmDialog(this,p,"Create a subject",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)try{service.createOwned(name.getText(),description.getText());refresh();}catch(Exception ex){error(ex.getMessage());}}
    private void error(String text){JOptionPane.showMessageDialog(this,text,"Subject action",JOptionPane.ERROR_MESSAGE);}
}
