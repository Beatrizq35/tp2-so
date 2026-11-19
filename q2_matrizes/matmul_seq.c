// matmul_seq.c - Multiplicacao de matrizes SEQUENCIAL
// Uso: ./matmul_seq N [ordem=ikj|ijk] [repeticoes=5]
// Saida (CSV): versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum
#include <string.h>
#include "common.h"

static void mult_ijk(const double *A, const double *B, double *C, int n) {
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++) {
            double s = 0;
            for (int k = 0; k < n; k++) s += A[(size_t)i*n+k] * B[(size_t)k*n+j];
            C[(size_t)i*n+j] = s;
        }
}

static void mult_ikj(const double *A, const double *B, double *C, int n) {
    memset(C, 0, (size_t)n * n * sizeof(double));
    for (int i = 0; i < n; i++)
        for (int k = 0; k < n; k++) {
            double a = A[(size_t)i*n+k];
            for (int j = 0; j < n; j++) C[(size_t)i*n+j] += a * B[(size_t)k*n+j];
        }
}

int main(int argc, char **argv) {
    if (argc < 2) { fprintf(stderr, "Uso: %s N [ijk|ikj] [repeticoes]\n", argv[0]); return 1; }
    int n = atoi(argv[1]);
    const char *ordem = argc > 2 ? argv[2] : "ikj";
    int reps = argc > 3 ? atoi(argv[3]) : 5;
    if (n <= 0 || reps <= 0) { fprintf(stderr, "Parametros invalidos\n"); return 1; }

    double *A = aloca(n), *B = aloca(n), *C = aloca(n);
    inicializa(A, B, n);

    double soma = 0, menor = 1e300;
    for (int r = 0; r < reps; r++) {
        double t0 = agora();
        if (strcmp(ordem, "ijk") == 0) mult_ijk(A, B, C, n); else mult_ikj(A, B, C, n);
        double t = agora() - t0;
        soma += t; if (t < menor) menor = t;
    }
    printf("seq,%d,1,-,%s,%.6f,%.6f,%.6f\n", n, ordem, soma / reps, menor, checksum(C, n));
    free(A); free(B); free(C);
    return 0;
}
