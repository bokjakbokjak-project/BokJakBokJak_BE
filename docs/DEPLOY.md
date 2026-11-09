# 배포

main에 push(또는 dev → main PR 머지)되면 `.github/workflows/deploy.yml`이 자동으로 배포한다.

빌드(bootJar) → Docker 이미지 → GHCR 업로드 → 서버에서 컨테이너 교체 → `/api/health` 확인 → 디스코드 알림

## 구조

```
인터넷 ─443→ Nginx(HTTPS) ─→ 127.0.0.1:8080 bokjak-be 컨테이너
                                   │ docker network "bokjak"
                                   ├─ bokjak-db    (postgres:16)
                                   └─ bokjak-redis (redis:7)
```

DB와 Redis도 컨테이너로 띄우고 같은 Docker 네트워크에 둔다. 앱은 컨테이너 이름(bokjak-db, bokjak-redis)으로 접속하므로 PostgreSQL 접속 허용 설정(pg_hba.conf)을 따로 만질 필요가 없다. DB·Redis 포트는 외부에 열지 않는다.

## 서버 첫 설정 (한 번만)

Ubuntu 22.04/24.04 기준. 메모리 1GB 서버라면 스왑부터 만든다.

```bash
# 스왑 2GB (1GB 서버일 때)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # 적용하려면 로그아웃 후 다시 접속

# 네트워크, PostgreSQL, Redis
docker network create bokjak
docker run -d --name bokjak-db --restart unless-stopped --network bokjak \
  -e POSTGRES_DB=bokjakbokjak -e POSTGRES_USER=bokjakbokjak -e POSTGRES_PASSWORD='<DB 비밀번호>' \
  -e TZ=Asia/Seoul -v bokjak-db-data:/var/lib/postgresql/data postgres:16
docker run -d --name bokjak-redis --restart unless-stopped --network bokjak \
  -v bokjak-redis-data:/data redis:7 redis-server --appendonly yes --requirepass '<Redis 비밀번호>'

# Nginx + HTTPS (도메인이 서버 IP를 가리킨 뒤에)
sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx
sudo cp nginx/bokjak.conf /etc/nginx/conf.d/bokjak.conf   # server_name을 실제 도메인으로 수정
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d <도메인>
```

방화벽(보안 그룹)은 22, 80, 443만 연다.

## GitHub Secrets

Settings → Secrets and variables → Actions

| 이름 | 값 |
| --- | --- |
| SERVER_HOST | 서버 공인 IP (고정 IP) |
| SERVER_USER | ubuntu(EC2) 또는 azureuser(Azure) |
| SERVER_SSH_KEY | 서버 접속용 개인키 전체 (-----BEGIN부터 END까지) |
| ENV_FILE | 아래 서버용 .env 전체 |
| DISCORD_DEPLOY_WEBHOOK_URL | 선택 |

서버용 ENV_FILE 예시 (변수 이름은 `.env.example`과 같고 주소만 다르다):

```
TZ=Asia/Seoul
SERVER_PORT=8080
DB_URL=jdbc:postgresql://bokjak-db:5432/bokjakbokjak
DB_USERNAME=bokjakbokjak
DB_PASSWORD=<DB 비밀번호>
REDIS_URL=redis://:<Redis 비밀번호>@bokjak-redis:6379
JWT_SECRET_BASE64=<openssl rand -base64 32 결과>
JWT_ISSUER=bokjakbokjak
JWT_AUDIENCE=bokjakbokjak-api
JWT_ACCESS_TTL=30m
CORS_ALLOWED_ORIGINS=<프론트 배포 주소>,http://localhost:5173
```

새 환경변수가 생기면 ENV_FILE Secret을 수정한 뒤 다시 배포해야 반영된다. 비밀번호는 URL에 들어가므로 영문·숫자로만 만든다.

## 문제가 생기면

```bash
docker ps -a                      # 컨테이너 상태
docker logs --tail 100 bokjak-be  # 앱 로그
curl -i http://127.0.0.1:8080/api/health
```
