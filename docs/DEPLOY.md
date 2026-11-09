# 배포

main에 push(또는 dev → main PR 머지)되면 `.github/workflows/deploy.yml`이 자동으로 배포한다.

빌드(bootJar) → Docker 이미지 → GHCR 업로드 → VM에서 컨테이너 교체 → `/api/health` 확인 → 디스코드 알림

## 인프라 구성

| 구성요소 | 설명 |
| --- | --- |
| PostgreSQL | Azure Database for PostgreSQL 유연한 서버 (공용 접근, 방화벽으로 VM IP만 허용) |
| Redis | VM Docker 컨테이너 `bokjak-redis` (redis:7, 외부 포트 미개방) |
| Backend | VM Docker 컨테이너 `bokjak-be` (127.0.0.1:8080) |
| Nginx | 80/443 → 127.0.0.1:8080 리버스 프록시 + Let's Encrypt |
| 도메인 | Azure 공용 IP의 DNS 이름 레이블 (`<이름>.koreacentral.cloudapp.azure.com`) |
| CI/CD | GitHub Actions → GHCR 이미지 푸시 → VM SSH 배포 (main push) |
| API 문서 | Swagger UI `https://<도메인>/swagger-ui/index.html` |

앱과 Redis 컨테이너는 Docker 네트워크 `bokjak`으로 연결되고, 앱은 컨테이너 이름(`bokjak-redis`)으로 Redis에 접속한다.

## Azure 리소스 (한 번만)

모두 같은 리소스 그룹(예: `bokjak-rg`), 지역 **Korea Central**.

### VM

- 이미지: Ubuntu Server 24.04 LTS (x64)
- 크기: B2ats v2 또는 B1s (학생 무료 750시간 대상). **B2pts v2(ARM)는 고르지 않는다** — 배포 이미지가 amd64다.
- 인증: SSH 공개 키, 사용자 이름 `azureuser`, 새 키 쌍 생성 → `.pem` 다운로드
- 인바운드 포트: SSH(22), HTTP(80), HTTPS(443)
- 만든 뒤: 공용 IP 리소스 → 구성 → DNS 이름 레이블 입력 → 도메인 생성

### PostgreSQL 유연한 서버

- 워크로드 유형: 개발 / 컴퓨팅: 버스트 가능 **B1ms** / 스토리지 32GB, 자동 증가 끔
- 고가용성 끔, 백업 7일·로컬 중복
- 버전 16, 인증: PostgreSQL 인증만 (관리자 이름·비밀번호 설정)
- 네트워킹: 공용 액세스, 방화벽 규칙에 **VM 공용 IP** 추가 (로컬에서 접속할 땐 내 IP를 잠깐 추가했다가 삭제)
- 만든 뒤: 데이터베이스 메뉴 → `bokjakbokjak` 데이터베이스 추가

비용 관리 → 예산에서 소액 알림(예: $10)을 걸어 둔다.

## VM 첫 설정 (한 번만)

```bash
ssh -i <키파일>.pem azureuser@<VM 공용 IP>

# 스왑 2GB (VM 메모리 1GB)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # 적용하려면 exit 후 다시 접속

# Redis (외부 포트를 열지 않으므로 같은 네트워크의 앱만 접근 가능)
docker network create bokjak
docker run -d --name bokjak-redis --restart unless-stopped --network bokjak \
  -v bokjak-redis-data:/data redis:7 redis-server --appendonly yes

# Nginx + HTTPS
sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx
sudo nano /etc/nginx/conf.d/bokjak.conf   # nginx/bokjak.conf 내용 붙여넣고 server_name을 도메인으로 수정
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d <도메인>
```

## GitHub Secrets

Settings → Secrets and variables → Actions

| 이름 | 값 |
| --- | --- |
| VM_HOST | VM 공용 IP |
| VM_USER | azureuser |
| VM_SSH_KEY | `.pem` 파일 전체 (-----BEGIN부터 END까지) |
| ENV_FILE | 아래 서버용 .env 전체 |
| DISCORD_DEPLOY_WEBHOOK_URL | 선택 |

서버용 ENV_FILE 예시 (변수 이름은 `.env.example`과 같고 값만 다르다):

```
TZ=Asia/Seoul
SERVER_PORT=8080
DB_URL=jdbc:postgresql://<서버이름>.postgres.database.azure.com:5432/bokjakbokjak?sslmode=require
DB_USERNAME=<관리자 이름>
DB_PASSWORD=<관리자 비밀번호>
REDIS_URL=redis://bokjak-redis:6379
JWT_SECRET_BASE64=<openssl rand -base64 32 결과>
JWT_ISSUER=bokjakbokjak
JWT_AUDIENCE=bokjakbokjak-api
JWT_ACCESS_TTL=30m
CORS_ALLOWED_ORIGINS=<프론트 배포 주소>,http://localhost:5173
```

새 환경변수가 생기면 ENV_FILE Secret을 수정한 뒤 다시 배포해야 반영된다.

## 문제가 생기면

```bash
docker ps -a                      # 컨테이너 상태
docker logs --tail 100 bokjak-be  # 앱 로그
curl -i http://127.0.0.1:8080/api/health
```

- DB 연결 실패: 유연한 서버 방화벽에 VM 공용 IP가 있는지, DB_URL 끝에 `?sslmode=require`가 있는지 확인
- 배포 Action의 SSH 단계 실패: VM_SSH_KEY에 키 전체가 들어갔는지, VM 인바운드 22번이 열려 있는지 확인
