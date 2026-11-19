// common.h - funcoes compartilhadas (alocacao, inicializacao, tempo, checksum)
#ifndef COMMON_H
#define COMMON_H
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

static inline double agora(void) {            // tempo em segundos (relogio monotonico)
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec + t.tv_nsec * 1e-9;
}

// Matrizes n x n guardadas em vetor 1D (linha a linha)
static inline double *aloca(int n) {
    double *m = malloc((size_t)n * n * sizeof(double));
    if (!m) { fprintf(stderr, "Erro: memoria insuficiente\n"); exit(1); }
    return m;
}

// Inicializacao deterministica (mesmos valores em todas as maquinas/versoes)
static inline void inicializa(double *A, double *B, int n) {
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++) {
            A[(size_t)i * n + j] = ((i * 7 + j * 3) % 10) / 10.0;
            B[(size_t)i * n + j] = ((i * 5 + j * 11) % 10) / 10.0;
        }
}

static inline double checksum(const double *C, int n) {
    double s = 0;
    for (size_t i = 0; i < (size_t)n * n; i++) s += C[i];
    return s;
}
#endif
