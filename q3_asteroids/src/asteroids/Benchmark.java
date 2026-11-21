package asteroids;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;

/** Modo sem interface: mede Ts (sequencial), Tp (paralelo) e o speedup Sp = Ts / Tp. */
public class Benchmark {
    static final double DT = 1.0 / 60;
    static final int FRAMES = 30, REPS = 3;
    static final int[] TAMANHOS = {1000, 2000, 4000, 8000}; // nº de asteroides
    static final int[] THREADS = {1, 2, 4, 8};

    // uso: java asteroids.Benchmark   (gera benchmark_q3.csv na pasta atual)
    public static void main(String[] args) throws Exception {
        int cpus = Runtime.getRuntime().availableProcessors();
        System.out.println("CPUs disponíveis: " + cpus + " | frames por execução: " + FRAMES + " | repetições: " + REPS);
        sequencial(1000); paralelo(1000, 2); // aquecimento da JVM

        StringBuilder csv = new StringBuilder("cpus,asteroides,threads,Ts_ms,Tp_ms,speedup\n");
        System.out.printf(Locale.US, "%-11s %-8s %-10s %-10s %-8s%n", "asteroides", "threads", "Ts (ms)", "Tp (ms)", "Sp");
        for (int n : TAMANHOS) {
            double ts = 0;
            for (int r = 0; r < REPS; r++) ts += sequencial(n);
            ts /= REPS;
            for (int t : THREADS) {
                double tp = 0;
                for (int r = 0; r < REPS; r++) tp += paralelo(n, t);
                tp /= REPS;
                double sp = ts / tp;
                System.out.printf(Locale.US, "%-11d %-8d %-10.1f %-10.1f %-8.2f%n", n, t, ts, tp, sp);
                csv.append(String.format(Locale.US, "%d,%d,%d,%.1f,%.1f,%.2f%n", cpus, n, t, ts, tp, sp));
            }
        }
        Files.writeString(Path.of("benchmark_q3.csv"), csv.toString());
        System.out.println("Resultados salvos em benchmark_q3.csv");
    }

    static double sequencial(int n) {
        Mundo m = new Mundo(1000, 700, n, 42L);
        long ini = System.nanoTime();
        for (int f = 0; f < FRAMES; f++) {
            m.moverAsteroides(0, 1, DT);
            m.interagirAsteroides(0, 1);
        }
        return (System.nanoTime() - ini) / 1e6;
    }

    static double paralelo(int n, int t) throws Exception {
        Mundo m = new Mundo(1000, 700, n, 42L);
        CyclicBarrier b = new CyclicBarrier(t);
        Thread[] ths = new Thread[t];
        long ini = System.nanoTime();
        for (int i = 0; i < t; i++) {
            final int id = i;
            ths[i] = new Thread(() -> {
                try {
                    for (int f = 0; f < FRAMES; f++) {
                        m.moverAsteroides(id, t, DT);
                        b.await();
                        m.interagirAsteroides(id, t);
                        b.await();
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            }, "Bench-" + i);
            ths[i].start();
        }
        for (Thread th : ths) th.join();
        return (System.nanoTime() - ini) / 1e6;
    }
}