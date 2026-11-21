#!/bin/bash
# run_bench.sh - roda todos os experimentos da Questao 2 (Java) e grava resultados_<maquina>.csv
# Uso: ./run_bench.sh NOME_DA_MAQUINA        (ex.: ./run_bench.sh beatriz)
# Requisitos: JDK 11+ (javac e java no PATH). Linux, macOS, WSL ou Git Bash.
set -e
cd "$(dirname "$0")"
MAQ=${1:-maquina}
REPS=5

rm -rf bin && mkdir bin
javac -d bin Common.java MatMulSeq.java MatMulPar.java
SEQ="java -cp bin MatMulSeq"
PAR="java -cp bin MatMulPar"

# numero de nucleos logicos (Linux/macOS/WSL)
NCPU=$(getconf _NPROCESSORS_ONLN 2>/dev/null || nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
# nome da CPU
if command -v lscpu >/dev/null 2>&1; then
  CPU=$(lscpu | grep 'Model name' | sed 's/.*: *//')
elif command -v sysctl >/dev/null 2>&1; then
  CPU=$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo desconhecida)
else
  CPU=desconhecida
fi
JAVAVER=$(java -version 2>&1 | grep -v 'Picked up' | head -n 1)

# quantidades de threads: 1, 2, 4, nº de nucleos e o dobro (sem repetir)
THREADS=$( { echo 1; echo 2; echo 4; echo $NCPU; echo $((2*NCPU)); } | sort -n | uniq | tr '\n' ' ')
TAMANHOS="200 400 600 800 1000 1500 2000"   # reduza se a maquina for lenta
N_FIXO=1000                                   # tamanho usado na busca do melhor bloco

OUT="resultados_${MAQ}.csv"
{
  echo "# maquina=$MAQ"
  echo "# cpu=$CPU"
  echo "# nucleos_logicos=$NCPU"
  echo "# java=$JAVAVER"
  echo "versao,N,threads,estrategia,ordem,tempo_medio_s,tempo_min_s,checksum"
} > "$OUT"

echo ">> Sequencial (ijk e ikj)"
for N in $TAMANHOS; do
  for O in ijk ikj; do $SEQ $N $O $REPS >> "$OUT"; done
done

echo ">> Paralelo: estrategia x ordem x threads x tamanho"
for N in $TAMANHOS; do
  for T in $THREADS; do
    for E in linhas ciclico blocos; do
      for O in ijk ikj; do
        echo "   N=$N T=$T $E $O"
        $PAR $N $T $E $O $REPS >> "$OUT"
      done
    done
  done
done

echo ">> Tamanhos de bloco (estrategia blocos, N=$N_FIXO, T=$NCPU)"
for BS in 16 32 64 128 256; do
  $PAR $N_FIXO $NCPU blocos ikj $REPS $BS | sed "s/,blocos,/,blocos_bs$BS,/" >> "$OUT"
done
echo "Pronto: $OUT"
