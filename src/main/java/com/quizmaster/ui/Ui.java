package com.quizmaster.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Small reusable Swing helpers used by the Phase 1 screens. */
public final class Ui {
    private Ui() { }
    public static JButton link(String text){JButton button=new JButton("<html><u>"+text+"</u></html>");button.setFont(Theme.SMALL);button.setForeground(Theme.BLUE);button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));button.setBorder(javax.swing.BorderFactory.createEmptyBorder(4,0,4,0));button.setContentAreaFilled(false);button.setFocusPainted(true);button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);return button;}

    public static JPanel panel(Color color, java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(color);
        return panel;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JLabel wrappedLabel(String text, Font font, Color color, int width) {
        int lineLimit = Math.max(24, width / 9);
        StringBuilder html = new StringBuilder("<html>");
        int lineLength = 0;
        for (String word : text.split("\\s+")) {
            if (lineLength > 0 && lineLength + word.length() + 1 > lineLimit) {
                html.append("<br>");
                lineLength = 0;
            } else if (lineLength > 0) {
                html.append(' ');
                lineLength++;
            }
            html.append(word.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
            lineLength += word.length();
        }
        html.append("</html>");
        return label(html.toString(), font, color);
    }

    public static void styleInput(JComponent input) {
        input.setFont(Theme.BODY);
        input.setForeground(Theme.TEXT);
        input.setBackground(Theme.SURFACE);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent event) {
                input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.FOCUS, 2),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            }
            @Override public void focusLost(java.awt.event.FocusEvent event) {
                input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }
        });
    }

    public static JButton button(String text, boolean primary) {
        Color background = primary ? Theme.BLUE : Color.WHITE;
        Color foreground = primary ? Color.WHITE : Theme.TEXT;
        return makeButton(text, background, foreground, primary ? Theme.BLUE : Theme.BORDER);
    }

    public static JButton dangerButton(String text) {
        return makeButton(text, Theme.DANGER, Color.WHITE, Theme.DANGER);
    }

    private static JButton makeButton(String text, Color background, Color foreground, Color outline) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(Theme.BODY_BOLD);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(outline),
                BorderFactory.createEmptyBorder(11, 18, 11, 18)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setRolloverEnabled(true);
        button.setMinimumSize(new Dimension(96, 42));
        button.setPreferredSize(new Dimension(Math.max(116, button.getPreferredSize().width), 44));
        button.addPropertyChangeListener("enabled", event -> {
            boolean enabled = Boolean.TRUE.equals(event.getNewValue());
            button.setBackground(enabled ? background : new Color(232, 235, 241));
            button.setForeground(enabled ? foreground : Theme.MUTED);
        });
        return button;
    }

    public static JButton sidebarButton(String text, boolean selected) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFont(Theme.BODY_BOLD);
        button.setForeground(selected ? Color.WHITE : Theme.SIDEBAR_TEXT);
        button.setBackground(selected ? Theme.NAVY_LIGHT : Theme.NAVY);
        button.setOpaque(true);
        button.setFocusPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, selected ? Theme.BLUE : Theme.NAVY),
                BorderFactory.createEmptyBorder(0, 14, 0, 8)));
        button.setPreferredSize(new Dimension(190, 48));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return button;
    }

    public static JButton sidebarLogoutButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFont(Theme.BODY);
        button.setForeground(Theme.SIDEBAR_TEXT);
        button.setBackground(Theme.NAVY);
        button.setOpaque(true);
        button.setFocusPainted(true);
        button.setBorder(BorderFactory.createEmptyBorder(14, 25, 18, 8));
        return button;
    }

    public static void styleTable(JTable table) {
        table.setFont(Theme.BODY);
        table.setForeground(Theme.TEXT);
        table.setBackground(Color.WHITE);
        table.setRowHeight(38);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.PALE_BLUE);
        table.setSelectionForeground(Theme.NAVY);
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setBackground(Theme.PALE_BLUE);
        header.setForeground(Theme.NAVY);
        header.setReorderingAllowed(false);
    }

    /** Set consistent standard Swing colors before constructing any screens. */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("control", Theme.CANVAS);
            UIManager.put("nimbusBase", Theme.NAVY);
            UIManager.put("nimbusBlueGrey", new Color(222, 228, 238));
            UIManager.put("nimbusFocus", Theme.FOCUS);
            UIManager.put("text", Theme.TEXT);
            UIManager.put("Table.selectionBackground", Theme.PALE_BLUE);
            UIManager.put("Table.selectionForeground", Theme.NAVY);
            UIManager.put("OptionPane.background", Theme.CANVAS);
            UIManager.put("Panel.background", Theme.CANVAS);
            UIManager.put("defaultFont", Theme.BODY);
            UIManager.put("Button.font", Theme.BODY_BOLD);
            UIManager.put("Label.font", Theme.BODY);
            UIManager.put("TextField.font", Theme.BODY);
            UIManager.put("PasswordField.font", Theme.BODY);
            UIManager.put("ComboBox.font", Theme.BODY);
            UIManager.put("Table.font", Theme.BODY);
            UIManager.put("TableHeader.font", Theme.BODY_BOLD);
            UIManager.put("OptionPane.messageFont", Theme.BODY);
            UIManager.put("OptionPane.buttonFont", Theme.BODY_BOLD);
        } catch (Exception ignored) {
            // Individual components still receive the application theme below.
        }
    }

    public static JPanel card() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(20, 21, 20, 21)));
        return card;
    }

    public static JPanel centered(JPanel inner) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CANVAS);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(20, 20, 20, 20);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        wrapper.add(inner, constraints);
        return wrapper;
    }

    public static JLabel centeredLabel(String text, Font font, Color color) {
        JLabel label = label(text, font, color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public static JPanel header(String title, String subtitle) {
        JPanel header = panel(Theme.CANVAS, new BorderLayout(0, 7));
        header.add(label(title, Theme.TITLE, Theme.TEXT), BorderLayout.NORTH);
        header.add(label(subtitle, Theme.BODY, Theme.MUTED), BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));
        return header;
    }

    public static JPanel buttonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(Theme.CANVAS);
        return row;
    }

    public static JPanel headerAction(JButton button) {
        JPanel holder = panel(Theme.CANVAS, new BorderLayout());
        holder.add(button, BorderLayout.NORTH);
        return holder;
    }

    public static JPanel sidebarBrand() { return sidebarBrand("OOP & Computer Graphics"); }
    public static JPanel sidebarBrand(String subjectName) {
        JPanel brand = panel(Theme.NAVY, new BorderLayout(0, 3));
        brand.setBorder(BorderFactory.createEmptyBorder(26, 22, 28, 18));
        brand.add(label("QuizMaster", new Font("Segoe UI", Font.BOLD, 23), Color.WHITE), BorderLayout.NORTH);
        brand.add(label(subjectName, Theme.SMALL, new Color(179, 194, 220)), BorderLayout.CENTER);
        return brand;
    }

    public static Dimension minSize(int width, int height) { return new Dimension(width, height); }
}
