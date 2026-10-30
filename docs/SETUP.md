# Setup Guide: Jenkins, Docker Hub, Email, Webhook

This guide covers everything outside the code: the one-time setup that makes `git push` turn into a deployed, tested container. Follow it top to bottom once.

---

## 0. Prerequisites

| Tool | Check | Notes |
|---|---|---|
| Git | `git --version` | |
| JDK 21 | `java -version` | Temurin / OpenJDK 21 |
| Maven 3.9+ | `mvn -v` | Only needed for local runs; Jenkins can auto-install it |
| Docker + Compose v2.17+ | `docker version`, `docker compose version` | Docker Desktop (Windows/macOS) or Docker Engine + compose plugin (Linux) |
| Jenkins LTS | browser → `http://localhost:8080` | Install the suggested plugins on first start |
| Accounts | | GitHub, Docker Hub, a Gmail account for notifications |

**Where to run Jenkins**

- **Linux or WSL2 Ubuntu (recommended):** install Jenkins LTS from `pkg.jenkins.io`. Then let Jenkins use Docker:
  ```bash
  sudo usermod -aG docker jenkins
  sudo systemctl restart jenkins
  ```
- **Windows (native Jenkins MSI):** keep Docker Desktop running. The Jenkins service runs as *Local System* by default, and that account often can't reach Docker Desktop. Open `services.msc` → Jenkins → Properties → Log On, choose *This account*, enter your Windows user, then restart the service. The Jenkinsfile detects Windows and uses `bat` automatically.

Jenkins uses port **8080**, so the app is published on **8081** (through Nginx).

---

## 1. GitHub repository and Git workflow

1. Create a new **public** repository on GitHub, e.g. `calculator-devops` (no template files).
2. Clone it and add the project files **step by step**, committing as you go. Read each file before committing it, because you must be able to explain every part in the demo. A sensible order:

   | # | Files | Commit message |
   |---|---|---|
   | 1 | `pom.xml`, `CalculatorApplication.java`, `application*.properties`, `.gitignore` | `Initial Spring Boot project` |
   | 2 | `service/`, `exception/InvalidCalculationException.java`, `CalculatorServiceTest.java` | `Add calculator service with unit tests` |
   | 3 | `controller/`, `model/`, `GlobalExceptionHandler.java`, `CalculatorApiIntegrationTest.java` | `Add REST endpoints, validation and integration tests` |
   | 4 | `Dockerfile`, `.dockerignore` | `Add multi-stage Dockerfile` |
   | 5 | `docker-compose.yml`, `nginx/`, `.env.example`, **on a feature branch** (below) | `Add Compose stack with Nginx proxy and 2 replicas` |
   | 6 | `Jenkinsfile` | `Add declarative Jenkins pipeline` |
   | 7 | `README.md`, `docs/` | `Add documentation` |

3. **Feature branch + pull request** (required by the rubric):
   ```bash
   git checkout -b feature/compose-deploy
   git add docker-compose.yml nginx/ .env.example
   git commit -m "Add Compose stack with Nginx proxy and 2 replicas"
   git push -u origin feature/compose-deploy
   ```
   On GitHub, click **Compare & pull request**, then **Create pull request**, then **Merge**. Back locally:
   ```bash
   git checkout main
   git pull
   ```
4. Before every push, check that no secrets are committed: `git grep -n -i -E "password|token|secret"` should only match comments and variable names.

---

## 2. Docker Hub

1. Sign in at hub.docker.com and create a **public** repository named `calculator`. This is optional, because the first push creates it.
2. Go to **Account settings → Personal access tokens → Generate new token**, with access **Read & Write**. Copy the token. Jenkins uses this instead of your password.

---

## 3. Jenkins configuration

### 3.1 Plugins
*Manage Jenkins → Plugins → Available*. Make sure these are installed (most come with "suggested plugins"):

- Pipeline, Pipeline: Stage View
- Git, GitHub
- JUnit
- Email Extension
- Credentials Binding

### 3.2 Tools
*Manage Jenkins → Tools*. The names must match the `tools {}` block in the Jenkinsfile exactly.

- **JDK installations → Add JDK**: Name `JDK-21`, untick *Install automatically*, and set `JAVA_HOME` to your JDK 21 folder.
  - Linux: `/usr/lib/jvm/java-21-openjdk-amd64`
  - Windows: `C:\Program Files\Java\jdk-21` (Oracle JDK) or `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot`
- **Maven installations → Add Maven**: Name `Maven-3.9`, tick *Install automatically*, and pick the latest 3.9.x.

### 3.3 Docker Hub credential
*Manage Jenkins → Credentials → System → Global credentials → Add Credentials*

- Kind: **Username with password**
- Username: your Docker Hub username
- Password: the **access token** from step 2
- ID: `dockerhub-credentials` (exactly this, because the Jenkinsfile refers to it)

### 3.4 Email notifications (Gmail)

1. In your Google account, go to **Security** and turn on **2-Step Verification**. Then open **App passwords** and create one called "Jenkins". Copy the 16-character password.
2. *Manage Jenkins → System*:
   - **Jenkins Location → System Admin e-mail address**: your Gmail address. This becomes the "From" address.
   - **Extended E-mail Notification**:
     - SMTP server `smtp.gmail.com`, SMTP port `465`
     - Advanced → Credentials → Add → *Username with password*: your Gmail address + the app password
     - Tick **Use SSL**
     - Default Content Type: `HTML (text/html)`
   - Optionally fill in the plain **E-mail Notification** section the same way. Then click *Test configuration by sending test e-mail* to confirm SMTP works.
3. Save.

### 3.5 Notification address

