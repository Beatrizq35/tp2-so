#!/usr/bin/env python3
"""analyze.py - le resultados_*.csv da Questao 1, calcula Speedup (Sp = Ts/Tp) e gera tabelas e graficos.

Uso: python3 analyze.py resultados_*.csv     (ou liste os arquivos; padroes com * sao expandidos pelo script)
Requer: pandas, matplotlib, tabulate  (pip install pandas matplotlib tabulate)

Referencia principal: TEMPO MINIMO (tempo_min_s) das repeticoes - menos sensivel a interrupcoes.
Tambem sao geradas as tabelas com a MEDIA (tempo_medio_s), para comparacao.
  Sp_min = Ts_min / Tp_min   (Ts_min = menor tempo sequencial do mesmo N)
  Sp_med = Ts_med / Tp_med
  Ef = Sp / p                (eficiencia)
Tambem verifica se o checksum e o mesmo em todas as versoes para cada N."""
import sys, io, glob
import pandas as pd
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt

# aceita padroes com * (o PowerShell nao expande, entao o proprio script expande)
arquivos = []
for arg in sys.argv[1:]:
    achados = sorted(glob.glob(arg))
    if not achados:
        sys.exit(f"Nenhum arquivo encontrado para: {arg}")
    arquivos.extend(achados)
if not arquivos:
    sys.exit("Uso: python analyze.py resultados_*.csv")

dfs = []
for f in arquivos:
    meta, linhas = {}, []
    for l in open(f, encoding="utf-8-sig"):
        if l.startswith("#"):
            k, _, v = l[1:].strip().partition("=")
            meta[k] = v
        else:
            linhas.append(l)
    d = pd.read_csv(io.StringIO("".join(linhas)))
    d["maquina"] = meta.get("maquina", f)
    d["cpu"] = meta.get("cpu", "?")
    d["nucleos"] = int(meta.get("nucleos_logicos", 0))
    d["repeticoes"] = meta.get("repeticoes", "?")
    dfs.append(d)
df = pd.concat(dfs, ignore_index=True)

# --- validacao: checksum igual em todas as versoes de cada (maquina, N)
ck = df.groupby(["maquina", "N"]).checksum.nunique()
if (ck > 1).any():
    print("ATENCAO: checksums diferentes para:\n", ck[ck > 1])
else:
    print("Validacao OK: checksum identico em todas as versoes para cada N.")

seq = df[df.versao == "seq"]
par = df[df.versao == "par"].copy()
chave = ["maquina", "N"]
par = par.join(seq.groupby(chave).tempo_medio_s.min().rename("Ts_med"), on=chave)
par = par.join(seq.groupby(chave).tempo_min_s.min().rename("Ts_min"), on=chave)
par["Sp_min"] = par.Ts_min / par.tempo_min_s
par["Sp_med"] = par.Ts_med / par.tempo_medio_s
par["Ef_min"] = par.Sp_min / par.threads
par["Ef_med"] = par.Sp_med / par.threads
par.to_csv("speedup_completo.csv", index=False)

def tabela(g, valores, fmt):
    return g.pivot(index="N", columns="threads", values=valores).round(fmt).to_markdown()

with open("tabelas.md", "w", encoding="utf-8") as out:
    for maq, g in par.groupby("maquina"):
        info = g.iloc[0]
        out.write(f"\n## {maq} - {info.cpu} ({info.nucleos} nucleos logicos, {info.repeticoes} repeticoes)\n")

        out.write("\n### REFERENCIA: tempo minimo (s) - paralelo\n\n" + tabela(g, "tempo_min_s", 6) + "\n")
        out.write("\n### REFERENCIA: Speedup pelo tempo minimo (Sp = Ts_min / Tp_min)\n\n" + tabela(g, "Sp_min", 3) + "\n")
        out.write("\n### REFERENCIA: Eficiencia pelo tempo minimo (Ef = Sp / p)\n\n" + tabela(g, "Ef_min", 3) + "\n")

        out.write("\n### Comparacao: tempo medio (s) - paralelo\n\n" + tabela(g, "tempo_medio_s", 6) + "\n")
        out.write("\n### Comparacao: Speedup pela media (Sp_med = Ts_med / Tp_med)\n\n" + tabela(g, "Sp_med", 3) + "\n")

    out.write("\n## Tempo sequencial (s), por maquina\n")
    for col, titulo in [("tempo_min_s", "REFERENCIA: tempo minimo"),
                        ("tempo_medio_s", "Comparacao: tempo medio")]:
        tab = (seq.pivot_table(index="N", columns="maquina", values=col)
                  .round(6).rename_axis(index="N", columns=None))
        out.write(f"\n### {titulo}\n\n" + tab.to_markdown() + "\n")

for maq, g in par.groupby("maquina"):
    s = seq[seq.maquina == maq].groupby("N").tempo_min_s.min()
    nuc = int(g.nucleos.iloc[0])
    t_sel = nuc if nuc in set(g.threads) else g.threads.max()
    p = g[g.threads == t_sel].set_index("N").tempo_min_s

    # 1) tempo x N (tempo minimo): sequencial vs paralelo com nucleos threads
    plt.figure(figsize=(6, 4))
    plt.plot(s.index, s.values, "o-", label="Sequencial")
    plt.plot(p.index, p.values, "s-", label=f"Paralelo ({t_sel} threads)")
    plt.xscale("log"); plt.yscale("log")
    plt.xlabel("Tamanho do vetor N"); plt.ylabel("Tempo minimo (s)")
    plt.title(f"Tempo x N - {maq}")
    plt.legend(); plt.grid(alpha=.3, which="both"); plt.tight_layout()
    plt.savefig(f"grafico_tempo_{maq}.png", dpi=150); plt.close()

    # 2) speedup x threads (pelo tempo minimo), para varios N
    plt.figure(figsize=(6, 4))
    for N, gn in g.groupby("N"):
        gn = gn.sort_values("threads")
        plt.plot(gn.threads, gn.Sp_min, "o-", label=f"N={N}")
    mx = g.threads.max()
    plt.plot([1, mx], [1, mx], "k--", alpha=.4, label="Ideal")
    plt.axhline(1, color="gray", lw=.8, ls=":")
    plt.xlabel("Threads"); plt.ylabel("Speedup (Ts_min/Tp_min)")
    plt.title(f"Speedup x threads - {maq}")
    plt.legend(fontsize=7); plt.grid(alpha=.3); plt.tight_layout()
    plt.savefig(f"grafico_speedup_{maq}.png", dpi=150); plt.close()

print("Gerados: speedup_completo.csv, tabelas.md e graficos *.png")
