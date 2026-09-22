# 원본 저장소 통합 목록

| 통합 경로 | 원본 | 공개 상태 |
|---|---|---|
| `services/auth` | [baef-p2-auth](https://github.com/greencomacademy/baef-p2-auth) | 공개 통합 |
| `services/store` | [baef-p2-store](https://github.com/greencomacademy/baef-p2-store) | 공개 통합 |
| `services/platform` | [baef-p2-platform](https://github.com/greencomacademy/baef-p2-platform) | 공개 통합 |
| `services/order` | [baef-p2-order](https://github.com/greencomacademy/baef-p2-order) | 공개 통합 |
| `services/report` | [beaf-p2-report](https://github.com/greencomacademy/beaf-p2-report) | 공개 통합 |
| `services/notification` | [baef-p2-notification](https://github.com/greencomacademy/baef-p2-notification) | 공개 통합 |
| `services/billing` | [baef-p2-billing](https://github.com/greencomacademy/baef-p2-billing) | 공개 통합 |
| `gateway/scg` | [baef-p2-scg](https://github.com/greencomacademy/baef-p2-scg) | 공개 통합 |
| `client` | [baef-p2-client](https://github.com/greencomacademy/baef-p2-client) | 공개 통합 |
| `client/archive/front-p2-fix` | [baef-front-p2-fix](https://github.com/greencomacademy/baef-front-p2-fix) | 공개 통합 |
| `external-simulator/client` | [baef-external-simulator](https://github.com/greencomacademy/baef-external-simulator) | 공개 통합 |
| `external-simulator/server` | [baef-p2-external-platform-simulator](https://github.com/greencomacademy/baef-p2-external-platform-simulator) | 공개 통합 |
| 별도 비공개 저장소 | [baef-p2-k8s](https://github.com/greencomacademy/baef-p2-k8s) | `DeliveryInsider-MSA-k8s`로 분리 |

모든 공개 통합 경로는 `git subtree add`를 `--squash` 없이 실행해 원본 커밋 이력을 보존했습니다. `baef-p2-k8s`는 배포 환경 정보를 분리하기 위해 개인 계정의 별도 비공개 저장소에 원본 전체 이력을 보존합니다.

조직 목록에서 독립된 2차 `infra` 저장소는 확인되지 않았습니다. 관련 구성은 `baef-p2-k8s`에 포함되어 있습니다.
