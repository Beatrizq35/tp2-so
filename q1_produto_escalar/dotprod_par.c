// dotprod_par.c - Produto escalar PARALELO com pthreads
// Uso: ./dotprod_par N THREADS [repeticoes=5]
// Divisao: cada thread recebe um bloco contiguo de ~N/THREADS elementos
// (a ultima thread pega o resto). Cada thread calcula uma soma parcial;
// a thread principal soma as parciais.
// O tempo medido inclui a criacao e o join das threads (overhead incluso).
// Saida (CSV): versao,N,threads,tempo_medio_s,tempo_min_s,checksum
#include <pthread.h>
#include "common.h"

typedef struct {
    const double *a, *b;
    long ini, fim;
    double parcial;
} Tarefa;

static void *trabalha(void *arg) {
    Tarefa *t = (Tarefa *)arg;
    double s = 0;
    for (long i = t->ini; i < t->fim; i++) s += t->a[i] * t->b[i];
    t->parcial = s;
    return NULL;
}

static double produto_paralelo(const double *a, const double *b, long n, int T,
                               pthread_t *th, Tarefa *tar) {
    long bloco = n / T;
    for (int k = 0; k < T; k++) {
        tar[k].a = a;
        tar[k].b = b;
        tar[k].ini = k * bloco;
        tar[k].fim = (k == T - 1) ? n : (k + 1) * bloco;
        tar[k].parcial = 0;
        if (pthread_create(&th[k], NULL, trabalha, &tar[k]) != 0) {
            fprintf(stderr, "Erro ao criar thread %d\n", k);
            exit(1);
        }
    }
    double soma = 0;
    for (int k = 0; k < T; k++) {
        pthread_join(th[k], NULL);
        soma += tar[k].parcial;
    }
    return soma;
}

int main(int argc, char **argv) {
    if (argc < 3) { fprintf(stderr, "Uso: %s N THREADS [repeticoes]\n", argv[0]); return 1; }
    long n = atol(argv[1]);
    int T = atoi(argv[2]);
    int reps = argc > 3 ? atoi(argv[3]) : 5;
    if (n <= 0 || T <= 0 || reps <= 0) { fprintf(stderr, "Parametros invalidos\n"); return 1; }

    double *a = aloca_vetor(n), *b = aloca_vetor(n);
    inicializa_vetores(a, b, n);
    pthread_t *th = malloc((size_t)T * sizeof(pthread_t));
    Tarefa *tar = malloc((size_t)T * sizeof(Tarefa));
    if (!th || !tar) { fprintf(stderr, "Erro: memoria insuficiente\n"); return 1; }

    double soma = 0, menor = 1e300, res = 0;
    for (int r = 0; r < reps; r++) {
        double t0 = agora();
        res = produto_paralelo(a, b, n, T, th, tar);
        double t = agora() - t0;
        soma += t; if (t < menor) menor = t;
    }
    printf("par,%ld,%d,%.6f,%.6f,%.0f\n", n, T, soma / reps, menor, res);
    free(a); free(b); free(th); free(tar);
    return 0;
}
