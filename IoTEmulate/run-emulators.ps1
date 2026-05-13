param(
  [int]$instances = 4
)

# Usage: .\run-emulators.ps1 -instances 4
Set-Location -Path $PSScriptRoot

$configs = Get-ChildItem -Path "$PSScriptRoot\configs" -Filter "dev*.json" | Select-Object -First $instances

if ($configs.Count -eq 0) {
  Write-Error "No config files found in configs/"
  exit 1
}

foreach ($cfg in $configs) {
  $name = [System.IO.Path]::GetFileNameWithoutExtension($cfg.Name)
  $log = "$PSScriptRoot\logs\$name.log"
  if (-not (Test-Path "$PSScriptRoot\logs")) { New-Item -ItemType Directory -Path "$PSScriptRoot\logs" | Out-Null }

  Write-Host "Starting $name with config $($cfg.FullName) -> log $log"
  Start-Process -FilePath "node" -ArgumentList "emulator.js --config `"$($cfg.FullName)`"" -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError $log -WindowStyle Hidden
}

Write-Host "Started $($configs.Count) emulators. Check logs/ for output."
