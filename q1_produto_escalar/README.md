# Questão 1 – Produto escalar (Java, classe Thread) — responsável: Ruthelene

## Arquivos
- `DotprodSeq.java` – versão sequencial
- `DotprodPar.java` – versão paralela; nº de threads escolhido pelo usuário; cada thread recebe um bloco contíguo do vetor (a última pega o resto)
- `run_bench.ps1` – versão para Windows (PowerShell) do script abaixo; repetições padrão: 30 (`-Repeticoes`)
- `run_bench.sh` – compila, roda todos os experimentos (30 repetições; mude com `REPS=5 ./run_bench.sh ...`) e gera `resultados_<maquina>.csv`
- `analyze.py` – valida os checksums, calcula Speedup (Sp = Ts/Tp), gera `tabelas.md`, `speedup_completo.csv` e gráficos PNG

## Requisitos
- JDK 11 ou superior (`javac -version`)
- Python 3 com pandas, matplotlib e tabulate, para o `analyze.py`

## Uso manual
    javac DotprodSeq.java DotprodPar.java
    java DotprodSeq 1000000 30         # N=1.000.000, 30 repetições
    java DotprodPar 1000000 4 30       # N=1.000.000, 4 threads, 30 repetições

## Experimentos (cada colega, na própria máquina)

Antes de medir: notebook conectado na tomada, plano de energia "Alto desempenho", outros programas fechados. O script avisa se o plano não estiver ativo; não meça enquanto o aviso aparecer.

### Windows (PowerShell) — use este
Entre na pasta da questão (não na raiz do repositório):

    cd "D:\Projetos\Faculdade\SO 2\tp2-so\q1_produto_escalar"
    powershell -ExecutionPolicy Bypass -File .\run_bench.ps1 -Maquina seunome
    Move-Item resultados_seunome.csv ..\resultados\q1\

Troque `seunome` pelo seu nome, sem espaços (ex.: `-Maquina ruthelene`). O arquivo `.sh` não roda no PowerShell.
Se a pasta `resultados\q1` não existir: `New-Item -ItemType Directory -Force ..\resultados\q1`.
Precisa do JDK no PATH: teste com `javac -version`.

### Linux / macOS / WSL
    ./run_bench.sh seunome
    mv resultados_seunome.csv ../resultados/q1/

Juntar os CSVs e gerar tabelas e gráficos (dentro de `resultados/analise/q1/`):
    pip install pandas matplotlib tabulate
    cd resultados/analise/q1
    python3 ../../../q1_produto_escalar/analyze.py ../../q1/resultados_*.csv

Copie para `relatorio/figuras/` os PNGs que forem usar no texto.

## Detalhes da implementação
- Vetores: valores inteiros pequenos em `double`, inicialização determinística. Produtos e somas são inteiros exatos, então o checksum é igual em todas as versões, independentemente da ordem das somas.
- Aquecimento do JIT: uma execução antes da medição, que não entra no tempo.
- O tempo paralelo inclui criar e juntar (join) as threads, como overhead.
- Se aparecer `OutOfMemoryError` com N = 10.000.000, rode com `java -Xmx1g ...`.

## Métrica
Speedup Sp = Ts / Tp, onde Ts = tempo sequencial médio (mesmo N) e Tp = tempo paralelo médio com p threads.
Eficiência Ef = Sp / p.

## Observações para o relatório
- Para N pequeno, o overhead de criar threads pode fazer a versão paralela ser mais lenta.
- O produto escalar é limitado pela largura de banda de memória, então o ganho tende a ficar abaixo do número de núcleos.
- Com mais threads que núcleos, o speedup estagna ou cai.
