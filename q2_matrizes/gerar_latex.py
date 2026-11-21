#!/usr/bin/env python3
"""gerar_latex.py - gera tabelas em LaTeX da Questao 2 a partir do speedup_completo.csv
(produzido pelo analyze.py).
Uso (dentro de resultados/analise/q2/):
    python3 ../../../q2_matrizes/gerar_latex.py
Saida: tabelas_q2.tex (colar no relatorio) e um resumo no terminal."""
import sys
import pandas as pd

arq = sys.argv[1] if len(sys.argv) > 1 else "speedup_completo.csv"
df = pd.read_csv(arq)
base = df[~df.estrategia.str.contains("_bs")]
Nmax = int(base.N.max())

def f(x, casas=2):
    return f"{x:.{casas}f}".replace(".", ",")

def tex(s):
    return str(s).replace("_", r"\_")

nome_est = {"linhas": "Linhas contíguas", "ciclico": "Linhas cíclicas", "blocos": "Blocos"}
out = []

# ---- Tabela 1: speedup da melhor configuracao por maquina x threads (N = Nmax)
g = base[base.N == Nmax]
melhor = g.loc[g.groupby(["maquina", "threads"]).tempo_medio_s.idxmin()]
tb = melhor.pivot(index="maquina", columns="threads", values="Sp")
ths = list(tb.columns)
out.append(r"""\begin{table}[H]
\centering
\small
\caption{Questão 2: \textit{speedup} da melhor configuração paralela para $N=%d$, por máquina e número de \textit{threads}.}
\label{tab:q2-speedup}
\begin{tabular}{l%s}
\toprule
\textbf{Máquina} & %s \\
\midrule""" % (Nmax, "r" * len(ths), " & ".join(f"\\textbf{{{t}}}" for t in ths)))
for maq, linha in tb.iterrows():
    out.append(f"{tex(maq)} & " + " & ".join(f(v) for v in linha.values) + r" \\")
out.append(r"""\bottomrule
\end{tabular}
\end{table}
""")

# ---- Tabela 2: estrategias x ordem (N = Nmax, T = nº de nucleos) por maquina
for maq, gm in base.groupby("maquina"):
    nuc = int(gm.nucleos.iloc[0])
    T = nuc if nuc in set(gm.threads) else int(gm.threads.max())
    gt = gm[(gm.N == Nmax) & (gm.threads == T)].copy()
    gt["ordem_ord"] = gt.ordem.map({"ijk": 0, "ikj": 1})
    gt["est_ord"] = gt.estrategia.map({"linhas": 0, "ciclico": 1, "blocos": 2})
    gt = gt.sort_values(["est_ord", "ordem_ord"])
    ts = gt.Ts_melhor.iloc[0]
    out.append(r"""\begin{table}[H]
\centering
\small
\caption{Questão 2: comparação das estratégias de divisão (%s, $N=%d$, $T=%d$ \textit{threads}; $T_s=%s$~s da melhor versão sequencial).}
\label{tab:q2-estrategias-%s}
\begin{tabular}{llrrr}
\toprule
\textbf{Estratégia} & \textbf{Ordem} & \textbf{Tempo (s)} & \textbf{$S_p$} & \textbf{$E_p$} \\
\midrule""" % (tex(maq), Nmax, T, f(ts, 4), maq))
    for _, r in gt.iterrows():
        out.append(f"{nome_est.get(r.estrategia, r.estrategia)} & \\texttt{{{r.ordem}}} & {f(r.tempo_medio_s, 4)} & {f(r.Sp)} & {f(r.eficiencia)} \\\\")
    out.append(r"""\bottomrule
\end{tabular}
\end{table}
""")

# ---- Tabela 3: tamanho do bloco
bs = df[df.estrategia.str.contains("_bs")].copy()
if len(bs):
    bs["BS"] = bs.estrategia.str.extract(r"_bs(\d+)").astype(int)
    for maq, gm in bs.groupby("maquina"):
        gm = gm.sort_values("BS")
        out.append(r"""\begin{table}[H]
\centering
\small
\caption{Questão 2: efeito do tamanho do bloco (%s, estratégia de blocos, $N=%d$, $T=%d$ \textit{threads}).}
\label{tab:q2-blocos-%s}
\begin{tabular}{rrr}
\toprule
\textbf{$BS$} & \textbf{Tempo (s)} & \textbf{$S_p$} \\
\midrule""" % (tex(maq), int(gm.N.iloc[0]), int(gm.threads.iloc[0]), maq))
        for _, r in gm.iterrows():
            out.append(f"{r.BS} & {f(r.tempo_medio_s, 4)} & {f(r.Sp)} \\\\")
        out.append(r"""\bottomrule
\end{tabular}
\end{table}
""")

open("tabelas_q2.tex", "w", encoding="utf-8").write("\n".join(out))

# ---- Resumo no terminal (para escrever a analise)
print("Gerado: tabelas_q2.tex\n")
for maq, gm in base.groupby("maquina"):
    gn = gm[gm.N == Nmax]
    top = gn.loc[gn.tempo_medio_s.idxmin()]
    print(f"[{maq}] melhor configuracao (N={Nmax}): {top.threads} threads, estrategia={top.estrategia}, "
          f"ordem={top.ordem}, tempo={top.tempo_medio_s:.4f}s, Sp={top.Sp:.2f}, Ep={top.eficiencia:.2f}")
