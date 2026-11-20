#!/bin/bash
# run_bench.sh - roda todos os experimentos da Questao 1 e grava resultados_<maquina>.csv
# Uso: ./run_bench.sh NOME_DA_MAQUINA        (ex.: ./run_bench.sh ruthelene)
# Obs.: no Windows use WSL ou MinGW (gcc com pthread).
# Para um teste rapido: TAMANHOS="1000 10000" ./run_bench.sh teste
set -e
MAQ=${1:-maquina}
REPS=5
gcc -O2 -pthread -o dotprod_seq dotprod_seq.c
gcc -O2 -pthread -o dotprod_par dotprod_par.c

NCPU=$( (nproc 2>/dev/null) || echo 4 )
# quantidades de threads: 1,2,4,... ate 2*nucleos (inclui o numero de nucleos)
THREADS=$(python3 - <<PY
n=$NCPU
s={1,2,4,n,2*n}
s|={x for x in (8,16) if x<=2*n}
print(" ".join(map(str,sorted(s))))
PY
)
TAMANHOS=${TAMANHOS:-"1000 10000 100000 1000000 10000000"}   # N = tamanho do vetor

OUT="resultados_${MAQ}.csv"
{
  echo "# maquina=$MAQ"
  echo "# cpu=$(lscpu 2>/dev/null | grep 'Model name' | sed 's/.*: *//')"
  echo "# nucleos_logicos=$NCPU"
  echo "versao,N,threads,tempo_medio_s,tempo_min_s,checksum"
} > "$OUT"

echo ">> Sequencial"
for N in $TAMANHOS; do ./dotprod_seq $N $REPS >> "$OUT"; done

echo ">> Paralelo: N x threads"
for N in $TAMANHOS; do
  for T in $THREADS; do
    echo "   N=$N T=$T"
    ./dotprod_par $N $T $REPS >> "$OUT"
  done
done
echo "Pronto: $OUT"
