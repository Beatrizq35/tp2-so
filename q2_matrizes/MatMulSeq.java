// MatMulSeq.java - Multiplicacao de matrizes SEQUENCIAL
// Uso: java -cp bin MatMulSeq N [ordem=ikj|ijk] [repeticoes=5]
// Saida (CSV): versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum
//
// Obs.: o calculo e feito linha a linha por um metodo (calculaLinha), com o MESMO
// codigo usado em MatMulPar. Assim a JVM (JIT) otimiza as duas versoes da mesma forma
// e a comparacao sequencial x paralelo fica justa.

public class MatMulSeq {

    // calcula a linha i de C (colunas 0..n-1) - identico ao calcula() de MatMulPar
    static void calculaLinha(double[] a, double[] b, double[] c, int n, int i, boolean ikj) {
        if (ikj) {
            for (int j = 0; j < n; j++) c[i * n + j] = 0;
            for (int k = 0; k < n; k++) {
                double x = a[i * n + k];
                for (int j = 0; j < n; j++) c[i * n + j] += x * b[k * n + j];
            }
        } else {
            for (int j = 0; j < n; j++) {
                double s = 0;
                for (int k = 0; k < n; k++) s += a[i * n + k] * b[k * n + j];
                c[i * n + j] = s;
            }
        }
    }

    static void multiplica(double[] a, double[] b, double[] c, int n, boolean ikj) {
        for (int i = 0; i < n; i++) calculaLinha(a, b, c, n, i, ikj);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -cp bin MatMulSeq N [ijk|ikj] [repeticoes]");
            System.exit(1);
        }
        int n = Integer.parseInt(args[0]);
        String ordem = args.length > 1 ? args[1] : "ikj";
        int reps = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        if (n <= 0 || reps <= 0) { System.err.println("Parametros invalidos"); System.exit(1); }
        boolean ikj = !ordem.equals("ijk");

        double[] a = new double[n * n], b = new double[n * n], c = new double[n * n];
        Common.inicializa(a, b, n);

        // Aquecimento da JVM (JIT): 1 execucao NAO medida
        multiplica(a, b, c, n, ikj);

        double soma = 0, menor = Double.MAX_VALUE;
        for (int r = 0; r < reps; r++) {
            double t0 = Common.agora();
            multiplica(a, b, c, n, ikj);
            double t = Common.agora() - t0;
            soma += t;
            if (t < menor) menor = t;
        }
        System.out.println(Common.csv("seq", n, 1, "-", ikj ? "ikj" : "ijk", soma / reps, menor, Common.checksum(c)));
    }
}
