# Questão 1 – Produto escalar (C + pthreads) — responsável: Ruthelene

## Arquivos
- `common.h` – tempo, alocação e inicialização determinística dos vetores
- `dotprod_seq.c` – versão sequencial
- `dotprod_par.c` – versão paralela; nº de threads escolhido pelo usuário; cada thread recebe um bloco contíguo do vetor (a última pega o resto)
- `run_bench.sh` – roda todos os experimentos (média de 5 execuções) e gera `resultados_<maquina>.csv`
- `analyze.py` – valida os checksums, calcula Speedup (Sp = Ts/Tp), gera `tabelas.md`, `speedup_completo.csv` e gráficos PNG

## Uso manual
    gcc -O2 -pthread -o dotprod_seq dotprod_seq.c
    gcc -O2 -pthread -o dotprod_par dotprod_par.c
    ./dotprod_seq 1000000 5            # N=1.000.000, 5 repeticoes
    ./dotprod_par 1000000 4 5          # N=1.000.000, 4 threads, 5 repeticoes

## Experimentos (cada colega, na própria máquina)
    ./run_bench.sh nome                # ex.: ./run_bench.sh ruthelene
    mv resultados_nome.csv ../resultados/q1/

Juntar os CSVs e gerar tabelas e gráficos (dentro de `resultados/analise/q1/`):
    pip install pandas matplotlib tabulate
    cd resultados/analise/q1
    python3 ../../../q1_produto_escalar/analyze.py ../../q1/resultados_*.csv

Copie para `relatorio/figuras/` os PNGs que forem usar no texto.

## Validação
O checksum (soma de todos os produtos, inteiro exato) deve ser idêntico em todas as versões para cada N. O `analyze.py` confere isso e avisa se algo divergir.
Os valores dos vetores são inteiros pequenos guardados em `double`, então a soma é exata e não depende da ordem das somas.

## Métrica
Speedup Sp = Ts / Tp, onde Ts = tempo sequencial médio (mesmo N) e Tp = tempo paralelo médio com p threads.
Eficiência Ef = Sp / p.

## Observações para o relatório
- O tempo paralelo inclui criar e juntar as threads (overhead). Para N pequeno, o overhead pode fazer a versão paralela ser mais lenta.
- O produto escalar é limitado pela largura de banda de memória, então o ganho tende a ficar abaixo do número de núcleos.
- Com mais threads que núcleos, o speedup estagna ou cai.
