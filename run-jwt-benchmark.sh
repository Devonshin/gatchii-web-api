#!/bin/bash

###############################################################################
# JWT 성능 벤치마크 실행 스크립트
#
# 사용법:
#   ./run-jwt-benchmark.sh [옵션]
#
# 옵션:
#   all          : 모든 벤치마크 실행 (기본값)
#   generation   : JWT 생성 벤치마크만 실행
#   verification : JWT 검증 벤치마크만 실행
#   parsing      : JWT 파싱 벤치마크만 실행
#   quick        : 빠른 테스트 (워밍업/측정 반복 횟수 최소화)
#
# 예제:
#   ./run-jwt-benchmark.sh
#   ./run-jwt-benchmark.sh generation
#   ./run-jwt-benchmark.sh quick
#
# @author Devonshin
# @date 2025-01-19
###############################################################################

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 기본 설정
MODE="${1:-all}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULT_DIR="build/reports/jmh"
RESULT_FILE="${RESULT_DIR}/results_${TIMESTAMP}.json"

# 결과 디렉토리 생성
mkdir -p "${RESULT_DIR}"

log_info "JWT 벤치마크 시작 - 모드: ${MODE}"
log_info "결과 파일: ${RESULT_FILE}"

# 벤치마크 실행 함수
run_benchmark() {
    local includes="$1"
    local description="$2"
    
    log_info "${description} 벤치마크 실행 중..."
    
    if [ "${MODE}" == "quick" ]; then
        # 빠른 테스트: 워밍업 1회, 측정 2회
        ./gradlew jmh \
            -Pjmh.includes="${includes}" \
            -Pjmh.warmupIterations=1 \
            -Pjmh.measurementIterations=2 \
            -Pjmh.resultFormat=JSON \
            -Pjmh.resultFile="${RESULT_FILE}"
    else
        # 정상 테스트: 워밍업 2회, 측정 3회
        ./gradlew jmh \
            -Pjmh.includes="${includes}" \
            -Pjmh.resultFormat=JSON \
            -Pjmh.resultFile="${RESULT_FILE}"
    fi
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_success "${description} 벤치마크 완료"
        return 0
    else
        log_error "${description} 벤치마크 실패 (exit code: ${exit_code})"
        return 1
    fi
}

# 모드에 따른 벤치마크 실행
case "${MODE}" in
    generation)
        run_benchmark ".*JwtGenerationBenchmark.*" "JWT 생성"
        ;;
    verification)
        run_benchmark ".*JwtVerificationBenchmark.*" "JWT 검증"
        ;;
    parsing)
        run_benchmark ".*JwtParsingBenchmark.*" "JWT 파싱"
        ;;
    quick)
        log_warn "빠른 테스트 모드 - 정확도가 낮을 수 있습니다"
        run_benchmark ".*" "전체 JWT"
        ;;
    all|*)
        run_benchmark ".*JwtGenerationBenchmark.*" "JWT 생성"
        run_benchmark ".*JwtVerificationBenchmark.*" "JWT 검증"
        run_benchmark ".*JwtParsingBenchmark.*" "JWT 파싱"
        ;;
esac

# 결과 요약
if [ -f "${RESULT_FILE}" ]; then
    log_success "벤치마크 완료!"
    log_info "결과 파일: ${RESULT_FILE}"
    log_info "상세 결과 확인: cat ${RESULT_FILE} | jq ."
else
    log_error "결과 파일이 생성되지 않았습니다"
    exit 1
fi

# 시스템 정보 기록
log_info "시스템 정보 기록 중..."
cat > "${RESULT_DIR}/system_info_${TIMESTAMP}.txt" <<EOF
# JWT 벤치마크 시스템 정보
실행 시간: $(date)
모드: ${MODE}

## 시스템 정보
OS: $(uname -s)
커널: $(uname -r)
아키텍처: $(uname -m)

## Java 정보
$(java -version 2>&1)

## 빌드 정보
Gradle 버전: $(./gradlew --version | grep Gradle)
Kotlin 버전: $(./gradlew --version | grep Kotlin)
EOF

log_success "시스템 정보 저장 완료: ${RESULT_DIR}/system_info_${TIMESTAMP}.txt"

log_info "========================================="
log_success "JWT 벤치마크 실행 완료"
log_info "========================================="
