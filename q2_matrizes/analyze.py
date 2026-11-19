#!/usr/bin/env python3
"""analyze.py - le resultados_*.csv, calcula Speedup (Sp = Ts/Tp) e gera tabelas e graficos.
Uso: python3 analyze.py resultados_beatriz.csv resultados_leticia.csv ...
Requer: pandas, matplotlib  (pip install pandas matplotlib)
Ts = tempo da MELHOR versao sequencial (menor entre ijk e ikj) para o mesmo N.
Tambem e gerada a coluna Sp_mesmo_algoritmo (Ts com a mesma ordem de lacos da versao paralela)."""
import sys, io, pandas as pd
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt

dfs = []
for f in sys.argv[1:]:
    meta = {}
    linhas = []
    for l in open(f, encoding="utf-8"):
        if l.startswith("#"):
            k, _, v = l[1:].strip().partition("="); meta[k] = v
        else:
            linhas.append(l)
    d = pd.read_csv(io.StringIO("".join(linhas)))
    d["maquina"] = meta.get("maquina", f)
    d["cpu"] = meta.get("cpu", "?")
    d["nucleos"] = int(meta.get("nucleos_logicos", 0))
    dfs.append(d)
df = pd.concat(dfs, ignore_index=True)

seq = df[df.versao == "seq"]
ts_melhor = seq.groupby(["maquina", "N"]).tempo_medio_s.min().rename("Ts_melhor")
ts_ordem = seq.set_index(["maquina", "N", "ordem"]).tempo_medio_s.rename("Ts_ordem")
par = df[df.versao == "par"].copy()
par = par.join(ts_melhor, on=["maquina", "N"]).join(ts_ordem, on=["maquina", "N", "ordem"])
par["Sp"] = par.Ts_melhor / par.tempo_medio_s
par["Sp_mesmo_algoritmo"] = par.Ts_ordem / par.tempo_medio_s
par["eficiencia"] = par.Sp / par.threads
par.to_csv("speedup_completo.csv", index=False)

with open("tabelas.md", "w", encoding="utf-8") as out:
    for maq, g in par.groupby("maquina"):
        info = g.iloc[0]
        out.write(f"\n## {maq} - {info.cpu} ({info.nucleos} nucleos logicos)\n")
        # tabela de speedup: melhor estrategia/ordem por (N, threads)
        g2 = g[~g.estrategia.str.contains("_bs")]
        melhor = g2.loc[g2.groupby(["N", "threads"]).tempo_medio_s.idxmin()]
        tb = melhor.pivot(index="N", columns="threads", values="Sp").round(2)
        out.write("\n### Speedup (melhor configuracao paralela por N e threads)\n\n" + tb.to_markdown() + "\n")
        # melhor configuracao global para o maior N
        Nmax = g2.N.max()
        top = g2[g2.N == Nmax].sort_values("tempo_medio_s").head(5)
        out.write(f"\n### Top 5 configuracoes (N={Nmax})\n\n" +
                  top[["threads", "estrategia", "ordem", "tempo_medio_s", "Sp", "eficiencia"]].round(4).to_markdown(index=False) + "\n")
        # estrategias comparadas
        comp = g2[g2.N == Nmax].pivot_table(index="threads", columns=["estrategia", "ordem"], values="tempo_medio_s").round(4)
        out.write(f"\n### Tempo (s) por estrategia/ordem, N={Nmax}\n\n" + comp.to_markdown() + "\n")
        bs = g[g.estrategia.str.contains("_bs")]
        if len(bs):
            out.write("\n### Efeito do tamanho do bloco\n\n" +
                      bs[["N", "threads", "estrategia", "tempo_medio_s", "Sp"]].round(4).to_markdown(index=False) + "\n")

for maq, g in par.groupby("maquina"):
    g2 = g[~g.estrategia.str.contains("_bs")]
    # 1) tempo x tamanho: sequencial (melhor) vs paralelo (melhor config c/ nº de threads = nucleos)
    nuc = int(g.nucleos.iloc[0])
    t_sel = nuc if nuc in set(g2.threads) else g2.threads.max()
    p = g2[g2.threads == t_sel]; p = p.loc[p.groupby("N").tempo_medio_s.idxmin()]
    s = seq[seq.maquina == maq].groupby("N").tempo_medio_s.min()
    plt.figure(figsize=(6, 4))
    plt.plot(s.index, s.values, "o-", label="Sequencial")
    plt.plot(p.N, p.tempo_medio_s, "s-", label=f"Paralelo ({t_sel} threads)")
    plt.xlabel("Tamanho N (matrizes NxN)"); plt.ylabel("Tempo (s)"); plt.title(f"Tempo x N - {maq}")
    plt.legend(); plt.grid(alpha=.3); plt.tight_layout(); plt.savefig(f"grafico_tempo_{maq}.png", dpi=150); plt.close()
    # 2) speedup x threads para varios N
    plt.figure(figsize=(6, 4))
    for N, gn in g2.groupby("N"):
        m = gn.loc[gn.groupby("threads").tempo_medio_s.idxmin()].sort_values("threads")
        plt.plot(m.threads, m.Sp, "o-", label=f"N={N}")
    mx = g2.threads.max(); plt.plot([1, mx], [1, mx], "k--", alpha=.4, label="Ideal")
    plt.xlabel("Threads"); plt.ylabel("Speedup"); plt.title(f"Speedup x threads - {maq}")
    plt.legend(fontsize=7); plt.grid(alpha=.3); plt.tight_layout(); plt.savefig(f"grafico_speedup_{maq}.png", dpi=150); plt.close()
print("Gerados: speedup_completo.csv, tabelas.md e graficos *.png")
