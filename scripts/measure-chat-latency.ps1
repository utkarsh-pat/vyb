param(
    [string[]]$Devices = @("emulator-5554", "emulator-5556"),
    [ValidateRange(10, 900)]
    [int]$DurationSeconds = 60,
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"
$tag = "VybChatLatency"

if (-not (Test-Path -LiteralPath $Adb)) {
    throw "adb was not found at $Adb"
}

foreach ($device in $Devices) {
    & $Adb -s $device get-state | Out-Null
    & $Adb -s $device logcat -c
}

Write-Host "Chat latency capture started for $($Devices -join ', ')."
Write-Host "Send messages, open them on the other device, and switch one device offline/online."
Start-Sleep -Seconds $DurationSeconds

$rows = foreach ($device in $Devices) {
    $lines = & $Adb -s $device logcat -d -s "${tag}:D" "*:S"
    foreach ($line in $lines) {
        if ($line -match 'metric=(?<metric>[^ ]+)(?: durationMs=(?<duration>\d+))?') {
            [PSCustomObject]@{
                Device = $device
                Metric = $Matches.metric
                DurationMs = if ($Matches.duration) { [int]$Matches.duration } else { $null }
                Raw = $line
            }
        }
    }
}

function Get-Percentile([int[]]$Values, [double]$Percentile) {
    if (-not $Values -or $Values.Count -eq 0) { return $null }
    $ordered = @($Values | Sort-Object)
    $index = [Math]::Ceiling(($Percentile / 100) * $ordered.Count) - 1
    return $ordered[[Math]::Max(0, [Math]::Min($index, $ordered.Count - 1))]
}

$summary = $rows |
    Where-Object { $null -ne $_.DurationMs } |
    Group-Object Metric |
    ForEach-Object {
        $values = @($_.Group.DurationMs)
        [PSCustomObject]@{
            Metric = $_.Name
            Samples = $values.Count
            P50Ms = Get-Percentile $values 50
            P95Ms = Get-Percentile $values 95
            P99Ms = Get-Percentile $values 99
            MaxMs = ($values | Measure-Object -Maximum).Maximum
        }
    } |
    Sort-Object Metric

if (-not $summary) {
    Write-Warning "No VybChatLatency samples were captured. Keep a conversation open on both debug builds and retry."
    exit 2
}

$summary | Format-Table -AutoSize

$output = Join-Path (Get-Location) ".tmp\chat-latency-$((Get-Date).ToString('yyyyMMdd-HHmmss')).csv"
New-Item -ItemType Directory -Force -Path (Split-Path $output) | Out-Null
$rows | Export-Csv -NoTypeInformation -Path $output
Write-Host "Raw samples: $output"
