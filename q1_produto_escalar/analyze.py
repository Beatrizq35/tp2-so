#!/usr/bin/env python3
"""analyze.py - le resultados_*.csv da Questao 1, calcula Speedup (Sp = Ts/Tp) e gera tabelas e graficos.
Uso: python3 analyze.py resultados_ruthelene.csv resultados_beatriz.csv ...
Requer: pandas, matplotlib, tabulate  (pip install pandas matplotlib tabulate)
Ts = tempo sequencial medio (tempo_medio_s da versao 'seq') para o mesmo N.
Tp = tempo paralelo medio com T threads.  Sp = Ts / Tp.
Tambem verifica se o checksum e o mesmo em todas as versoes para cada N (validacao)."""
import sys, io
import pandas as pd
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt

dfs = []
for f in sys.argv[1:]:
    meta, linhas = {}, []
    for l in open(f, encoding="utf-8"):
        if l.startswith("#"):
            k, _, v = l[1:].strip().partition("=")
            meta[k] = v
        else:
            linhas.append(l)
    d = pd.read_csv(io.StringIO("".join(linhas)))
    d["maquina"] = meta.get("maquina", f)
    d["cpu"] = meta.get("cpu", "?")
    d["nucleos"] = int(meta.get("nucleos_logicos", 0))
    dfs.append(d)
df = pd.concat(dfs, ignore_index=True)

# --- validacao: checksum igual em todas as versoes de cada (maquina, N)
ck = df.groupby(["maquina", "N"]).checksum.nunique()
if (ck > 1).any():
    print("ATENCAO: checksums diferentes para:\n", ck[ck > 1])
else:
    print("Validacao OK: checksum identico em todas as versoes para cada N.")

seq = df[df.versao == "seq"]
ts = seq.groupby(["maquina", "N"]).tempo_medio_s.min().rename("Ts")
par = df[df.versao == "par"].copy()
par = par.join(ts, on=["maquina", "N"])
par["Sp"] = par.Ts / par.tempo_medio_s
par["eficiencia"] = par.Sp / par.threads
par.to_csv("speedup_completo.csv", index=False)

with open("tabelas.md", "w", encoding="utf-8") as out:
    for maq, g in par.groupby("maquina"):
        info = g.iloc[0]
        out.write(f"\n## {maq} - {info.cpu} ({info.nucleos} nucleos logicos)\n")
        tb_t = g.pivot(index="N", columns="threads", values="tempo_medio_s").round(6)
        out.write("\n### Tempo medio (s) - paralelo, por N e threads\n\n" + tb_t.to_markdown() + "\n")
        tb_s = g.pivot(index="N", columns="threads", values="Sp").round(3)
        out.write("\n### Speedup Sp = Ts/Tp\n\n" + tb_s.to_markdown() + "\n")
        tb_e = g.pivot(index="N", columns="threads", values="eficiencia").round(3)
        out.write("\n### Eficiencia Ef = Sp/p\n\n" + tb_e.to_markdown() + "\n")
    out.write("\n## Tempo sequencial (s)\n\n" +
              seq.pivot_table(index="N", columns="maquina", values="tempo_medio_s")
                 .round(6).to_markdown() + "\n")

for maq, g in par.groupby("maquina"):
    s = seq[seq.maquina == maq].set_index("N").tempo_medio_s
    # 1) tempo x N: sequencial vs paralelo (threads = nucleos, se testado)
    nuc = int(g.nucleos.iloc[0])
    t_sel = nuc if nuc in set(g.threads) else g.threads.max()
    p = g[g.threads == t_sel].set_index("N").tempo_medio_s
    plt.figure(figsize=(6, 4))
    plt.plot(s.index, s.values, "o-", label="Sequencial")
    plt.plot(p.index, p.values, "s-", label=f"Paralelo ({t_sel} threads)")
    plt.xscale("log"); plt.yscale("log")
    plt.xlabel("Tamanho do vetor N"); plt.ylabel("Tempo (s)")
    plt.title(f"Tempo x N - {maq}")
    plt.legend(); plt.grid(alpha=.3, which="both"); plt.tight_layout()
    plt.savefig(f"grafico_tempo_{maq}.png", dpi=150); plt.close()

    # 2) speedup x threads, para varios N
    plt.figure(figsize=(6, 4))
    for N, gn in g.groupby("N"):
        gn = gn.sort_values("threads")
        plt.plot(gn.threads, gn.Sp, "o-", label=f"N={N}")
    mx = g.threads.max()
    plt.plot([1, mx], [1, mx], "k--", alpha=.4, label="Ideal")
    plt.xlabel("Threads"); plt.ylabel("Speedup (Ts/Tp)")
    plt.title(f"Speedup x threads - {maq}")
    plt.legend(fontsize=7); plt.grid(alpha=.3); plt.tight_layout()
    plt.savefig(f"grafico_speedup_{maq}.png", dpi=150); plt.close()

print("Gerados: speedup_completo.csv, tabelas.md e graficos *.png")
