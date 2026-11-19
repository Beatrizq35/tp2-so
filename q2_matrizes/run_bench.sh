#!/bin/bash
# run_bench.sh - roda todos os experimentos da Questao 2 e grava em resultados_<maquina>.csv
# Uso: ./run_bench.sh NOME_DA_MAQUINA        (ex.: ./run_bench.sh beatriz)
# Obs.: no Windows use WSL ou MinGW (gcc com pthread).
set -e
MAQ=${1:-maquina}
REPS=5
gcc -O2 -pthread -o matmul_seq matmul_seq.c
gcc -O2 -pthread -o matmul_par matmul_par.c

NCPU=$( (nproc 2>/dev/null) || echo 4 )
# quantidades de threads: 1,2,4,... ate 2*nucleos (inclui o numero de nucleos)
THREADS=$(python3 - <<PY
n=$NCPU
s={1,2,4,n,2*n}
s|={x for x in (8,16) if x<=2*n}
print(" ".join(map(str,sorted(s))))
PY
)
TAMANHOS="200 400 600 800 1000 1500 2000"   # reduza se a maquina for lenta
N_FIXO=1000                                   # tamanho usado na busca da melhor configuracao

OUT="resultados_${MAQ}.csv"
{
  echo "# maquina=$MAQ"
  echo "# cpu=$(lscpu 2>/dev/null | grep 'Model name' | sed 's/.*: *//')"
  echo "# nucleos_logicos=$NCPU"
  echo "versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum"
} > "$OUT"

echo ">> Sequencial (ijk e ikj)"
for N in $TAMANHOS; do
  for O in ijk ikj; do ./matmul_seq $N $O $REPS >> "$OUT"; done
done

echo ">> Paralelo: estrategia x ordem x threads x tamanho"
for N in $TAMANHOS; do
  for T in $THREADS; do
    for E in linhas ciclico blocos; do
      for O in ijk ikj; do
        echo "   N=$N T=$T $E $O"
        ./matmul_par $N $T $E $O $REPS >> "$OUT"
      done
    done
  done
done

echo ">> Tamanhos de bloco (estrategia blocos, N=$N_FIXO, T=$NCPU)"
for BS in 16 32 64 128 256; do
  ./matmul_par $N_FIXO $NCPU blocos ikj $REPS $BS | sed "s/,blocos,/,blocos_bs$BS,/" >> "$OUT"
done
echo "Pronto: $OUT"
