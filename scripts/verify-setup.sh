#!/usr/bin/env bash
# Session 0 / Session 1 environment check (macOS, Linux, WSL, Git Bash).
# Usage: bash scripts/verify-setup.sh
MODEL="${OLLAMA_CHAT_MODEL:-llama3.2}"
pass=0; warn=0; fail=0
ok()   { printf "  \033[32m[OK]\033[0m   %s\n" "$1"; pass=$((pass+1)); }
wn()   { printf "  \033[33m[WARN]\033[0m %s\n" "$1"; warn=$((warn+1)); }
bad()  { printf "  \033[31m[FAIL]\033[0m %s\n" "$1"; fail=$((fail+1)); }

echo "SupportPilot - Session 1 setup check"

# Java 21+
if command -v java >/dev/null 2>&1; then
  v=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
  if [ "${v:-0}" -ge 21 ] 2>/dev/null; then ok "Java $v"; else bad "Java 21+ required (found: $v)"; fi
else bad "Java not found - install JDK 21 or 25"; fi

# Maven
if command -v mvn >/dev/null 2>&1; then ok "Maven $(mvn -v 2>/dev/null | head -1 | awk '{print $3}')"
else bad "Maven not found - install Maven 3.9+"; fi

# Git
if command -v git >/dev/null 2>&1; then ok "Git $(git --version | awk '{print $3}')"; else bad "Git not found"; fi

# Docker (needed from Session 2 onwards)
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then ok "Docker is running"
else wn "Docker not running - not needed today, required from Session 2"; fi

# Ollama
if curl -s --max-time 3 http://localhost:11434/api/tags >/dev/null 2>&1; then
  ok "Ollama is running on :11434"
  if curl -s http://localhost:11434/api/tags | grep -q "\"$MODEL"; then ok "Model '$MODEL' is pulled"
  else wn "Model '$MODEL' not pulled yet - run: ollama pull $MODEL"; fi
else wn "Ollama not reachable on :11434 - install it and run: ollama serve"; fi

# Cloud keys (at least one, or use Ollama only)
if [ -n "$OPENAI_API_KEY" ] || [ -n "$ANTHROPIC_API_KEY" ]; then ok "A cloud API key is set"
else wn "No OPENAI_API_KEY / ANTHROPIC_API_KEY set - you can still do every lab with the 'local' profile"; fi

echo
echo "Result: $pass OK, $warn warnings, $fail failures"
[ "$fail" -eq 0 ] && echo "You are ready for Session 1." || echo "Fix the failures above before Session 1."
exit "$fail"