`DOCKERHUB_USER` is set in the Jenkinsfile. The email recipient is kept out of the public repo: add it under *Manage Jenkins → System → Global properties → Environment variables* (name `NOTIFY_EMAIL`, value your address).

### 3.6 Create the pipeline job

1. **New Item** → name `calculator-pipeline` → **Pipeline** → OK.
2. **Triggers**: tick **GitHub hook trigger for GITScm polling**.
3. **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**, Repository URL: `https://github.com/ShivekRanjan/calculator-devops.git` (public repos need no credentials)
   - Branch Specifier: `*/main`
   - Script Path: `Jenkinsfile`
4. Save, then click **Build Now** once. The first build also registers the triggers declared in the Jenkinsfile.

---

## 4. GitHub webhook (automatic trigger)

GitHub can't reach `localhost`, so expose Jenkins with a tunnel such as **ngrok**:

```bash
ngrok config add-authtoken <your-ngrok-token>
ngrok http 8080
# -> Forwarding  https://abcd-1234.ngrok-free.app -> http://localhost:8080
```

On GitHub, go to the repo's **Settings → Webhooks → Add webhook**:

- Payload URL: `https://abcd-1234.ngrok-free.app/github-webhook/` (the trailing `/` matters)
- Content type: `application/json`
- Events: **Just the push event**

After saving, *Recent Deliveries* should show a green tick. A free ngrok URL changes every time ngrok restarts, so update the webhook before the demo.

If you skip the webhook, the `pollSCM('H/5 * * * *')` trigger in the Jenkinsfile still picks up pushes within about 5 minutes. The assignment accepts polling, but the webhook makes a better demo.

---

## 5. Verify the first green run

```bash
docker compose -p calculator ps         # 2x calculator + 1x proxy, all "healthy"
docker ps
curl "http://localhost:8081/api/v1/add?a=2&b=3"
curl "http://localhost:8081/api/v1/divide?a=5&b=0"
curl "http://localhost:8081/actuator/health"
curl "http://localhost:8081/actuator/info"        # shows the Jenkins build number
curl -i "http://localhost:8081/api/v1/multiply?a=6&b=7"   # X-Served-By header alternates between replicas
```

(On Windows PowerShell, use `curl.exe` instead of `curl`.)

Docker Hub should now show the tags `latest` and `<build number>`.

---

## 6. Screenshot checklist (Deliverable 4)

- [ ] Jenkins **Stage View** with all six stages green
- [ ] **Test Result** page (JUnit), showing all tests passing
- [ ] The **success email** in your inbox (and optionally a failure email)
- [ ] `docker ps` showing 3 running, healthy containers
- [ ] API responses: add / subtract / multiply / divide, divide-by-zero (400), `/actuator/health`
- [ ] Docker Hub repository page with the build-number tags

---

## 7. Live demo script (Class 17)

1. **Show the running stack**: `docker compose -p calculator ps`, then `curl .../actuator/info` and note build number *N*.
2. **Make a visible change**. For example, add a modulo endpoint:

   `CalculatorService.java`
   ```java
   public BigDecimal modulo(double a, double b) {
       BigDecimal dividend = toDecimal("a", a);
       BigDecimal divisor = toDecimal("b", b);
       if (divisor.signum() == 0) {
           throw new InvalidCalculationException("Modulo by zero is not allowed");
       }
       return dividend.remainder(divisor).stripTrailingZeros();
   }
   ```
   `CalculatorController.java`
   ```java
   @GetMapping("/modulo")
   public CalculationResponse modulo(@RequestParam("a") Double a, @RequestParam("b") Double b) {
       return CalculationResponse.of("modulo", a, b, calculatorService.modulo(a, b));
   }
   ```
   `CalculatorServiceTest.java`
   ```java
   @Test
   void modulo() {
       assertDecimalEquals("1", service.modulo(10, 3));
       assertThrows(InvalidCalculationException.class, () -> service.modulo(10, 0));
   }
   ```
3. Commit and push: `git commit -am "Add modulo endpoint" && git push`.
4. Switch to Jenkins. The build starts by itself within seconds (webhook), and you can walk through the stages as they run.
5. When it's green: `curl ".../actuator/info"` shows build *N+1*, `curl "http://localhost:8081/api/v1/modulo?a=10&b=3"` returns `1`, and the email arrives.
6. Optional extras:
   - Click **Build Now** again to show idempotency: the stack stays up and nothing breaks.
   - Push a deliberately failing test to show a red build, the test report, and the failure email. Then revert it.

---

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| `permission denied ... /var/run/docker.sock` | Linux: `sudo usermod -aG docker jenkins` and restart Jenkins |
| `docker: not found` / `'docker' is not recognized` | Docker isn't on the Jenkins service's PATH. On Windows, run the service as your user (section 0) |
| `Tool type "jdk" does not have an install of "JDK-21"` | Tool names in *Manage Jenkins → Tools* must match the Jenkinsfile exactly |
| `No such DSL method 'githubPush'` | Install the **GitHub** plugin, or delete the `githubPush()` line (polling still works) |
| `No such DSL method 'emailext'` | Install the **Email Extension** plugin |
| `denied: requested access to the resource is denied` on push | `DOCKERHUB_USER` must equal the credential's username, and the token needs *Read & Write* |
| `port is already allocated` (8081) | Something else uses 8081. Add `PROXY_PORT = '8090'` to the Jenkinsfile `environment {}` block |
| `unknown flag: --wait` / `--wait-timeout` | Update Docker Desktop / the compose plugin to v2.17+ |
| Gmail `535 Username and Password not accepted` | Use the **App Password**, not your normal password, with port 465 + SSL |
| Webhook delivery shows 404 / timeout | URL must end in `/github-webhook/`, and ngrok must be running |
| Disk filling up with old images | `docker image prune -a --filter "until=168h"` |
