package com.quizmaster.ui;

import com.quizmaster.model.Question;
import com.quizmaster.model.SyllabusUnit;
import com.quizmaster.service.QuestionService;
import com.quizmaster.storage.QuestionLoadResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Teacher-only question bank table and CRUD controls. */
final class QuestionManagementPanel extends JPanel {
    private final QuizMasterFrame frame;
    private final QuestionService questions;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Syllabus unit", "Topic", "Question"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final List<Question> visibleQuestions = new ArrayList<>();
    private final JLabel status = Ui.label(" ", Theme.SMALL, Theme.MUTED);
    private final JTextField search = new JTextField(20);
    private final JComboBox<Object> unitFilter = new JComboBox<>();
    private final CardLayout centerCards = new CardLayout();
    private final JPanel center = new JPanel(centerCards);
    private List<String> loadIssues = List.of();

    QuestionManagementPanel(QuizMasterFrame frame, QuestionService questions) {
        super(new BorderLayout());
        this.frame = frame;
        this.questions = questions;
        setBackground(Theme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(28, 34, 26, 34));

        JPanel header = Ui.panel(Theme.CANVAS, new BorderLayout());
        header.add(Ui.header("Question bank", "Manage syllabus-based questions for student practice"), BorderLayout.CENTER);
        JButton back = Ui.button("← Teacher overview", false);
        back.addActionListener(event -> frame.showTeacher());
        header.add(Ui.headerAction(back), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = Ui.panel(Theme.CANVAS, new BorderLayout(0, 14));
        JPanel controls = Ui.panel(Theme.CANVAS, new BorderLayout(10, 0));
        JPanel filters = Ui.panel(Theme.CANVAS, new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        filters.add(Ui.label("Filter", Theme.BODY_BOLD, Theme.TEXT));
        unitFilter.addItem("All units");
        for (SyllabusUnit unit : SyllabusUnit.values()) unitFilter.addItem(unit);
        unitFilter.setRenderer((list,value,index,selected,focus)->{String text=value instanceof SyllabusUnit u?u.getGenericTitle():String.valueOf(value);JLabel label=new JLabel(text);label.setOpaque(true);label.setBackground(selected?Theme.PALE_BLUE:Color.WHITE);label.setForeground(Theme.TEXT);label.setBorder(BorderFactory.createEmptyBorder(5,8,5,8));return label;});
        Ui.styleInput(unitFilter);
        filters.add(unitFilter);
        Ui.styleInput(search);
        search.setToolTipText("Search question ID, topic, or text");
        filters.add(search);
        controls.add(filters, BorderLayout.CENTER);
        JPanel actions = Ui.buttonRow();
        JButton add = Ui.button("+ Add question", true);
        JButton edit = Ui.button("Edit selected", false);
        JButton delete = Ui.dangerButton("Delete selected");
        JButton viewIssues = Ui.button("View data issues", false);
        edit.setEnabled(false);
        delete.setEnabled(false);
        add.addActionListener(event -> addQuestion());
        edit.addActionListener(event -> editSelected());
        delete.addActionListener(event -> deleteSelected());
        viewIssues.addActionListener(event -> showIssues());
        actions.add(add); actions.add(edit); actions.add(delete); actions.add(viewIssues);
        controls.add(actions, BorderLayout.SOUTH);
        controls.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        body.add(controls, BorderLayout.NORTH);

        Ui.styleTable(table);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.getViewport().setBackground(Color.WHITE);
        center.add(tableScroll, "table");
        JPanel emptyCard = Ui.card();
        emptyCard.add(Ui.centeredLabel("No questions to show", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        emptyCard.add(Ui.centeredLabel("Add a question to begin building this syllabus-based bank.", Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
        JButton emptyAdd = Ui.button("+ Add first question", true);
        emptyAdd.addActionListener(event -> addQuestion());
        emptyCard.add(emptyAdd, BorderLayout.SOUTH);
        JPanel empty = Ui.centered(emptyCard);
        center.add(empty, "empty");
        body.add(center, BorderLayout.CENTER);

        JPanel footer = Ui.panel(Theme.CANVAS, new BorderLayout(10, 0));
        footer.add(status, BorderLayout.CENTER);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        unitFilter.addActionListener(event -> applyFilter());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                boolean selected = table.getSelectedRow() >= 0;
                edit.setEnabled(selected);
                delete.setEnabled(selected);
            }
        });
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        refreshQuestions();
    }

    void refreshQuestions() {
        try {
            QuestionLoadResult result = questions.listQuestions();
            visibleQuestions.clear();
            visibleQuestions.addAll(result.getQuestions());
            loadIssues = result.getIssues();
            tableModel.setRowCount(0);
            for (Question question : visibleQuestions) {
                String subjectName;try{subjectName=questions.selectedSubjectName();}catch(Exception ex){subjectName="";}
                tableModel.addRow(new Object[]{question.getId(), question.getUnit().getTitleForSubject(subjectName), question.getTopic(), question.getText()});
            }
            centerCards.show(center, visibleQuestions.isEmpty() ? "empty" : "table");
            if (!loadIssues.isEmpty()) {
                setStatus("Loaded " + visibleQuestions.size() + " valid question(s); " + loadIssues.size() + " invalid row(s). Changes are blocked to protect the CSV.", true);
            } else if (visibleQuestions.isEmpty()) {
                setStatus("The question bank is empty.", false);
            } else {
                setStatus(visibleQuestions.size() + " question(s) loaded.", false);
            }
            applyFilter();
        } catch (SecurityException ex) {
            setStatus("Access denied: " + ex.getMessage(), true);
        } catch (java.io.IOException ex) {
            setStatus("Could not read questions.csv: " + ex.getMessage(), true);
        }
    }

    private void addQuestion() {
        String subjectId;try{subjectId=questions.selectedSubjectId();}catch(Exception ex){showError(ex.getMessage());return;}
        Question question = QuestionEditorDialog.show(frame, null,subjectId);
        if (question == null) return;
        runChange(() -> questions.addQuestion(question));
    }

    private void editSelected() {
        Question selected = selectedQuestion();
        if (selected == null) { showMessage("Select a question to edit.", "No question selected"); return; }
        Question updated = QuestionEditorDialog.show(frame, selected,selected.getSubjectId());
        if (updated == null) return;
        runChange(() -> questions.updateQuestion(updated));
    }

    private void deleteSelected() {
        Question selected = selectedQuestion();
        if (selected == null) { showMessage("Select a question to delete.", "No question selected"); return; }
        Object[] choices = {"Keep question", "Delete question"};
        int answer = JOptionPane.showOptionDialog(frame,
                "Delete question " + selected.getId() + "? This cannot be undone.",
                "Confirm delete", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[0]);
        if (answer == 1) runChange(() -> questions.deleteQuestion(selected.getId()));
    }

    private Question selectedQuestion() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= visibleQuestions.size()) return null;
        return visibleQuestions.get(modelRow);
    }

    private void runChange(IoAction action) {
        try {
            action.run();
            refreshQuestions();
        } catch (SecurityException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage());
            refreshQuestions();
        }
    }

