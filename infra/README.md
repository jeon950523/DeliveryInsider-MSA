# Infra 안내

`greencomacademy` 조직에서 독립적인 2차 `infra` 저장소는 확인되지 않았습니다. 현재 배포 자동화와 인프라 관련 구현은 [`../k8s`](../k8s)에 함께 보존되어 있습니다.

- Kubernetes 서비스·Deployment·ConfigMap·Secret 예시
- Jenkins 빌드/테스트/이미지 태그 갱신 파이프라인
- Argo CD ApplicationSet
- 서비스별 CI 스키마 검증 가이드

이 디렉터리는 통합 저장소의 구조를 명확히 하기 위한 안내 문서입니다. 실제 인프라 구성은 `k8s/`와 각 서비스의 Dockerfile·CI 설정을 함께 확인해야 합니다.
