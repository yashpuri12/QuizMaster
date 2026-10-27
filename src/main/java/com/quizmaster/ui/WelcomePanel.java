package com.quizmaster.ui;

import com.quizmaster.model.StudentProfile;
import com.quizmaster.model.TeacherProfile;
import com.quizmaster.service.StudentProfileService;
import com.quizmaster.service.TeacherService;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;

/** Login-first welcome flow with separate, policy-aware registration screens. */
final class WelcomePanel extends JPanel {
    private final QuizMasterFrame frame;
    private final StudentProfileService students;
    private final TeacherService teachers;
    private final CardLayout layout = new CardLayout();
    private final JPanel views = new JPanel(layout);

    WelcomePanel(QuizMasterFrame frame, StudentProfileService students, TeacherService teachers) {
        super(new BorderLayout()); this.frame=frame; this.students=students; this.teachers=teachers;
        setBackground(Theme.CANVAS);
        views.add(home(),NavigationPolicy.WELCOME); views.add(loginForm(false),NavigationPolicy.STUDENT_LOGIN);
        views.add(loginForm(true),NavigationPolicy.TEACHER_LOGIN); views.add(studentRegistration(),NavigationPolicy.STUDENT_REGISTER);
        views.add(teacherRegistration(),NavigationPolicy.TEACHER_REGISTER); views.add(legacyClaim(),NavigationPolicy.LEGACY_CLAIM);
        add(views,BorderLayout.CENTER); layout.show(views,NavigationPolicy.WELCOME);
    }

    private JPanel home() {
        JPanel shell=Ui.panel(Theme.CANVAS,new GridBagLayout()); JPanel card=Ui.card();
        card.setPreferredSize(new Dimension(620,460));
        JPanel hero=Ui.panel(Color.WHITE,new BorderLayout(0,12));
        hero.add(Ui.label("SPPU • COMPUTER ENGINEERING",Theme.BODY_BOLD,Theme.BLUE),BorderLayout.NORTH);
        hero.add(Ui.label("Practice with purpose.",new Font("Segoe UI",Font.BOLD,36),Theme.NAVY),BorderLayout.CENTER);
        hero.add(Ui.label("SPPU OOP & Computer Graphics Practice System",Theme.BODY,Theme.MUTED),BorderLayout.SOUTH);
        card.add(hero,BorderLayout.NORTH);
        JPanel actions=Ui.panel(Color.WHITE,new GridLayout(2,1,0,12));
        JButton student=Ui.button("Student Login",true), teacher=Ui.button("Teacher Login",false);
        student.addActionListener(e->show(NavigationPolicy.loginFor(com.quizmaster.model.Role.STUDENT))); teacher.addActionListener(e->show(NavigationPolicy.loginFor(com.quizmaster.model.Role.TEACHER)));
        actions.add(student);actions.add(teacher);card.add(actions,BorderLayout.CENTER);
        card.add(Ui.label("Local desktop learning companion · Java 17",Theme.SMALL,Theme.MUTED),BorderLayout.SOUTH);
        shell.add(card);return shell;
    }

