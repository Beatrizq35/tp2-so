# Questão 2 – Multiplicação de matrizes (C + pthreads)

## Arquivos
- `matmul_seq.c` – versão sequencial (ordens de laços `ijk` e `ikj`)
- `matmul_par.c` – versão paralela, nº de threads escolhido pelo usuário, 3 estratégias de divisão
- `common.h` – tempo, alocação, inicialização determinística e checksum
- `run_bench.sh` – roda todos os experimentos (média de 5 execuções) e gera `resultados_<maquina>.csv`
- `analyze.py` – calcula Speedup, gera `tabelas.md`, `speedup_completo.csv` e gráficos PNG

## Uso manual
    gcc -O2 -pthread -o matmul_seq matmul_seq.c
    gcc -O2 -pthread -o matmul_par matmul_par.c
    ./matmul_seq 1000 ikj 5
    ./matmul_par 1000 4 linhas ikj 5          # N=1000, 4 threads
    ./matmul_par 1000 4 blocos ikj 5 64       # blocos 64x64

## Experimentos (cada colega, na própria máquina)
    ./run_bench.sh nome          # ex.: ./run_bench.sh jonathas
    # anotar: modelo da CPU e nº de núcleos (o script já registra via lscpu)
Juntar os CSVs e rodar:
    pip install pandas matplotlib tabulate
    python3 analyze.py resultados_beatriz.csv resultados_leticia.csv resultados_jonathas.csv ...

## Estratégias paralelas
- `linhas`: cada thread recebe um bloco contíguo de linhas de C
- `ciclico`: linhas intercaladas (i, i+T, i+2T, ...) – melhor balanceamento
- `blocos`: C dividida em tiles BSxBS distribuídos ciclicamente

Speedup: Sp = Ts / Tp (Ts = melhor sequencial para o mesmo N; coluna `Sp_mesmo_algoritmo` usa a mesma ordem de laços).
O checksum impresso deve ser idêntico em todas as versões (validação do resultado).
