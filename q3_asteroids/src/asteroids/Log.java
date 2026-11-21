package asteroids;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;

/** Log thread-safe: imprime no console e na janela, sempre com o nome da thread que gerou a mensagem. */
public class Log {
    private static JTextArea area;

    public static JScrollPane criarPainel() {
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        area.setBackground(new Color(20, 20, 30));
        area.setForeground(new Color(180, 255, 180));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(430, 700));
        return sp;
    }

    public static void print(String msg) {
        String linha = "[" + Thread.currentThread().getName() + "] " + msg;
        System.out.println(linha);
        JTextArea a = area;
        if (a == null) return;
        SwingUtilities.invokeLater(() -> {
            a.append(linha + "\n");
            if (a.getLineCount() > 400) {
                try { a.replaceRange("", 0, a.getLineStartOffset(150)); } catch (BadLocationException ex) { }
            }
            a.setCaretPosition(a.getDocument().getLength());
        });
    }
}