    private JPanel loginForm(boolean teacher) {
        JPanel shell=Ui.panel(Theme.CANVAS,new GridBagLayout());JPanel card=Ui.card();
        card.setPreferredSize(new Dimension(520,460));String role=teacher?"Teacher":"Student";
        card.add(Ui.label(role+" Login",Theme.TITLE,Theme.NAVY),BorderLayout.NORTH);
        JTextField id=new JTextField(24);JPasswordField password=new JPasswordField(24);
        Ui.styleInput(id);Ui.styleInput(password);JLabel status=Ui.label(" ",Theme.SMALL,Theme.MUTED);
        JPanel form=Ui.panel(Color.WHITE,new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(7,0,7,0);
        c.gridy=0;form.add(Ui.label(role+" ID",Theme.BODY_BOLD,Theme.TEXT),c);c.gridy=1;form.add(id,c);
        c.gridy=2;form.add(Ui.label("Password",Theme.BODY_BOLD,Theme.TEXT),c);c.gridy=3;form.add(password,c);
        JButton signIn=Ui.button("Sign In",true);c.gridy=4;c.insets=new Insets(14,0,6,0);form.add(signIn,c);
        signIn.addActionListener(e->{char[] secret=password.getPassword();try{if(teacher){if(teachers.login(id.getText(),secret))frame.enterTeacher();else showError(status,"Teacher ID or password is incorrect.");}else if(students.login(id.getText(),secret))frame.enterStudent();else showError(status,"Student ID or password is incorrect.");}catch(Exception ex){showError(status,ex.getMessage());}finally{Arrays.fill(secret,'\0');password.setText("");}});
        JButton registration=Ui.link(teacher?"New teacher? Register":"New student? Create account");
        c.gridy=5;c.insets=new Insets(5,0,5,0);form.add(registration,c);
        registration.addActionListener(e->show(NavigationPolicy.registrationFor(teacher?com.quizmaster.model.Role.TEACHER:com.quizmaster.model.Role.STUDENT)));
        card.add(form,BorderLayout.CENTER);
        JPanel bottom=Ui.panel(Color.WHITE,new BorderLayout(0,8));bottom.add(status,BorderLayout.CENTER);
        JButton back=Ui.button("← Welcome",false);back.addActionListener(e->show(NavigationPolicy.WELCOME));bottom.add(back,BorderLayout.SOUTH);
        card.add(bottom,BorderLayout.SOUTH);shell.add(card);frame.getRootPane().setDefaultButton(signIn);return shell;
    }

    private JPanel studentRegistration() {
        JPanel shell=Ui.panel(Theme.CANVAS,new BorderLayout());JPanel card=Ui.card();card.setPreferredSize(new Dimension(540,535));
        JPanel heading=Ui.panel(Color.WHITE,new BorderLayout(0,8));
        heading.add(Ui.label("Create Student Account",Theme.TITLE,Theme.NAVY),BorderLayout.NORTH);
        heading.add(Ui.wrappedLabel("Create your profile with a name and password. QuizMaster assigns your Student ID after registration; you will use that ID to sign in.",Theme.BODY,Theme.MUTED,470),BorderLayout.CENTER);
        card.add(heading,BorderLayout.NORTH);
        JTextField name=new JTextField(24);JPasswordField password=new JPasswordField(24),confirm=new JPasswordField(24);
        for(JComponent input:new JComponent[]{name,password,confirm})styleRegistrationInput(input);
        JLabel status=Ui.label("Your Student ID will be assigned automatically.",Theme.SMALL,Theme.MUTED);
        JPanel form=registrationForm();
        addStackedField(form,"Full/display name",name);addStackedField(form,"Password (10+ characters, letter and number)",password);addStackedField(form,"Confirm password",confirm);
        JButton create=Ui.button("Create account",true);addFormControl(form,create,12);
        JButton claim=Ui.link("Have an existing profile? Set its password");claim.setAlignmentX(Component.LEFT_ALIGNMENT);form.add(Box.createVerticalStrut(5));form.add(claim);claim.addActionListener(e->show(NavigationPolicy.LEGACY_CLAIM));
        create.addActionListener(e->{char[] a=password.getPassword(),b=confirm.getPassword();try{StudentProfile created=students.registerGenerated(name.getText(),a,b);showAssignedId("Student ID assigned",created.getStudentId(),"Use this ID and the password you just created to sign in.");name.setText("");show(NavigationPolicy.loginFor(com.quizmaster.model.Role.STUDENT));}catch(Exception ex){showError(status,ex.getMessage());}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');password.setText("");confirm.setText("");}});
        card.add(form,BorderLayout.CENTER);JPanel bottom=Ui.panel(Color.WHITE,new BorderLayout(0,8));bottom.add(status,BorderLayout.CENTER);JButton back=Ui.button("← Back to Student Login",false);back.addActionListener(e->show(NavigationPolicy.loginFor(com.quizmaster.model.Role.STUDENT)));bottom.add(back,BorderLayout.SOUTH);card.add(bottom,BorderLayout.SOUTH);addScrollableCard(shell,card);return shell;
    }

    private JPanel legacyClaim() {
        JPanel shell=Ui.panel(Theme.CANVAS,new GridBagLayout());JPanel card=Ui.card();card.setPreferredSize(new Dimension(540,560));
        card.add(Ui.label("Set Password for Existing Profile",Theme.TITLE,Theme.NAVY),BorderLayout.NORTH);
        JTextField name=new JTextField(24),id=new JTextField(24);JPasswordField password=new JPasswordField(24),confirm=new JPasswordField(24);
        for(JComponent input:new JComponent[]{name,id,password,confirm})Ui.styleInput(input);
        JLabel status=Ui.wrappedLabel("Enter the saved ID and exact display name from the older profile, then choose a password. This does not sign you in.",Theme.SMALL,Theme.MUTED,450);
        JPanel form=Ui.panel(Color.WHITE,new GridBagLayout());GridBagConstraints c=fieldConstraints();int row=0;
        addField(form,c,row++,"Saved Student ID",id);addField(form,c,row++,"Saved display name",name);addField(form,c,row++,"New password",password);addField(form,c,row++,"Confirm password",confirm);
        JButton save=Ui.button("Set password",true);c.gridy=row;form.add(save,c);
        save.addActionListener(e->{char[] a=password.getPassword(),b=confirm.getPassword();try{StudentProfile p=students.claimLegacyProfile(name.getText(),id.getText(),a,b);showAssignedId("Profile secured",p.getStudentId(),"Your existing profile is preserved. Sign in with this ID and your new password.");show(NavigationPolicy.loginFor(com.quizmaster.model.Role.STUDENT));}catch(Exception ex){showError(status,ex.getMessage());}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');password.setText("");confirm.setText("");}});
        card.add(new JScrollPane(form),BorderLayout.CENTER);JPanel bottom=Ui.panel(Color.WHITE,new BorderLayout(0,8));bottom.add(status,BorderLayout.CENTER);JButton back=Ui.button("← Student Registration",false);back.addActionListener(e->show(NavigationPolicy.registrationFor(com.quizmaster.model.Role.STUDENT)));bottom.add(back,BorderLayout.SOUTH);card.add(bottom,BorderLayout.SOUTH);shell.add(card);return shell;
    }

    private JPanel teacherRegistration() {
        JPanel shell=Ui.panel(Theme.CANVAS,new BorderLayout());JPanel card=Ui.card();card.setPreferredSize(new Dimension(560,590));
        card.add(Ui.label("Teacher Registration",Theme.TITLE,Theme.NAVY),BorderLayout.NORTH);
        JLabel policy=Ui.wrappedLabel("Teacher accounts require a one-time registration code issued by a signed-in teacher. Ask a teacher to generate an invite before submitting.",Theme.SMALL,Theme.MUTED,470);
        JTextField name=new JTextField(24),code=new JTextField(24);JPasswordField password=new JPasswordField(24),confirm=new JPasswordField(24);
        for(JComponent input:new JComponent[]{name,code,password,confirm})styleRegistrationInput(input);
        JLabel status=Ui.label(" ",Theme.SMALL,Theme.MUTED);JPanel form=registrationForm();
        addStackedField(form,"Teacher name",name);addStackedField(form,"One-time registration code",code);addStackedField(form,"Password (10+ characters, letter and number)",password);addStackedField(form,"Confirm password",confirm);
        JButton register=Ui.button("Register Teacher",true);addFormControl(form,register,12);
        register.addActionListener(e->{char[] a=password.getPassword(),b=confirm.getPassword();try{TeacherProfile created=teachers.registerTeacherWithCode(name.getText(),a,b,code.getText());showAssignedId("Teacher ID assigned",created.getTeacherId(),"Use this ID and your password on the Teacher Login screen.");name.setText("");code.setText("");show(NavigationPolicy.loginFor(com.quizmaster.model.Role.TEACHER));}catch(Exception ex){showError(status,ex.getMessage());}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');password.setText("");confirm.setText("");}});
        JPanel heading=Ui.panel(Color.WHITE,new BorderLayout(0,8));heading.add(Ui.label("Teacher Registration",Theme.TITLE,Theme.NAVY),BorderLayout.NORTH);heading.add(policy,BorderLayout.CENTER);
        card.add(heading,BorderLayout.NORTH);card.add(form,BorderLayout.CENTER);JPanel bottom=Ui.panel(Color.WHITE,new BorderLayout(0,8));bottom.add(status,BorderLayout.CENTER);JButton back=Ui.button("← Back to Teacher Login",false);back.addActionListener(e->show(NavigationPolicy.loginFor(com.quizmaster.model.Role.TEACHER)));bottom.add(back,BorderLayout.SOUTH);card.add(bottom,BorderLayout.SOUTH);addScrollableCard(shell,card);return shell;
    }

    private static GridBagConstraints fieldConstraints(){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(5,0,5,0);return c;}
    private static void addField(JPanel form,GridBagConstraints c,int row,String label,JComponent input){c.gridy=row*2;form.add(Ui.label(label,Theme.BODY_BOLD,Theme.TEXT),c);c.gridy=row*2+1;form.add(input,c);}
    private static JPanel registrationForm(){JPanel form=Ui.panel(Color.WHITE,new BorderLayout());form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));form.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));return form;}
    private static void addStackedField(JPanel form,String label,JComponent input){JPanel group=Ui.panel(Color.WHITE,new BorderLayout(0,6));group.setAlignmentX(Component.LEFT_ALIGNMENT);group.add(Ui.label(label,Theme.BODY_BOLD,Theme.TEXT),BorderLayout.NORTH);group.add(input,BorderLayout.CENTER);group.setMaximumSize(new Dimension(Integer.MAX_VALUE,74));form.add(group);form.add(Box.createVerticalStrut(10));}
    private static void addFormControl(JPanel form,JButton button,int topGap){JPanel holder=Ui.panel(Color.WHITE,new BorderLayout());holder.setAlignmentX(Component.LEFT_ALIGNMENT);holder.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));holder.add(button,BorderLayout.CENTER);form.add(Box.createVerticalStrut(topGap));form.add(holder);}
    private static void styleRegistrationInput(JComponent input){Ui.styleInput(input);Dimension preferred=input.getPreferredSize();input.setPreferredSize(new Dimension(Math.max(260,preferred.width),42));input.setMinimumSize(new Dimension(220,42));}
    private static void addScrollableCard(JPanel shell,JPanel card){JPanel centered=Ui.panel(Theme.CANVAS,new GridBagLayout());centered.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));centered.add(card);JScrollPane scroll=new JScrollPane(centered);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Theme.CANVAS);scroll.getVerticalScrollBar().setUnitIncrement(18);shell.add(scroll,BorderLayout.CENTER);}
    private void showAssignedId(String title,String id,String instructions){JLabel value=Ui.label(id,new Font("Segoe UI",Font.BOLD,25),Theme.BLUE);JPanel panel=Ui.panel(Color.WHITE,new BorderLayout(0,10));panel.add(value,BorderLayout.NORTH);panel.add(Ui.label(instructions,Theme.BODY,Theme.MUTED),BorderLayout.CENTER);JButton copy=Ui.button("Copy ID",false);copy.addActionListener(e->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(id),null);copy.setText("Copied");});panel.add(copy,BorderLayout.SOUTH);JOptionPane.showMessageDialog(this,panel,title,JOptionPane.INFORMATION_MESSAGE);}
    private void show(String view){layout.show(views,view);}
    private static void showError(JLabel label,String message){label.setForeground(new Color(184,52,62));label.setText(message==null||message.isBlank()?"Unable to complete that action.":message);}
}

