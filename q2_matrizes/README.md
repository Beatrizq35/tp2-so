# Questão 2 – Multiplicação de matrizes (Java + Threads)

## Arquivos
- `Common.java` – inicialização determinística, checksum, tempo e formato da linha CSV
- `MatMulSeq.java` – versão sequencial (ordens de laços `ijk` e `ikj`)
- `MatMulPar.java` – versão paralela (classe `Thread`), nº de threads escolhido pelo usuário, 3 estratégias de divisão
- `run_bench.sh` – roda todos os experimentos (média de 5 execuções) e gera `resultados_<maquina>.csv`
- `analyze.py` – calcula Speedup, gera `tabelas.md`, `speedup_completo.csv` e gráficos PNG

## Requisitos
JDK 11 ou superior (`javac` e `java` no PATH). Verifique com `javac -version`.

## Uso manual
    mkdir -p bin
    javac -d bin Common.java MatMulSeq.java MatMulPar.java
    java -cp bin MatMulSeq 1000 ikj 5            # N=1000, ordem ikj, 5 repetições
    java -cp bin MatMulPar 1000 4 linhas ikj 5   # N=1000, 4 threads
    java -cp bin MatMulPar 1000 4 blocos ikj 5 64  # blocos 64x64

## Experimentos (cada colega, na própria máquina)
    ./run_bench.sh nome          # ex.: ./run_bench.sh jonathas
O script compila o código, detecta CPU e nº de núcleos (Linux, macOS, WSL ou Git Bash) e registra o resultado.
Depois, junte os CSVs em `../resultados/q2/` e rode:
    pip install pandas matplotlib tabulate
    python3 analyze.py ../resultados/q2/resultados_*.csv

## Estratégias paralelas
- `linhas`: cada thread recebe uma faixa contígua de linhas de C
- `ciclico`: linhas intercaladas (i, i+T, i+2T, ...) – melhor balanceamento
- `blocos`: C dividida em tiles BSxBS distribuídos ciclicamente

## Observações sobre Java
- **Aquecimento (JIT):** cada execução do programa faz 1 multiplicação *não medida* antes das 5 medidas, para que o compilador JIT da JVM já tenha otimizado o código.
- **Memória:** N=2000 usa ~100 MB. Se aparecer `OutOfMemoryError`, rode com `java -Xmx2g -cp bin ...` ou reduza `TAMANHOS` no script.
- **Speedup:** Sp = Ts / Tp (Ts = melhor sequencial para o mesmo N; a coluna `Sp_mesmo_algoritmo` usa a mesma ordem de laços).
- O checksum impresso deve ser idêntico em todas as versões (validação do resultado).
