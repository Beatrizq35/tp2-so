// dotprod_seq.c - Produto escalar SEQUENCIAL
// Uso: ./dotprod_seq N [repeticoes=5]
// Saida (CSV): versao,N,threads,tempo_medio_s,tempo_min_s,checksum
#include "common.h"

static double produto(const double *a, const double *b, long n) {
    double s = 0;
    for (long i = 0; i < n; i++) s += a[i] * b[i];
    return s;
}

int main(int argc, char **argv) {
    if (argc < 2) { fprintf(stderr, "Uso: %s N [repeticoes]\n", argv[0]); return 1; }
    long n = atol(argv[1]);
    int reps = argc > 2 ? atoi(argv[2]) : 5;
    if (n <= 0 || reps <= 0) { fprintf(stderr, "Parametros invalidos\n"); return 1; }

    double *a = aloca_vetor(n), *b = aloca_vetor(n);
    inicializa_vetores(a, b, n);

    double soma = 0, menor = 1e300, res = 0;
    for (int r = 0; r < reps; r++) {
        double t0 = agora();
        res = produto(a, b, n);
        double t = agora() - t0;
        soma += t; if (t < menor) menor = t;
    }
    printf("seq,%ld,1,%.6f,%.6f,%.0f\n", n, soma / reps, menor, res);
    free(a); free(b);
    return 0;
}
