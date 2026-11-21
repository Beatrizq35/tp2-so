package asteroids;

import javax.swing.*;
import java.awt.BorderLayout;

/** Versão SEM threads próprias: tudo acontece em um único fluxo (timer da interface). */
public class MainSequencial {
    static final double DT = 1.0 / 60;
    static final double SPAWN_S = 2.0;

    // uso: java asteroids.MainSequencial [asteroides]
    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 150;
        SwingUtilities.invokeLater(() -> iniciar(n));
    }

    static void iniciar(int n) {
        Mundo mundo = new Mundo(1000, 700, n, System.nanoTime());
        mundo.log = Log::print;
        Painel painel = new Painel(mundo, "SEQUENCIAL", 0);

        JFrame f = new JFrame("Asteroids - versão SEQUENCIAL");
        f.setLayout(new BorderLayout());
        f.add(painel, BorderLayout.CENTER);
        f.add(Log.criarPainel(), BorderLayout.EAST);
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        painel.requestFocusInWindow();
        Log.print("Iniciado SEM threads extras: " + n + " asteroides, tudo em um único fluxo.");

        int[] frame = {0};
        double[] acum = {0};
        new Timer(16, e -> {
            long ini = System.nanoTime();
            int proc = mundo.moverAsteroides(0, 1, DT);
            mundo.interagirAsteroides(0, 1);
            mundo.passoJogo(DT);
            acum[0] += 0.016;
            if (acum[0] >= SPAWN_S) {
                acum[0] = 0;
                if (mundo.asteroides.size() < mundo.maximo) {
                    mundo.adicionarAsteroides(1);
                    Log.print("Novo asteroide criado (no mesmo fluxo)");
                }
            }
            mundo.msLogica = (System.nanoTime() - ini) / 1e6;
            if (++frame[0] % 120 == 0)
                Log.print(String.format("frame %d: processou sozinha %d asteroides (%.2f ms)", frame[0], proc, mundo.msLogica));
            painel.repaint();
        }).start();
    }
}