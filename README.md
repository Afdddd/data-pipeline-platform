# 학습 내용

---
## Spring

### 비동기 처리 (@Async)
- `@EnableAsync`로 비동기 활성화
- ThreadPoolTaskExecutor 설정
- 비동기 메서드는 별도 트랜잭션
- 파일 업로드 후 백그라운드 파싱에 활용

### AOP
- 횡단 관심사 분리
- `@Around` advice로 메서드 실행 제어
- 커스텀 `@Retryable` 재시도 로직 구현

### Spring Shell
- CLI 명령어 인터페이스
- `@ShellComponent`, `@ShellMethod`

---

### 멀티파트 업로드
- `MultipartFile` 처리
- InputStream으로 메모리 효율적 처리

### 청크 업로드
- 대용량 파일을 조각으로 나누어 업로드

### 멀티 포맷 파싱
- JSON (Jackson), CSV 
- XML , Binary (DataInputStream)
- Strategy 패턴으로 포맷별 처리

---

## 디자인 패턴

### Factory Pattern
- 객체 생성 로직 분리
- ParserFactory, GeneratorFactory

### 상태 패턴
- 파일 업로드 상태: PENDING → PROCESSING → COMPLETED/FAILED
- 청크 업로드 상태 관리

---

## Kotlin

### Null Safety
- `?` nullable 타입
- `?.` Safe Call, `?:` Elvis
- 컴파일 타임 NPE 방지

### 고차 함수 & 람다
- `any`, `find`, `filter`, `map`
- `it` 암시적 파라미터

### Data Class

### enum class
- `entries`로 모든 값 접근
- companion object로 static 메서드

### companion object
- `static` 키워드 대체
- 싱글톤 객체
- `@JvmStatic`으로 Java 호환
- **Redis**: 캐싱
- **Kafka/RabbitMQ**: 메시지 큐
- **Spring Batch**: 배치 처리
- **Monitoring**: Prometheus + Grafana
- **Circuit Breaker**: Resilience4j
- **Rate Limiting**: API 제한
