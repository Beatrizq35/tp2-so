// matmul_par.c - Multiplicacao de matrizes PARALELA (pthreads)
// Uso: ./matmul_par N THREADS [estrategia=linhas|ciclico|blocos] [ordem=ikj|ijk] [repeticoes=5] [tam_bloco=64]
//   linhas : cada thread recebe um bloco CONTIGUO de linhas de C
//   ciclico: linhas distribuidas de forma intercalada (i, i+T, i+2T, ...)
//   blocos : C dividida em blocos (tiles) BSxBS distribuidos ciclicamente entre as threads
// Saida (CSV): versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum
#include <pthread.h>
#include <string.h>
#include "common.h"

typedef enum { LINHAS, CICLICO, BLOCOS } Estrategia;

typedef struct {
    int id, nthreads, n, bs, ikj;
    Estrategia est;
    const double *A, *B;
    double *C;
} Args;

// calcula as linhas [i0,i1) (passo 'passo') e colunas [j0,j1) de C
static void calcula(const Args *a, int i, int j0, int j1) {
    int n = a->n;
    if (a->ikj) {
        for (int j = j0; j < j1; j++) a->C[(size_t)i*n+j] = 0;
        for (int k = 0; k < n; k++) {
            double x = a->A[(size_t)i*n+k];
            for (int j = j0; j < j1; j++) a->C[(size_t)i*n+j] += x * a->B[(size_t)k*n+j];
        }
    } else {
        for (int j = j0; j < j1; j++) {
            double s = 0;
            for (int k = 0; k < n; k++) s += a->A[(size_t)i*n+k] * a->B[(size_t)k*n+j];
            a->C[(size_t)i*n+j] = s;
        }
    }
}

static void *trabalho(void *p) {
    Args *a = p;
    int n = a->n, T = a->nthreads;
    if (a->est == LINHAS) {
        int base = n / T, resto = n % T;
        int ini = a->id * base + (a->id < resto ? a->id : resto);
        int fim = ini + base + (a->id < resto ? 1 : 0);
        for (int i = ini; i < fim; i++) calcula(a, i, 0, n);
    } else if (a->est == CICLICO) {
        for (int i = a->id; i < n; i += T) calcula(a, i, 0, n);
    } else { // BLOCOS
        int bs = a->bs, nb = (n + bs - 1) / bs, tarefa = 0;
        for (int bi = 0; bi < nb; bi++)
            for (int bj = 0; bj < nb; bj++, tarefa++) {
                if (tarefa % T != a->id) continue;
                int i1 = (bi+1)*bs < n ? (bi+1)*bs : n;
                int j1 = (bj+1)*bs < n ? (bj+1)*bs : n;
                for (int i = bi*bs; i < i1; i++) calcula(a, i, bj*bs, j1);
            }
    }
    return NULL;
}

static void multiplica(const double *A, const double *B, double *C, int n, int T,
                       Estrategia est, int ikj, int bs) {
    pthread_t *th = malloc(T * sizeof(pthread_t));
    Args *args = malloc(T * sizeof(Args));
    for (int t = 0; t < T; t++) {
        args[t] = (Args){ t, T, n, bs, ikj, est, A, B, C };
        pthread_create(&th[t], NULL, trabalho, &args[t]);
    }
    for (int t = 0; t < T; t++) pthread_join(th[t], NULL);
    free(th); free(args);
}

int main(int argc, char **argv) {
    if (argc < 3) {
        fprintf(stderr, "Uso: %s N THREADS [linhas|ciclico|blocos] [ijk|ikj] [repeticoes] [tam_bloco]\n", argv[0]);
        return 1;
    }
    int n = atoi(argv[1]), T = atoi(argv[2]);
    const char *se = argc > 3 ? argv[3] : "linhas";
    const char *ordem = argc > 4 ? argv[4] : "ikj";
    int reps = argc > 5 ? atoi(argv[5]) : 5;
    int bs = argc > 6 ? atoi(argv[6]) : 64;
    if (n <= 0 || T <= 0 || reps <= 0 || bs <= 0) { fprintf(stderr, "Parametros invalidos\n"); return 1; }

    Estrategia est = !strcmp(se, "ciclico") ? CICLICO : !strcmp(se, "blocos") ? BLOCOS : LINHAS;
    int ikj = strcmp(ordem, "ijk") != 0;

    double *A = aloca(n), *B = aloca(n), *C = aloca(n);
    inicializa(A, B, n);

    double soma = 0, menor = 1e300;
    for (int r = 0; r < reps; r++) {
        double t0 = agora();
        multiplica(A, B, C, n, T, est, ikj, bs);
        double t = agora() - t0;
        soma += t; if (t < menor) menor = t;
    }
    printf("par,%d,%d,%s,%s,%.6f,%.6f,%.6f\n", n, T, se, ikj ? "ikj" : "ijk", soma / reps, menor, checksum(C, n));
    free(A); free(B); free(C);
    return 0;
}
