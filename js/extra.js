(function () {
    "use strict";

    var TOKEN_KEY = "ccToken";
    var esc = function (t) { return escapeHtml(t); };

    function token() {
        try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; }
    }

    function goPortal() {
        try {
            localStorage.removeItem("role");
            localStorage.removeItem("user");
            localStorage.removeItem(TOKEN_KEY);
        } catch (e) {}
        window.location.href = "portal.html";
    }

    async function v2(path, method, body, raw) {
        var headers = { "Content-Type": "application/json" };
        if (token()) headers["Authorization"] = "Bearer " + token();
        var res;
        try {
            res = await fetch(window.CC_BASE + "/api/v2" + path, {
                method: method || "GET",
                headers: headers,
                body: body ? JSON.stringify(body) : undefined
            });
        } catch (e) {
            throw new Error("Cannot reach the server. Check your connection or API address.");
        }
        if (res.status === 401 && token()) {
            goPortal();
            throw new Error("Session expired");
        }
        if (!res.ok) {
            var msg = "Request failed (" + res.status + ")";
            try { var d = await res.json(); if (d.message) msg = d.message; } catch (e) {}
            throw new Error(msg);
        }
        return raw ? res : res.json();
    }

    function toast(text, type) {
        var box = document.getElementById("toastBox");
        if (!box) return;
        var el = document.createElement("div");
        el.className = "alert alert-" + (type || "success") + " shadow-sm mb-2";
        el.textContent = text;
        box.appendChild(el);
        setTimeout(function () { el.remove(); }, 3500);
    }

    function guard(fn) {
        return async function () {
            try { await fn.apply(null, arguments); } catch (e) { toast(e.message, "danger"); }
        };
    }

    function fmtDate(iso) {
        if (!iso) return "";
        var d = new Date(iso);
        return isNaN(d) ? iso : d.toLocaleString();
    }

    function setupTabs(onShow) {
        var tabs = document.getElementById("hubTabs");
        tabs.addEventListener("click", function (ev) {
            var btn = ev.target.closest("button[data-tab]");
            if (!btn) return;
            tabs.querySelectorAll(".nav-link").forEach(function (b) { b.classList.toggle("active", b === btn); });
            document.querySelectorAll("[data-pane]").forEach(function (p) {
                p.classList.toggle("d-none", p.dataset.pane !== btn.dataset.tab);
            });
            onShow(btn.dataset.tab);
        });
        document.getElementById("logoutBtn").addEventListener("click", goPortal);
    }

    function bindPassword() {
        var form = document.getElementById("pwForm");
        form.addEventListener("submit", guard(async function (ev) {
            ev.preventDefault();
            await v2("/auth/change-password", "POST", {
                oldPassword: form.oldPassword.value,
                newPassword: form.newPassword.value
            });
            form.reset();
            toast("Password updated");
        }));
    }

    function announcementHtml(a, canEdit) {
        var tools = canEdit
            ? '<div class="mt-2"><button class="btn btn-sm btn-outline-secondary me-1" data-pin="' + a.id + '" data-state="' + (a.pinned ? "0" : "1") + '">' +
              (a.pinned ? "Unpin" : "Pin") + '</button><button class="btn btn-sm btn-outline-danger" data-del="' + a.id + '">Delete</button></div>'
            : "";
        return '<div class="announce' + (a.pinned ? " pinned" : "") + '">' +
            '<div class="d-flex justify-content-between"><strong>' + (a.pinned ? '<i class="bi bi-pin-angle-fill text-warning"></i> ' : "") + esc(a.title) + "</strong>" +
            '<small class="text-muted">' + esc(fmtDate(a.createdAt)) + "</small></div>" +
            '<div class="text-muted" style="white-space:pre-wrap">' + esc(a.message) + "</div>" +
            '<small class="text-muted">Posted by ' + esc(a.postedBy) + "</small>" + tools + "</div>";
    }

    var STATUS_BADGE = { UPCOMING: "bg-success", TODAY: "bg-warning text-dark", PAST: "bg-secondary", UNKNOWN: "bg-light text-dark" };

    /* ---------------- portal ---------------- */
    function initPortal() {
        var mode = "student";
        var loginForm = document.getElementById("loginForm");
        var signupForm = document.getElementById("signupForm");
        var errorBox = document.getElementById("errorBox");

        function fail(msg) { errorBox.textContent = msg; errorBox.classList.remove("d-none"); }

        function finish(data) {
            try {
                localStorage.setItem(TOKEN_KEY, data.token);
                saveSession(data.role, data.user);
            } catch (e) {}
            window.location.href = data.role + "-hub.html";
        }

        document.getElementById("portalTabs").addEventListener("click", function (ev) {
            var btn = ev.target.closest("button[data-mode]");
            if (!btn) return;
            mode = btn.dataset.mode;
            document.querySelectorAll("#portalTabs .nav-link").forEach(function (b) { b.classList.toggle("active", b === btn); });
            loginForm.classList.toggle("d-none", mode === "signup");
            signupForm.classList.toggle("d-none", mode !== "signup");
            errorBox.classList.add("d-none");
        });

        loginForm.addEventListener("submit", async function (ev) {
            ev.preventDefault();
            errorBox.classList.add("d-none");
            try {
                finish(await v2("/auth/login", "POST", {
                    role: mode, username: loginForm.username.value.trim(), password: loginForm.password.value
                }));
            } catch (e) { fail(e.message); }
        });

        signupForm.addEventListener("submit", async function (ev) {
            ev.preventDefault();
            errorBox.classList.add("d-none");
            var body = {};
            new FormData(signupForm).forEach(function (v, k) { body[k] = v; });
            try { finish(await v2("/auth/signup", "POST", body)); } catch (e) { fail(e.message); }
        });
    }

    /* ---------------- student hub ---------------- */
    function initStudentHub() {
        if (!token() || getRole() !== "student") return goPortal();
        document.getElementById("welcomeName").textContent = getUser().name;

        var events = [], clubs = [];

        function eventCardHtml(row) {
            var e = row.event;
            var pct = e.maximumParticipants > 0 ? Math.min(100, Math.round(row.registered * 100 / e.maximumParticipants)) : 0;
            var seats = row.seatsLeft === null ? "Open registration" : row.seatsLeft + " of " + e.maximumParticipants + " seats left";
            var action;
            if (row.joinedByMe) action = '<button class="btn btn-outline-danger w-100" data-cancel-event="' + e.id + '">Cancel registration</button>';
            else if (row.status === "PAST") action = '<button class="btn btn-secondary w-100" disabled>Event over</button>';
            else if (row.full) action = '<button class="btn btn-secondary w-100" disabled>Full</button>';
            else action = '<button class="btn btn-primary w-100" data-reg-event="' + e.id + '">Register</button>';
            var form = safeLink(e.googleFormLink);
            var formLink = form ? '<a class="small d-block mt-2 text-center" href="' + esc(form) + '" target="_blank" rel="noopener">Extra details form</a>' : "";
            return '<div class="col-md-6 col-lg-4"><div class="card item-card h-100"><div class="card-body p-4 d-flex flex-column">' +
                '<div class="d-flex justify-content-between mb-2"><h5 class="mb-0">' + esc(e.eventName) + '</h5><span class="badge ' + STATUS_BADGE[row.status] + '">' + row.status + "</span></div>" +
                '<p class="text-muted">' + esc(e.description) + "</p>" +
                '<ul class="info-list mb-3"><li><i class="bi bi-calendar3"></i>' + esc(e.date) + " " + esc(e.time) + '</li><li><i class="bi bi-geo-alt"></i>' + esc(e.venue) + "</li></ul>" +
                '<div class="bar-track mb-1"><div class="bar-fill' + (row.full ? " full" : "") + '" style="width:' + pct + '%"></div></div>' +
                '<small class="text-muted mb-3">' + seats + "</small>" +
                '<div class="mt-auto">' + action + formLink + "</div></div></div></div>";
        }

        function renderEvents() {
            var q = document.getElementById("eventSearch").value.trim().toLowerCase();
            var f = document.getElementById("eventFilter").value;
            var rows = events.filter(function (r) {
                var e = r.event;
                var hay = [e.eventName, e.venue, e.description].join(" ").toLowerCase();
                return (!q || hay.indexOf(q) >= 0) && (!f || r.status === f);
            });
            document.getElementById("eventList").innerHTML = rows.length ? rows.map(eventCardHtml).join("") : emptyHtml("No events match.");
        }

        function renderClubs() {
            var q = document.getElementById("clubSearch").value.trim().toLowerCase();
            var rows = clubs.filter(function (r) {
                return !q || [r.club.clubName, r.club.department].join(" ").toLowerCase().indexOf(q) >= 0;
            });
            document.getElementById("clubList").innerHTML = rows.length ? rows.map(function (r) {
                var c = r.club;
                var btn = r.joinedByMe
                    ? '<button class="btn btn-outline-danger w-100" data-leave-club="' + c.id + '">Leave club</button>'
                    : '<button class="btn btn-primary w-100" data-join-club="' + c.id + '">Join club</button>';
                var form = safeLink(c.googleFormLink);
                var formLink = form ? '<a class="small d-block mt-2 text-center" href="' + esc(form) + '" target="_blank" rel="noopener">Club form</a>' : "";
                return '<div class="col-md-6 col-lg-4"><div class="card item-card h-100"><div class="card-body p-4 d-flex flex-column">' +
                    '<h5>' + esc(c.clubName) + '</h5><span class="badge badge-soft align-self-start mb-2">' + esc(c.department) + "</span>" +
                    '<p class="text-muted">' + esc(c.description) + "</p>" +
                    '<ul class="info-list mb-3"><li><i class="bi bi-person-badge"></i>' + esc(c.teacherCoordinator) + '</li><li><i class="bi bi-people"></i>' + r.members + " members</li></ul>" +
                    '<div class="mt-auto">' + btn + formLink + "</div></div></div></div>";
            }).join("") : emptyHtml("No clubs match.");
        }

        var myRegs = {};

        async function loadMine() {
            var rows = await v2("/registrations/mine");
            myRegs = {};
            rows.forEach(function (r) { myRegs[r.registration.kind + ":" + r.registration.targetId] = r.registration.id; });
            var box = document.getElementById("mineList");
            if (!rows.length) { box.innerHTML = emptyHtml("You have not registered for anything yet."); return rows; }
            box.innerHTML = '<div class="card border-0 shadow-sm"><div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th>Type</th><th>Name</th><th>When</th><th>Status</th><th></th></tr></thead><tbody>' +
                rows.map(function (r) {
                    var g = r.registration;
                    var st = g.kind === "EVENT" ? (g.attended ? '<span class="badge bg-success">Attended</span>' : '<span class="badge ' + STATUS_BADGE[r.status || "UNKNOWN"] + '">' + (r.status || "-") + "</span>") : '<span class="badge bg-info text-dark">Member</span>';
                    return "<tr><td>" + esc(g.kind) + "</td><td>" + esc(r.title) + "</td><td>" + esc([r.date, r.time].filter(Boolean).join(" ") || "-") + "</td><td>" + st +
                        '</td><td class="text-end"><button class="btn btn-sm btn-outline-danger" data-cancel="' + g.id + '">Cancel</button></td></tr>';
                }).join("") + "</tbody></table></div></div>";
            return rows;
        }

        var refresh = guard(async function () {
            var res = await Promise.all([v2("/events/overview"), v2("/clubs/overview"), loadMine(), fetch(window.CC_BASE + "/api/v2/announcements").then(function (r) { return r.json(); })]);
            events = res[0]; clubs = res[1];
            renderEvents(); renderClubs();
            var upcomingMine = res[2].filter(function (r) { return r.registration.kind === "EVENT" && (r.status === "UPCOMING" || r.status === "TODAY"); }).length;
            var tile = function (n, l) { return '<div class="col-6 col-md-3"><div class="card stat-tile"><div class="card-body"><div class="num">' + n + '</div><small class="text-muted">' + l + "</small></div></div></div>"; };
            document.getElementById("homeStats").innerHTML =
                tile(events.filter(function (r) { return r.status === "UPCOMING" || r.status === "TODAY"; }).length, "Upcoming events") +
                tile(clubs.length, "Clubs") + tile(upcomingMine, "My upcoming events") +
                tile(res[2].filter(function (r) { return r.registration.kind === "CLUB"; }).length, "Clubs joined");
            document.getElementById("announceList").innerHTML = res[3].length ? res[3].map(function (a) { return announcementHtml(a, false); }).join("") : emptyHtml("No announcements yet.");
        });

        setupTabs(function () {});
        bindPassword();
        document.getElementById("eventSearch").addEventListener("input", renderEvents);
        document.getElementById("eventFilter").addEventListener("change", renderEvents);
        document.getElementById("clubSearch").addEventListener("input", renderClubs);

        document.querySelector("main").addEventListener("click", guard(async function (ev) {
            var b = ev.target.closest("button");
            if (!b) return;
            if (b.dataset.regEvent) { await v2("/registrations", "POST", { kind: "EVENT", targetId: Number(b.dataset.regEvent) }); toast("Registered!"); await refresh(); }
            else if (b.dataset.joinClub) { await v2("/registrations", "POST", { kind: "CLUB", targetId: Number(b.dataset.joinClub) }); toast("Joined club!"); await refresh(); }
            else if (b.dataset.cancelEvent || b.dataset.leaveClub || b.dataset.cancel) {
                var id = b.dataset.cancel || myRegs[(b.dataset.cancelEvent ? "EVENT:" + b.dataset.cancelEvent : "CLUB:" + b.dataset.leaveClub)];
                if (!id || !confirm("Are you sure?")) return;
                await v2("/registrations/" + id, "DELETE"); toast("Cancelled"); await refresh();
            }
        }));
        refresh();
    }

    /* ---------------- teacher hub ---------------- */
    function initTeacherHub() {
        if (!token() || getRole() !== "teacher") return goPortal();
        document.getElementById("welcomeName").textContent = getUser().name;

        function bars(items, labelFn, valFn, maxFn) {
            if (!items.length) return emptyHtml("Nothing to show yet.");
            return items.map(function (it) {
                var v = valFn(it), m = maxFn(it);
                var pct = m > 0 ? Math.min(100, Math.round(v * 100 / m)) : 0;
                return '<div class="mb-3"><div class="d-flex justify-content-between small"><span>' + esc(labelFn(it)) + "</span><span>" + v + (m > 0 ? " / " + m : "") + '</span></div><div class="bar-track"><div class="bar-fill' + (m > 0 && v >= m ? " full" : "") + '" style="width:' + pct + '%"></div></div></div>';
            }).join("");
        }

        var loadStats = guard(async function () {
            var s = await v2("/stats");
            var tile = function (n, l, icon) { return '<div class="col-6 col-md-3"><div class="card stat-tile"><div class="card-body"><i class="bi bi-' + icon + ' text-muted"></i><div class="num">' + n + '</div><small class="text-muted">' + l + "</small></div></div></div>"; };
            document.getElementById("statTiles").innerHTML = tile(s.students, "Students", "mortarboard") + tile(s.clubs, "Clubs", "people") + tile(s.events, "Events", "calendar-event") + tile(s.registrations, "Registrations", "check2-square");
            document.getElementById("eventBars").innerHTML = bars(s.perEvent, function (e) { return e.name; }, function (e) { return e.registered; }, function (e) { return e.capacity || 0; });
            document.getElementById("clubBars").innerHTML = bars(s.perClub, function (c) { return c.name; }, function (c) { return c.members; }, function () { return 0; });
            var depts = Object.keys(s.studentsByDepartment).map(function (k) { return { n: k, v: s.studentsByDepartment[k] }; });
            document.getElementById("deptBars").innerHTML = bars(depts, function (d) { return d.n; }, function (d) { return d.v; }, function () { return s.students; });
            var sel = document.getElementById("regTarget");
            var prev = sel.value;
            sel.innerHTML = s.perEvent.map(function (e) { return '<option value="EVENT/' + e.id + '">Event: ' + esc(e.name) + "</option>"; }).join("") +
                s.perClub.map(function (c) { return '<option value="CLUB/' + c.id + '">Club: ' + esc(c.name) + "</option>"; }).join("");
            if (prev) sel.value = prev;
            await loadRegs();
        });

        var loadRegs = guard(async function () {
            var sel = document.getElementById("regTarget");
            var box = document.getElementById("regTable");
            if (!sel.value) { box.innerHTML = emptyHtml("Create an event or club first."); return; }
            var rows = await v2("/registrations/" + sel.value);
            var isEvent = sel.value.indexOf("EVENT") === 0;
            if (!rows.length) { box.innerHTML = emptyHtml("No registrations yet."); return; }
            box.innerHTML = '<table class="table align-middle mb-0"><thead><tr><th>ID</th><th>Name</th><th>Dept</th><th>Sem</th><th>Registered</th>' + (isEvent ? "<th>Attended</th>" : "") + "<th></th></tr></thead><tbody>" +
                rows.map(function (r) {
                    return "<tr><td>" + esc(r.studentId) + "</td><td>" + esc(r.studentName) + "</td><td>" + esc(r.department) + "</td><td>" + esc(r.semester) + "</td><td>" + esc(fmtDate(r.createdAt)) + "</td>" +
                        (isEvent ? '<td><div class="form-check form-switch"><input class="form-check-input" type="checkbox" data-att="' + r.id + '"' + (r.attended ? " checked" : "") + "></div></td>" : "") +
                        '<td class="text-end"><button class="btn btn-sm btn-outline-danger" data-remove="' + r.id + '">Remove</button></td></tr>';
                }).join("") + "</tbody></table>";
        });

        var loadNews = guard(async function () {
            var list = await fetch(window.CC_BASE + "/api/v2/announcements").then(function (r) { return r.json(); });
            document.getElementById("newsList").innerHTML = list.length ? list.map(function (a) { return announcementHtml(a, true); }).join("") : emptyHtml("No announcements yet.");
        });

        setupTabs(function (tab) { if (tab === "stats" || tab === "regs") loadStats(); if (tab === "news") loadNews(); });
        bindPassword();

        document.getElementById("regTarget").addEventListener("change", loadRegs);

        document.getElementById("exportBtn").addEventListener("click", guard(async function () {
            var sel = document.getElementById("regTarget");
            if (!sel.value) return;
            var res = await v2("/registrations/" + sel.value + "/export.csv", "GET", null, true);
            var blob = await res.blob();
            var a = document.createElement("a");
            a.href = URL.createObjectURL(blob);
            a.download = sel.value.replace("/", "-").toLowerCase() + "-registrations.csv";
            a.click();
            URL.revokeObjectURL(a.href);
        }));

        document.getElementById("regTable").addEventListener("change", guard(async function (ev) {
            var box = ev.target.closest("input[data-att]");
            if (box) await v2("/registrations/" + box.dataset.att + "/attendance", "PUT", { attended: box.checked });
        }));
        document.getElementById("regTable").addEventListener("click", guard(async function (ev) {
            var b = ev.target.closest("button[data-remove]");
            if (!b || !confirm("Remove this registration?")) return;
            await v2("/registrations/" + b.dataset.remove, "DELETE");
            await loadStats();
        }));

        document.getElementById("newsForm").addEventListener("submit", guard(async function (ev) {
            ev.preventDefault();
            var f = ev.target;
            await v2("/announcements", "POST", { title: f.title.value, message: f.message.value, pinned: f.pinned.checked });
            f.reset(); toast("Announcement posted"); await loadNews();
        }));
        document.getElementById("newsList").addEventListener("click", guard(async function (ev) {
            var b = ev.target.closest("button");
            if (!b) return;
            if (b.dataset.pin) await v2("/announcements/" + b.dataset.pin + "/pin", "PUT", { pinned: b.dataset.state === "1" });
            else if (b.dataset.del) { if (!confirm("Delete this announcement?")) return; await v2("/announcements/" + b.dataset.del, "DELETE"); }
            else return;
            await loadNews();
        }));

        loadStats();
    }

    document.addEventListener("DOMContentLoaded", function () {
        var page = document.body.dataset.page;
        if (page === "portal") initPortal();
        if (page === "student-hub") initStudentHub();
        if (page === "teacher-hub") initTeacherHub();
    });
})();
