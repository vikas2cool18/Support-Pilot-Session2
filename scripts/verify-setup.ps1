# Session 0 / Session 1 environment check for Windows PowerShell.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\verify-setup.ps1
$model = if ($env:OLLAMA_CHAT_MODEL) { $env:OLLAMA_CHAT_MODEL } else { "llama3.2" }
$fail = 0; $warn = 0
function Ok($m)   { Write-Host "  [OK]   $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  [WARN] $m" -ForegroundColor Yellow; $script:warn++ }
function Bad($m)  { Write-Host "  [FAIL] $m" -ForegroundColor Red; $script:fail++ }

Write-Host "SupportPilot - Session 1 setup check"

try {
  $javaOut = (& java -version 2>&1 | Select-Object -First 1).ToString()
  if ($javaOut -match 'version "(\d+)') { if ([int]$Matches[1] -ge 21) { Ok "Java $($Matches[1])" } else { Bad "Java 21+ required (found $($Matches[1]))" } }
} catch { Bad "Java not found - install JDK 21 or 25" }

if (Get-Command mvn -ErrorAction SilentlyContinue) { Ok "Maven found" } else { Bad "Maven not found - install Maven 3.9+" }
if (Get-Command git -ErrorAction SilentlyContinue) { Ok "Git found" } else { Bad "Git not found" }

if (Get-Command docker -ErrorAction SilentlyContinue) {
  docker info *> $null; if ($LASTEXITCODE -eq 0) { Ok "Docker is running" } else { Warn "Docker not running - required from Session 2" }
} else { Warn "Docker not installed - required from Session 2" }

try {
  $tags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 3
  Ok "Ollama is running on :11434"
  if ($tags.models.name -match "^$model") { Ok "Model '$model' is pulled" } else { Warn "Model '$model' not pulled - run: ollama pull $model" }
} catch { Warn "Ollama not reachable on :11434 - install it and run: ollama serve" }

if ($env:OPENAI_API_KEY -or $env:ANTHROPIC_API_KEY) { Ok "A cloud API key is set" }
else { Warn "No OPENAI_API_KEY / ANTHROPIC_API_KEY - you can still use the 'local' profile" }

Write-Host ""
Write-Host "Result: $fail failures, $warn warnings"
if ($fail -eq 0) { Write-Host "You are ready for Session 1." } else { Write-Host "Fix the failures above before Session 1." }
exit $fail
