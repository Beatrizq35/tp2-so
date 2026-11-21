// MatMulPar.java - Multiplicacao de matrizes PARALELA (java.lang.Thread)
// Uso: java -cp bin MatMulPar N THREADS [estrategia=linhas|ciclico|blocos] [ordem=ikj|ijk] [repeticoes=5] [tam_bloco=64]
//   linhas : cada thread recebe uma faixa CONTIGUA de linhas de C
//   ciclico: linhas intercaladas entre as threads (i, i+T, i+2T, ...)
//   blocos : C dividida em blocos (tiles) BSxBS distribuidos ciclicamente entre as threads
// Saida (CSV): versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum

public class MatMulPar {

    enum Estrategia { LINHAS, CICLICO, BLOCOS }

    /** Tarefa de cada thread: calcula a sua parte de C. Nao ha escrita compartilhada -> sem locks. */
    static class Trabalhador extends Thread {
        final int id, T, n, bs;
        final boolean ikj;
        final Estrategia est;
        final double[] a, b, c;

        Trabalhador(int id, int T, int n, int bs, boolean ikj, Estrategia est,
                    double[] a, double[] b, double[] c) {
            this.id = id; this.T = T; this.n = n; this.bs = bs;
            this.ikj = ikj; this.est = est; this.a = a; this.b = b; this.c = c;
        }

        // calcula a linha i de C, colunas [j0, j1)
        private void calcula(int i, int j0, int j1) {
            if (ikj) {
                for (int j = j0; j < j1; j++) c[i * n + j] = 0;
                for (int k = 0; k < n; k++) {
                    double x = a[i * n + k];
                    for (int j = j0; j < j1; j++) c[i * n + j] += x * b[k * n + j];
                }
            } else {
                for (int j = j0; j < j1; j++) {
                    double s = 0;
                    for (int k = 0; k < n; k++) s += a[i * n + k] * b[k * n + j];
                    c[i * n + j] = s;
                }
            }
        }

        @Override
        public void run() {
            if (est == Estrategia.LINHAS) {
                int base = n / T, resto = n % T;
                int ini = id * base + Math.min(id, resto);
                int fim = ini + base + (id < resto ? 1 : 0);
                for (int i = ini; i < fim; i++) calcula(i, 0, n);
            } else if (est == Estrategia.CICLICO) {
                for (int i = id; i < n; i += T) calcula(i, 0, n);
            } else { // BLOCOS
                int nb = (n + bs - 1) / bs, tarefa = 0;
                for (int bi = 0; bi < nb; bi++)
                    for (int bj = 0; bj < nb; bj++, tarefa++) {
                        if (tarefa % T != id) continue;
                        int i1 = Math.min((bi + 1) * bs, n);
                        int j1 = Math.min((bj + 1) * bs, n);
                        for (int i = bi * bs; i < i1; i++) calcula(i, bj * bs, j1);
                    }
            }
        }
    }

    static void multiplica(double[] a, double[] b, double[] c, int n, int T,
                           Estrategia est, boolean ikj, int bs) throws InterruptedException {
        Trabalhador[] th = new Trabalhador[T];
        for (int t = 0; t < T; t++) {
            th[t] = new Trabalhador(t, T, n, bs, ikj, est, a, b, c);
            th[t].start();
        }
        for (int t = 0; t < T; t++) th[t].join();   // espera todas terminarem
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Uso: java -cp bin MatMulPar N THREADS [linhas|ciclico|blocos] [ijk|ikj] [repeticoes] [tam_bloco]");
            System.exit(1);
        }
        int n = Integer.parseInt(args[0]);
        int T = Integer.parseInt(args[1]);
        String se = args.length > 2 ? args[2] : "linhas";
        String ordem = args.length > 3 ? args[3] : "ikj";
        int reps = args.length > 4 ? Integer.parseInt(args[4]) : 5;
        int bs = args.length > 5 ? Integer.parseInt(args[5]) : 64;
        if (n <= 0 || T <= 0 || reps <= 0 || bs <= 0) { System.err.println("Parametros invalidos"); System.exit(1); }

        Estrategia est = se.equals("ciclico") ? Estrategia.CICLICO
                       : se.equals("blocos") ? Estrategia.BLOCOS : Estrategia.LINHAS;
        boolean ikj = !ordem.equals("ijk");

        double[] a = new double[n * n], b = new double[n * n], c = new double[n * n];
        Common.inicializa(a, b, n);

        // Aquecimento da JVM (JIT): 1 execucao NAO medida
        multiplica(a, b, c, n, T, est, ikj, bs);

        double soma = 0, menor = Double.MAX_VALUE;
        for (int r = 0; r < reps; r++) {
            double t0 = Common.agora();
            multiplica(a, b, c, n, T, est, ikj, bs);
            double t = Common.agora() - t0;
            soma += t;
            if (t < menor) menor = t;
        }
        System.out.println(Common.csv("par", n, T, se, ikj ? "ikj" : "ijk", soma / reps, menor, Common.checksum(c)));
    }
}
