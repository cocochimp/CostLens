#!/usr/bin/env bash
# 统一构建入口：固定 JDK 21，避免 Maven 默认 JDK 25 与 Lombok 注解处理器不兼容
set -euo pipefail

export JAVA_HOME="/Users/lovin/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home"
cd "$(dirname "$0")"

case "${1:-verify}" in
  verify)  shift || true; mvn -B clean verify "$@" ;;
  test)    shift || true; mvn -B test "$@" ;;
  compile) shift || true; mvn -B compile "$@" ;;
  package) shift || true; mvn -B package -DskipTests "$@" ;;
  run)     shift || true; mvn -B -pl :costlens-start -am spring-boot:run "$@" ;;
  *)       mvn -B "$@" ;;
esac