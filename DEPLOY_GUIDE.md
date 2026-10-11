# CampusConnect v2 – What's new and how to go live

## 1. What was added (old files untouched)

| Area | New file(s) |
|------|-------------|
| Signed login tokens, signup, change password | `v2/controller/AuthV2Controller.java`, `v2/security/TokenService.java` |
| Event registration with seat limit, duplicate block, cancel | `v2/controller/RegistrationController.java`, `v2/model/Registration.java` |
| Club join / leave with member counts | same controller |
| Attendance marking + CSV export (teacher) | same controller |
| Announcements (post, pin, delete) | `v2/controller/AnnouncementController.java` |
| Teacher stats overview | `v2/controller/StatsController.java` |
| Health check `/api/v2/health`, `/` redirect | `StatsController`, `HomeController` |
| New pages | `frontend/portal.html`, `student-hub.html`, `teacher-hub.html` |
| Scripts / styles | `js/config.js`, `js/extra.js`, `css/extra.css` |
| Live-deployment files | `Dockerfile`, `docker-compose.yml`, `.env.example`, `application-prod.properties`, `database/campusconnect_v2.sql` |

New tables (`registrations`, `announcements`) are created automatically by Hibernate (`ddl-auto=update`). The SQL file is optional.

Open `frontend/portal.html` to use the new features. Old pages still work, and the hubs link to them ("Classic view" / "Manage data" for adding clubs, events and students).

## 2. Run locally first

1. Keep your MySQL running and the `campusconnect` database present.
2. `cd campusconnect-backend` then `mvnw.cmd spring-boot:run`
3. Open `frontend/portal.html` (or http://localhost:8080 once the jar serves it in Docker mode).
4. Test: teacher `T101 / teacher123` adds an event with max participants 1 in "Manage data"; student `S101 / student123` registers; a second student gets "Sorry, this event is full".

## 3. Before you push to GitHub

- `campusconnect-backend/src/main/resources/application.properties` contains your real MySQL password. Replace it with `YOUR_MYSQL_PASSWORD` and change the password on your machine. Never push real passwords.
- A root `.gitignore` was added (ignores `target/`, `.env`).
- Create a repo, then from the `cc` folder: `git init`, `git add .`, `git commit -m "CampusConnect v2"`, `git remote add origin <your-repo-url>`, `git push -u origin main`.

## 4. Go live – recommended: ONE service (backend + frontend together)

The Docker image serves the website and the API from the same address, so there are no CORS problems and `config.js` picks the right API address by itself.

### Option A – Render + free cloud MySQL (no credit card needed usually)

1. **Database:** create a free MySQL at Aiven (or TiDB Cloud / any MySQL host). Create a database called `campusconnect`. Note host, port, user, password.
2. Build the JDBC URL: `jdbc:mysql://HOST:PORT/campusconnect?sslMode=REQUIRED&serverTimezone=Asia/Kolkata`
3. On render.com: New → Web Service → connect your GitHub repo → Runtime: **Docker** (root directory = the `cc` folder).
4. Add environment variables:
   - `DB_URL` = the URL from step 2
   - `DB_USERNAME`, `DB_PASSWORD`
   - `CC_TOKEN_SECRET` = any long random string (30+ characters)
5. Deploy. First build takes a few minutes. Open `https://<your-service>.onrender.com`; it redirects to the sign-in page.
6. Free Render services sleep after inactivity, so the first load can take about 50 seconds. Keep that in mind before a viva demo; open it a minute early.

### Option B – Railway (backend + MySQL in one project)

New project → add **MySQL** → add service from your GitHub repo (Dockerfile is detected). Set `DB_URL` using Railway's MySQL host/port values (`jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true`), plus `DB_USERNAME`, `DB_PASSWORD` and `CC_TOKEN_SECRET`. Generate a public domain in Settings → Networking. Railway's free credit is limited, so check current pricing.

### Option C – Your own VPS / lab server

1. Install Docker. Copy the `cc` folder to the server.
2. `cp .env.example .env` and edit `MYSQL_ROOT_PASSWORD` and `CC_TOKEN_SECRET`.
3. `docker compose up -d --build`. The site is on port 80.
4. For a domain and HTTPS, put Caddy or Nginx in front, or use Cloudflare.

Pricing and free tiers change often, so confirm current limits on each provider's site.

## 5. Optional – frontend on Netlify/GitHub Pages, backend elsewhere

Upload `frontend`, `css`, `js` keeping the same folder layout. In every page, add before the `config.js` tag:
`<script>window.CC_API_BASE = "https://your-backend-url";</script>`
This also makes the old pages (which hard-code `localhost:8080`) work, but only on pages that load `config.js`; the three new pages already do.

## 6. After going live (important)

1. Log in as teacher `T101 / teacher123` → Account → change the password immediately. Do the same for demo student `S101`.
2. Add your real clubs and events through "Manage data".
3. Check `https://<your-url>/api/v2/health` returns `{"status":"UP"}`.

## 7. Known limits (be ready for viva questions)

- The old endpoints (`/api/students`, `/api/events`, ...) have no authentication and passwords are stored as plain text. I did not alter them, as requested. All new v2 endpoints verify a signed token and role. Next hardening step: add Spring Security and BCrypt password hashing, then require the token on the old endpoints.
- Tokens last 12 hours. Set `CC_TOKEN_SECRET`, or users are logged out each time the server restarts.
- This build could not be compiled in my sandbox (Maven Central was blocked). Run `mvnw.cmd spring-boot:run` once locally; if the compiler reports an error, send it to me and I'll fix it.
