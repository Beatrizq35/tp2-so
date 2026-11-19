# Trabalho Prático 2 – Sistemas Operacionais (UFAM)
Programação paralela com threads (C + pthreads). Entrega: 27/11/2026 (ColabWeb).

## Estrutura
| Pasta | Conteúdo | Responsável |
|---|---|---|
| `q1_produto_escalar/` | Produto escalar sequencial e paralelo | Ruthelene |
| `q2_matrizes/` | Multiplicação de matrizes sequencial e paralela | Beatriz |
| `q3_asteroids/` | Jogo estilo Asteroids (com e sem threads) | Leticia e Jonathas |
| `resultados/q1`, `resultados/q2` | CSV gerado em cada máquina (`resultados_<nome>.csv`) | todos |
| `resultados/analise/` | Tabelas e gráficos gerados pelo `analyze.py` | quem consolida |
| `relatorio/` | Relatório em LaTeX (modelo SBC) | todos |

## Rodar os experimentos (Q1 e Q2)
Linux ou WSL, com `gcc`:

    cd q2_matrizes
    ./run_bench.sh seunome        # gera resultados_seunome.csv
    mv resultados_seunome.csv ../resultados/q2/

## Gerar tabelas e gráficos
    pip install pandas matplotlib tabulate
    cd resultados/analise/q2
    python3 ../../../q2_matrizes/analyze.py ../../q2/resultados_*.csv

Copie os PNGs desejados para `relatorio/figuras/`.

## Padrão combinado
C com pthreads, `gcc -O2 -pthread`, média de 5 execuções, mesma inicialização determinística dos dados.
Registrar qualquer uso de IA, site ou livro (exigência do enunciado) na seção "Registro de Fontes" do relatório.
