# run_bench.ps1 - Questao 1 (Java) no Windows (PowerShell)
# Roda os experimentos e grava resultados_<maquina>.csv
#
# Uso (dentro da pasta q1_produto_escalar):
#   powershell -ExecutionPolicy Bypass -File .\run_bench.ps1 -Maquina ruthelene
#
# Teste rapido:
#   powershell -ExecutionPolicy Bypass -File .\run_bench.ps1 -Maquina teste -Tamanhos "1000 10000" -Repeticoes 3
# Requer JDK (javac e java) no PATH.

param(
    [string]$Maquina = "maquina",
    [string]$Tamanhos = "1000 10000 100000 1000000 10000000",
    [int]$Repeticoes = 30
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$reps = $Repeticoes
$utf8 = New-Object System.Text.UTF8Encoding $false

# aviso de medicao (nao interrompe o script)
$plano = (powercfg /getactivescheme) -join " "
if ($plano -notmatch "Alto desempenho|High performance") {
    Write-Host "AVISO: plano de energia nao e 'Alto desempenho'. Troque em Opcoes de Energia antes de medir." -ForegroundColor Yellow
}

# compila
javac DotprodSeq.java DotprodPar.java
if ($LASTEXITCODE -ne 0) { throw "Falha na compilacao (verifique o JDK: javac -version)" }

# threads: 1,2,4,nucleos,2*nucleos e 8,16 (se couber em 2*nucleos)
$ncpu = [Environment]::ProcessorCount
$threads = @(1, 2, 4, $ncpu, (2 * $ncpu)) + @(8, 16 | Where-Object { $_ -le 2 * $ncpu })
$threads = $threads | Sort-Object -Unique

$cpu = "?"
try { $cpu = (Get-CimInstance Win32_Processor | Select-Object -First 1).Name } catch { }

$out = Join-Path $PSScriptRoot "resultados_$Maquina.csv"
$cabecalho = @(
    "# maquina=$Maquina",
    "# cpu=$cpu",
    "# nucleos_logicos=$ncpu",
    "# repeticoes=$reps",
    "versao,N,threads,tempo_medio_s,tempo_min_s,checksum"
)
[IO.File]::WriteAllLines($out, $cabecalho, $utf8)

function Acrescenta($linha) {
    [IO.File]::AppendAllText($out, $linha + "`r`n", $utf8)
}

$tam = $Tamanhos -split '\s+' | Where-Object { $_ -ne "" }
Write-Host "Repeticoes por configuracao: $reps"

Write-Host ">> Sequencial"
foreach ($n in $tam) {
    $saida = java DotprodSeq $n $reps 2>$null
    $linha = $saida | Where-Object { $_ -like "seq,*" }
    if ($linha) { Acrescenta $linha; Write-Host "   $linha" }
}

Write-Host ">> Paralelo: N x threads"
foreach ($n in $tam) {
    foreach ($t in $threads) {
        Write-Host "   N=$n T=$t"
        $saida = java DotprodPar $n $t $reps 2>$null
        $linha = $saida | Where-Object { $_ -like "par,*" }
        if ($linha) { Acrescenta $linha } else { Write-Host "   ERRO em N=$n T=$t" }
    }
}
Write-Host "Pronto: $out"
