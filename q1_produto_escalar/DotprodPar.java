import java.util.Locale;

// DotprodPar.java - Produto escalar PARALELO com threads (classe Thread)
// (Questao 1, TP2 SO - UFAM)
// Uso: java DotprodPar N THREADS [repeticoes=5]
// Divisao: cada thread recebe um bloco contiguo de ~N/THREADS elementos
// (a ultima thread pega o resto). Cada thread calcula uma soma parcial;
// a thread principal soma as parciais.
// O tempo medido inclui criar e juntar (join) as threads, como na versao C.
// Saida (CSV): versao,N,threads,tempo_medio_s,tempo_min_s,checksum
public class DotprodPar {

    static class Trabalhador extends Thread {
        private final double[] a, b;
        private final int ini, fim;
        double parcial;

        Trabalhador(double[] a, double[] b, int ini, int fim) {
            this.a = a;
            this.b = b;
            this.ini = ini;
            this.fim = fim;
        }

        @Override
        public void run() {
            double s = 0;
            for (int i = ini; i < fim; i++) s += a[i] * b[i];
            parcial = s;
        }
    }

    static double produtoParalelo(double[] a, double[] b, int T) throws InterruptedException {
        int n = a.length;
        int bloco = n / T;
        Trabalhador[] ts = new Trabalhador[T];
        for (int k = 0; k < T; k++) {
            int ini = k * bloco;
            int fim = (k == T - 1) ? n : (k + 1) * bloco; // a ultima pega o resto
            ts[k] = new Trabalhador(a, b, ini, fim);
            ts[k].start();
        }
        double soma = 0;
        for (Trabalhador t : ts) {
            t.join();
            soma += t.parcial;
        }
        return soma;
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Uso: java DotprodPar N THREADS [repeticoes]");
            System.exit(1);
        }
        int n = Integer.parseInt(args[0]);
        int T = Integer.parseInt(args[1]);
        int reps = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        if (n <= 0 || T <= 0 || reps <= 0) {
            System.err.println("Parametros invalidos");
            System.exit(1);
        }

        double[] a = new double[n];
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            a[i] = (i * 7 + 3) % 10;
            b[i] = (i * 5 + 1) % 10;
        }

        produtoParalelo(a, b, T); // aquecimento do JIT (nao medido)

        double soma = 0, menor = Double.MAX_VALUE, res = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            res = produtoParalelo(a, b, T);
            double t = (System.nanoTime() - t0) / 1e9;
            soma += t;
            if (t < menor) menor = t;
        }
        System.out.println(String.format(Locale.US, "par,%d,%d,%.6f,%.6f,%.0f",
                n, T, soma / reps, menor, res));
    }
}
