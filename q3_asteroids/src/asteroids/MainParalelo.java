package asteroids;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Versão COM threads.
 *  - N threads "Worker-i": cada uma move e processa uma fatia dos asteroides
 *  - "Logica": coordena o frame (barreira), trata tiros/colisões/nave
 *  - "Spawner": produz novos asteroides em uma fila (produtor/consumidor)
 *  - "Renderizacao": pede o redesenho da tela
 */
public class MainParalelo {
    static final double DT = 1.0 / 60;

    // uso: java asteroids.MainParalelo [threads] [asteroides]  (sem argumentos, pergunta numa janela)
    public static void main(String[] args) {
        int t = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        if (t <= 0) t = perguntar("Número de threads de atualização:", Runtime.getRuntime().availableProcessors());
        if (n <= 0) n = perguntar("Quantidade inicial de asteroides:", 150);
        final int T = t, N = n;
        SwingUtilities.invokeLater(() -> iniciar(T, N));
    }

    static int perguntar(String msg, int padrao) {
        try { return Math.max(1, Integer.parseInt(JOptionPane.showInputDialog(msg, padrao).trim())); }
        catch (Exception e) { return padrao; }
    }

    static void iniciar(int T, int N) {
        Mundo mundo = new Mundo(1000, 700, N, System.nanoTime());
        mundo.log = Log::print;
        Painel painel = new Painel(mundo, "PARALELO", T);

        JFrame f = new JFrame("Asteroids - versão PARALELA (" + T + " threads)");
        f.setLayout(new BorderLayout());
        f.add(painel, BorderLayout.CENTER);
        f.add(Log.criarPainel(), BorderLayout.EAST);
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        painel.requestFocusInWindow();
        Log.print("Iniciando: " + T + " workers + Logica + Spawner + Renderizacao, " + N + " asteroides");

        CyclicBarrier barreira = new CyclicBarrier(T + 1); // T workers + thread Logica
        for (int i = 0; i < T; i++) iniciarThread(new Trabalhador(i, T, mundo, barreira), "Worker-" + i);
        iniciarThread(() -> loopLogica(mundo, barreira), "Logica");
        iniciarThread(() -> loopSpawner(mundo), "Spawner");
        iniciarThread(() -> {
            while (!Thread.currentThread().isInterrupted()) { painel.repaint(); dormir(16); }
        }, "Renderizacao");
    }

    static void iniciarThread(Runnable r, String nome) {
        Thread t = new Thread(r, nome);
        t.setDaemon(true);
        t.start();
    }

    static void dormir(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Coordena cada frame: libera os workers, espera as duas fases e faz a parte serial. */
    static void loopLogica(Mundo mundo, CyclicBarrier barreira) {
        int frame = 0;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long ini = System.nanoTime();
                barreira.await();   // 1: libera os workers para MOVER
                barreira.await();   // 2: todos moveram -> workers vão INTERAGIR
                barreira.await();   // 3: todos terminaram
                mundo.passoJogo(DT);
                mundo.msLogica = (System.nanoTime() - ini) / 1e6;
                if (++frame % 120 == 0)
                    Log.print(String.format("frame %d concluído: %d asteroides, %d tiros (%.2f ms)",
                            frame, mundo.asteroides.size(), mundo.tiros.size(), mundo.msLogica));
                long resto = 16 - (System.nanoTime() - ini) / 1_000_000;
                if (resto > 0) dormir(resto);
            }
        } catch (InterruptedException | BrokenBarrierException e) { /* encerrando */ }
    }

    /** Produtor: enfileira um asteroide novo a cada 2 s; a thread Logica consome a fila. */
    static void loopSpawner(Mundo mundo) {
        while (!Thread.currentThread().isInterrupted()) {
            dormir(2000);
            if (mundo.asteroides.size() + mundo.pendentes.size() < mundo.maximo) {
                mundo.pendentes.add(mundo.novoAsteroide(false));
                Log.print("Novo asteroide enfileirado (fila=" + mundo.pendentes.size() + ")");
            }
        }
    }

    static class Trabalhador implements Runnable {
        private final int id, total;
        private final Mundo mundo;
        private final CyclicBarrier barreira;

        Trabalhador(int id, int total, Mundo mundo, CyclicBarrier barreira) {
            this.id = id; this.total = total; this.mundo = mundo; this.barreira = barreira;
        }

        @Override public void run() {
            int frame = 0;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    barreira.await();
                    long t0 = System.nanoTime();
                    int qtd = mundo.moverAsteroides(id, total, DT);
                    long t1 = System.nanoTime();
                    barreira.await();
                    long t2 = System.nanoTime();
                    mundo.interagirAsteroides(id, total);
                    long t3 = System.nanoTime();
                    barreira.await();
                    if (++frame % 120 == 0)
                        Log.print(String.format("frame %d: cuidou de %d asteroides (%.2f ms de CPU)",
                                frame, qtd, ((t1 - t0) + (t3 - t2)) / 1e6));
                }
            } catch (InterruptedException | BrokenBarrierException e) { /* encerrando */ }
        }
    }
}