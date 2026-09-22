# Infra 안내

`greencomacademy` 조직에서 독립적인 2차 `infra` 저장소는 확인되지 않았습니다. 배포 자동화와 인프라 관련 구현은 원본 [`baef-p2-k8s`](https://github.com/greencomacademy/baef-p2-k8s)에 함께 보존되어 있으며, 개인 계정에서는 비공개 `DeliveryInsider-MSA-k8s` 저장소로 분리합니다.

- Kubernetes 서비스·Deployment·ConfigMap·Secret 예시
- Jenkins 빌드/테스트/이미지 태그 갱신 파이프라인
- Argo CD ApplicationSet
- 서비스별 CI 스키마 검증 가이드

이 디렉터리는 공개 통합 저장소의 구조를 명확히 하기 위한 안내 문서입니다. 실제 인프라 구성은 권한이 있는 경우 비공개 배포 저장소와 각 서비스의 Dockerfile·CI 설정을 함께 확인해야 합니다.
