// common.h - funcoes compartilhadas da Questao 1 (produto escalar)
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

static inline double *aloca_vetor(long n) {
    double *v = malloc((size_t)n * sizeof(double));
    if (!v) { fprintf(stderr, "Erro: memoria insuficiente\n"); exit(1); }
    return v;
}

// Inicializacao deterministica (mesmos valores em todas as maquinas/versoes).
// Valores inteiros pequenos guardados em double: produtos e somas sao inteiros
// exatos (< 2^53), entao sequencial e paralelo dao o MESMO checksum,
// independentemente da ordem em que as somas sao feitas.
static inline void inicializa_vetores(double *a, double *b, long n) {
    for (long i = 0; i < n; i++) {
        a[i] = (double)((i * 7 + 3) % 10);
        b[i] = (double)((i * 5 + 1) % 10);
    }
}
#endif
