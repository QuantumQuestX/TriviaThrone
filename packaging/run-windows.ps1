$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

& (Join-Path $PSScriptRoot "TriviaThrone.exe") @args
exit $LASTEXITCODE
