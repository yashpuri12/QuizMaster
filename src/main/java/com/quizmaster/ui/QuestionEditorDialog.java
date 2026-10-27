package com.quizmaster.ui;

import com.quizmaster.model.Question;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.model.Subject;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.JRootPane;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.UUID;

/** Modal form for adding or editing one multiple-choice question. */
final class QuestionEditorDialog extends JDialog {
    private final Question original;
    private final String subjectId;
    private final JComboBox<SyllabusUnit> unit = new JComboBox<>(SyllabusUnit.values());
    private final JTextField topic = new JTextField(40);
    private final JTextArea questionText = new JTextArea(3, 40);
    private final JTextField[] options = {new JTextField(40), new JTextField(40), new JTextField(40), new JTextField(40)};
    private final JComboBox<String> correct = new JComboBox<>(new String[]{"Option 1", "Option 2", "Option 3", "Option 4"});
    private final JTextArea explanation = new JTextArea(2, 40);
    private Question result;

    private QuestionEditorDialog(Window owner, Question original,String subjectId) {
        super(owner, original == null ? "Add question" : "Edit question", Dialog.ModalityType.APPLICATION_MODAL);
        this.original = original;
        this.subjectId=original==null?subjectId:original.getSubjectId();
        unit.setRenderer((list,value,index,selected,focus)->{JLabel label=new JLabel(value==null?"":value.getTitleForSubject(this.subjectId.equals(Subject.OOP_ID)?Subject.OOP_NAME:""));label.setOpaque(true);label.setBackground(selected?Theme.PALE_BLUE:java.awt.Color.WHITE);label.setForeground(Theme.TEXT);label.setBorder(BorderFactory.createEmptyBorder(5,8,5,8));return label;});
        Ui.styleInput(unit);
        Ui.styleInput(correct);
        Ui.styleInput(topic);
        Ui.styleInput(questionText);
        Ui.styleInput(explanation);
        questionText.setLineWrap(true); questionText.setWrapStyleWord(true);
        explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        questionText.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Theme.BORDER), javax.swing.BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        explanation.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Theme.BORDER), javax.swing.BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        for (JTextField option : options) Ui.styleInput(option);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        int row = 0;
        addRow(form, row++, "Syllabus unit", unit);
        addRow(form, row++, "Topic", topic);
        addRow(form, row++, "Question text", questionText);
        for (int i = 0; i < options.length; i++) addRow(form, row++, "Option " + (i + 1), options[i]);
        addRow(form, row++, "Correct answer", correct);
        addRow(form, row, "Explanation (optional)", explanation);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Theme.CANVAS);
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        content.add(Ui.label(original == null ? "Write a clear four-option question." : "Update the selected question.", Theme.BODY, Theme.MUTED), BorderLayout.NORTH);
        content.add(new JScrollPane(form), BorderLayout.CENTER);
        JPanel buttons = Ui.buttonRow();
        javax.swing.JButton save = Ui.button(original == null ? "Add question" : "Save changes", true);
        save.addActionListener(event -> saveQuestion());
        javax.swing.JButton cancel = Ui.button("Cancel", false);
        cancel.addActionListener(event -> dispose());
        buttons.add(save);
        buttons.add(cancel);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        populate();
        setSize(800, 700);
        setMinimumSize(new java.awt.Dimension(620, 600));
        setResizable(true);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"), JRootPane.WHEN_IN_FOCUSED_WINDOW);
        setLocationRelativeTo(owner);
    }

    static Question show(Window owner, Question original) {return show(owner,original,Subject.OOP_ID);}
    static Question show(Window owner, Question original,String subjectId) {
        QuestionEditorDialog dialog = new QuestionEditorDialog(owner, original,subjectId);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component input) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(6, 4, 6, 12);
        panel.add(Ui.label(label, Theme.BODY_BOLD, Theme.TEXT), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.weightx = 1; right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(6, 4, 6, 4);
        panel.add(input, right);
    }

    private void populate() {
        if (original == null) return;
        unit.setSelectedItem(original.getUnit());
        topic.setText(original.getTopic());
        questionText.setText(original.getText());
        for (int i = 0; i < options.length; i++) options[i].setText(original.getOptions().get(i));
        correct.setSelectedIndex(original.getCorrectAnswerIndex());
        explanation.setText(original.getExplanation());
    }

    private void saveQuestion() {
        try {
            String id = original == null ? "Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : original.getId();
            result = new Question(id, (SyllabusUnit) unit.getSelectedItem(), topic.getText(), singleLine(questionText.getText()),
                    List.of(options[0].getText(), options[1].getText(), options[2].getText(), options[3].getText()),
                    correct.getSelectedIndex(), singleLine(explanation.getText()),subjectId);
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check question fields", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String singleLine(String value) { return value.replaceAll("\\R+", " ").trim(); }
}
