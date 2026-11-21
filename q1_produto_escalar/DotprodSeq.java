import java.util.Locale;

// DotprodSeq.java - Produto escalar SEQUENCIAL (Questao 1, TP2 SO - UFAM)
// Uso: java DotprodSeq N [repeticoes=5]
// Saida (CSV): versao,N,threads,tempo_medio_s,tempo_min_s,checksum
public class DotprodSeq {

    static double produto(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java DotprodSeq N [repeticoes]");
            System.exit(1);
        }
        int n = Integer.parseInt(args[0]);
        int reps = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        if (n <= 0 || reps <= 0) {
            System.err.println("Parametros invalidos");
            System.exit(1);
        }

        // Inicializacao deterministica: inteiros pequenos em double.
        // Produtos e somas sao inteiros exatos (< 2^53), entao o checksum
        // e igual ao da versao paralela, independentemente da ordem das somas.
        double[] a = new double[n];
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            a[i] = (i * 7 + 3) % 10;
            b[i] = (i * 5 + 1) % 10;
        }

        produto(a, b); // aquecimento do JIT (nao medido)

        double soma = 0, menor = Double.MAX_VALUE, res = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            res = produto(a, b);
            double t = (System.nanoTime() - t0) / 1e9;
            soma += t;
            if (t < menor) menor = t;
        }
        System.out.println(String.format(Locale.US, "seq,%d,1,%.6f,%.6f,%.0f",
                n, soma / reps, menor, res));
    }
}