    private void applyFilter() {
        String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        Object selectedUnit = unitFilter.getSelectedItem();
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int row = entry.getIdentifier();
                Question question = visibleQuestions.get(row);
                boolean unitMatches = !(selectedUnit instanceof SyllabusUnit) || question.getUnit() == selectedUnit;
                boolean queryMatches = query.isEmpty()
                        || question.getId().toLowerCase(java.util.Locale.ROOT).contains(query)
                        || question.getTopic().toLowerCase(java.util.Locale.ROOT).contains(query)
                        || question.getText().toLowerCase(java.util.Locale.ROOT).contains(query);
                return unitMatches && queryMatches;
            }
        });
        if (!loadIssues.isEmpty()) {
            String message = "Loaded " + visibleQuestions.size() + " valid question(s); " + loadIssues.size()
                    + " invalid row(s). Changes are blocked to protect the CSV.";
            if (!visibleQuestions.isEmpty() && table.getRowCount() == 0) message += " No questions match this filter.";
            setStatus(message, true);
        } else if (!visibleQuestions.isEmpty() && table.getRowCount() == 0) setStatus("No questions match the current filter.", false);
        else if (loadIssues.isEmpty() && !visibleQuestions.isEmpty()) setStatus(visibleQuestions.size() + " question(s) loaded.", false);
    }

    private void showIssues() {
        if (loadIssues.isEmpty()) { showMessage("No malformed question rows were found.", "Question data"); return; }
        javax.swing.JTextArea details = new javax.swing.JTextArea(String.join("\n", loadIssues), 12, 68);
        details.setEditable(false);
        details.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
        JOptionPane.showMessageDialog(frame, new JScrollPane(details), "Question CSV issues", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message == null ? "The question change could not be saved." : message,
                "Question bank", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void setStatus(String message, boolean error) {
        status.setText(message);
        status.setForeground(error ? new Color(184, 52, 62) : Theme.MUTED);
    }

    @FunctionalInterface private interface IoAction { void run() throws Exception; }
}
