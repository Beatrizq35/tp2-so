// Common.java - funcoes compartilhadas (inicializacao, checksum, tempo)

/**
 * Utilitarios usados pelas versoes sequencial e paralela.
 * As matrizes NxN sao guardadas em vetores 1D (linha a linha): elemento (i,j) = m[i*n + j].
 */
public final class Common {
    private Common() {}

    /** Inicializacao deterministica: mesmos valores em todas as maquinas e versoes. */
    public static void inicializa(double[] a, double[] b, int n) {
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                a[i * n + j] = ((i * 7 + j * 3) % 10) / 10.0;
                b[i * n + j] = ((i * 5 + j * 11) % 10) / 10.0;
            }
    }

    /** Soma de todos os elementos de C: usada para validar o resultado entre as versoes. */
    public static double checksum(double[] c) {
        double s = 0;
        for (double v : c) s += v;
        return s;
    }

    /** Tempo em segundos (relogio monotonico). */
    public static double agora() {
        return System.nanoTime() * 1e-9;
    }

    /** Linha CSV padrao: versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum */
    public static String csv(String versao, int n, int threads, String estrategia,
                             String ordem, double medio, double minimo, double checksum) {
        return String.format(java.util.Locale.US, "%s,%d,%d,%s,%s,%.6f,%.6f,%.6f",
                versao, n, threads, estrategia, ordem, medio, minimo, checksum);
    }
}
