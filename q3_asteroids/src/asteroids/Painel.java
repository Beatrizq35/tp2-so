package asteroids;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;

/** Desenha o jogo e trata o teclado. Roda na thread de interface do Swing (EDT). */
public class Painel extends JPanel {
    private final Mundo mundo;
    private final String modo;
    private final int threads; // 0 = versão sequencial
    private int quadros, fps;
    private long ultimo = System.nanoTime();

    public Painel(Mundo mundo, String modo, int threads) {
        this.mundo = mundo; this.modo = modo; this.threads = threads;
        setPreferredSize(new Dimension(mundo.largura, mundo.altura));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { tecla(e, true); }
            @Override public void keyReleased(KeyEvent e) { tecla(e, false); }
        });
    }

    private void tecla(KeyEvent e, boolean down) {
        Nave n = mundo.nave;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT: n.esq = down; break;
            case KeyEvent.VK_RIGHT: n.dir = down; break;
            case KeyEvent.VK_UP: n.acel = down; break;
            case KeyEvent.VK_SPACE: n.atirando = down; break;
            default:
                if (down && (e.getKeyChar() == '+' || e.getKeyCode() == KeyEvent.VK_ADD)) mundo.adicionarAsteroides(100);
                if (down && (e.getKeyChar() == '-' || e.getKeyCode() == KeyEvent.VK_SUBTRACT)) mundo.removerAsteroides(100);
        }
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Asteroide a : mundo.asteroides) {
            g.setColor(cor(a.dono));
            int r = (int) a.raio;
            g.drawOval((int) (a.x - r), (int) (a.y - r), 2 * r, 2 * r);
            g.drawLine((int) a.x, (int) a.y, (int) (a.x + Math.cos(a.ang) * r), (int) (a.y + Math.sin(a.ang) * r));
            if (a.contatos > 0) { g.setColor(Color.RED); g.fillOval((int) a.x - 2, (int) a.y - 2, 4, 4); }
        }
        g.setColor(Color.WHITE);
        for (Tiro t : mundo.tiros) g.fillOval((int) t.x - 2, (int) t.y - 2, 4, 4);

        Nave n = mundo.nave;
        if (n.invulneravel <= 0 || ((int) (n.invulneravel * 10) % 2 == 0)) {
            Path2D.Double p = new Path2D.Double();
            p.moveTo(n.x + Math.cos(n.ang) * 14, n.y + Math.sin(n.ang) * 14);
            p.lineTo(n.x + Math.cos(n.ang + 2.5) * 10, n.y + Math.sin(n.ang + 2.5) * 10);
            p.lineTo(n.x + Math.cos(n.ang - 2.5) * 10, n.y + Math.sin(n.ang - 2.5) * 10);
            p.closePath();
            g.setColor(Color.CYAN); g.draw(p);
        }

        quadros++;
        long agora = System.nanoTime();
        if (agora - ultimo >= 1_000_000_000L) { fps = quadros; quadros = 0; ultimo = agora; }

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        g.drawString("Modo: " + modo + (threads > 0 ? " (" + threads + " threads de atualização)" : " (1 thread só)"), 10, 18);
        g.drawString(String.format("Asteroides: %d   Pontos: %d   Vidas: %d", mundo.asteroides.size(), mundo.pontos, mundo.vidas), 10, 36);
        g.drawString(String.format("FPS: %d   Lógica: %.2f ms/frame", fps, mundo.msLogica), 10, 54);
        g.drawString("Setas: mover | Espaço: atirar | +/-: 100 asteroides | ponto vermelho = contato", 10, getHeight() - 10);
        for (int i = 0; i < threads; i++) {
            g.setColor(cor(i)); g.fillRect(getWidth() - 120, 10 + i * 16, 10, 10);
            g.setColor(Color.WHITE); g.drawString("Worker-" + i, getWidth() - 104, 20 + i * 16);
        }
    }

    private Color cor(int dono) {
        if (threads == 0 || dono < 0) return Color.LIGHT_GRAY;
        return Color.getHSBColor((float) dono / Math.max(threads, 1), 0.6f, 1f);
    }
}