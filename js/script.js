const API = "http://localhost:8080/api";

function saveSession(role, user) {
    localStorage.setItem("role", role);
    localStorage.setItem("user", JSON.stringify(user));
}

function getRole() {
    return localStorage.getItem("role");
}

function getUser() {
    try {
        return JSON.parse(localStorage.getItem("user"));
    } catch (e) {
        return null;
    }
}

function logout() {
    localStorage.removeItem("role");
    localStorage.removeItem("user");
    window.location.href = "index.html";
}

function requireLogin(role) {
    const current = getRole();
    if (!current || (role && current !== role)) {
        window.location.href = "index.html";
        return false;
    }
    return true;
}

function escapeHtml(text) {
    if (text === null || text === undefined) return "";
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function safeLink(url) {
    if (url && /^https?:\/\//i.test(url.trim())) return url.trim();
    return "";
}

function emptyHtml(message) {
    return '<div class="empty-state"><i class="bi bi-inbox"></i>' + escapeHtml(message) + "</div>";
}

function errorHtml(message) {
    return '<div class="alert alert-danger">' + escapeHtml(message) + "</div>";
}

async function api(path, method = "GET", body = null) {
    const options = { method: method, headers: { "Content-Type": "application/json" } };
    if (body) options.body = JSON.stringify(body);

    let response;
    try {
        response = await fetch(API + path, options);
    } catch (e) {
        throw new Error("Cannot reach the server. Make sure Spring Boot is running on port 8080.");
    }

    if (!response.ok) {
        let message = "Request failed (" + response.status + ")";
        try {
            const data = await response.json();
            if (data.message) message = data.message;
        } catch (e) {}
        throw new Error(message);
    }

    if (response.status === 204) return null;
    return response.json();
}

function setBackLink() {
    const link = document.getElementById("backLink");
    if (link) link.href = getRole() + "-dashboard.html";
}

function initLogin(role) {
    const form = document.getElementById("loginForm");
    const errorBox = document.getElementById("errorBox");

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        errorBox.classList.add("d-none");
        const button = form.querySelector("button[type=submit]");
        button.disabled = true;

        try {
            const user = await api("/login/" + role, "POST", {
                username: form.username.value.trim(),
                password: form.password.value
            });
            saveSession(role, user);
            window.location.href = role + "-dashboard.html";
        } catch (error) {
            errorBox.textContent = error.message;
            errorBox.classList.remove("d-none");
            button.disabled = false;
        }
    });
}

async function initStudentDashboard() {
    if (!requireLogin("student")) return;

    const user = getUser();
    document.getElementById("welcomeName").textContent = user.name;

    try {
        const clubs = await api("/clubs");
        const events = await api("/events");
        document.getElementById("clubCount").textContent = clubs.length;
        document.getElementById("eventCount").textContent = events.length;
    } catch (error) {}
}

function clubCard(club) {
    const link = safeLink(club.googleFormLink);
    const button = link
        ? '<a class="btn btn-primary w-100" href="' + escapeHtml(link) + '" target="_blank" rel="noopener"><i class="bi bi-box-arrow-up-right me-1"></i> Join Club</a>'
        : '<button class="btn btn-secondary w-100" disabled>Form not available</button>';

    return '<div class="col-md-6 col-lg-4"><div class="card item-card"><div class="card-body p-4 d-flex flex-column">' +
        '<div class="d-flex align-items-center gap-3 mb-3"><div class="item-icon"><i class="bi bi-people-fill"></i></div>' +
        "<div><h5 class=\"mb-0\">" + escapeHtml(club.clubName) + '</h5><span class="badge bg-light text-dark">' + escapeHtml(club.department) + "</span></div></div>" +
        '<p class="text-muted">' + escapeHtml(club.description) + "</p>" +
        '<ul class="info-list mb-4"><li><i class="bi bi-person-badge"></i>Coordinator: ' + escapeHtml(club.teacherCoordinator) + "</li></ul>" +
        '<div class="mt-auto">' + button + "</div></div></div></div>";
}

function eventCard(event) {
    const link = safeLink(event.googleFormLink);
    const button = link
        ? '<a class="btn btn-primary w-100" href="' + escapeHtml(link) + '" target="_blank" rel="noopener"><i class="bi bi-box-arrow-up-right me-1"></i> Register</a>'
        : '<button class="btn btn-secondary w-100" disabled>Form not available</button>';

    return '<div class="col-md-6 col-lg-4"><div class="card item-card"><div class="card-body p-4 d-flex flex-column">' +
        '<div class="d-flex align-items-center gap-3 mb-3"><div class="item-icon"><i class="bi bi-calendar-event-fill"></i></div>' +
        "<h5 class=\"mb-0\">" + escapeHtml(event.eventName) + "</h5></div>" +
        '<p class="text-muted">' + escapeHtml(event.description) + "</p>" +
        '<ul class="info-list mb-4">' +
        '<li><i class="bi bi-calendar3"></i>' + escapeHtml(event.date) + "</li>" +
        '<li><i class="bi bi-clock"></i>' + escapeHtml(event.time) + "</li>" +
        '<li><i class="bi bi-geo-alt"></i>' + escapeHtml(event.venue) + "</li>" +
        '<li><i class="bi bi-people"></i>Maximum participants: ' + escapeHtml(event.maximumParticipants) + "</li></ul>" +
        '<div class="mt-auto">' + button + "</div></div></div></div>";
}

async function initClubs() {
    if (!requireLogin()) return;
    setBackLink();

    const box = document.getElementById("clubList");
    try {
        const clubs = await api("/clubs");
        box.innerHTML = clubs.length ? clubs.map(clubCard).join("") : emptyHtml("No clubs available yet.");
    } catch (error) {
        box.innerHTML = errorHtml(error.message);
    }
}

async function initEvents() {
    if (!requireLogin()) return;
    setBackLink();

    const box = document.getElementById("eventList");
    try {
        const events = await api("/events");
        box.innerHTML = events.length ? events.map(eventCard).join("") : emptyHtml("No events available yet.");
    } catch (error) {
        box.innerHTML = errorHtml(error.message);
    }
}

const entities = {
    students: {
        label: "Student",
        path: "/students",
        columns: ["studentId", "name", "email", "phone", "department", "semester"],
        fields: [
            { key: "studentId", label: "Student ID", type: "text" },
            { key: "name", label: "Full Name", type: "text" },
            { key: "email", label: "Email", type: "email" },
            { key: "phone", label: "Phone", type: "text" },
            { key: "department", label: "Department", type: "text" },
            { key: "semester", label: "Semester", type: "text" },
            { key: "password", label: "Password", type: "password", secret: true }
        ],
        rows: []
    },
    clubs: {
        label: "Club",
        path: "/clubs",
        columns: ["clubName", "department", "teacherCoordinator", "googleFormLink"],
        fields: [
            { key: "clubName", label: "Club Name", type: "text" },
            { key: "description", label: "Description", type: "textarea", optional: true },
            { key: "department", label: "Department", type: "text" },
            { key: "teacherCoordinator", label: "Teacher Coordinator", type: "text" },
            { key: "googleFormLink", label: "Google Form Link", type: "url", optional: true }
        ],
        rows: []
    },
    events: {
        label: "Event",
        path: "/events",
        columns: ["eventName", "date", "time", "venue", "maximumParticipants", "googleFormLink"],
        fields: [
            { key: "eventName", label: "Event Name", type: "text" },
            { key: "description", label: "Description", type: "textarea", optional: true },
            { key: "date", label: "Date", type: "date" },
            { key: "time", label: "Time", type: "time" },
            { key: "venue", label: "Venue", type: "text" },
            { key: "maximumParticipants", label: "Maximum Participants", type: "number", optional: true },
            { key: "googleFormLink", label: "Google Form Link", type: "url", optional: true }
        ],
        rows: []
    }
};

let currentKey = null;
let editingId = null;
let entityModal = null;

function showMessage(text, type) {
    document.getElementById("alertBox").innerHTML =
        '<div class="alert alert-' + type + ' alert-dismissible fade show">' + escapeHtml(text) +
        '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
}

function fieldLabel(entity, key) {
    return entity.fields.find(function (f) { return f.key === key; }).label;
}

function cellValue(key, value) {
    if (key === "googleFormLink") {
        const link = safeLink(value);
        return link ? '<a href="' + escapeHtml(link) + '" target="_blank" rel="noopener">Open form</a>' : "-";
    }
    return value === null || value === undefined || value === "" ? "-" : escapeHtml(value);
}

function renderTable(key) {
    const entity = entities[key];
    const box = document.getElementById("table-" + key);

    if (entity.rows.length === 0) {
        box.innerHTML = emptyHtml("No " + entity.label.toLowerCase() + " records found.");
        return;
    }

    let html = '<table class="table table-hover align-middle mb-0"><thead><tr>';
    entity.columns.forEach(function (col) {
        html += "<th>" + escapeHtml(fieldLabel(entity, col)) + "</th>";
    });
    html += '<th class="text-end">Actions</th></tr></thead><tbody>';

    entity.rows.forEach(function (row) {
        html += "<tr>";
        entity.columns.forEach(function (col) {
            html += "<td>" + cellValue(col, row[col]) + "</td>";
        });
        html += '<td class="text-end text-nowrap">' +
            '<button class="btn btn-sm btn-outline-primary me-1" data-action="edit" data-entity="' + key + '" data-id="' + row.id + '"><i class="bi bi-pencil-square"></i> Edit</button>' +
            '<button class="btn btn-sm btn-outline-danger" data-action="delete" data-entity="' + key + '" data-id="' + row.id + '"><i class="bi bi-trash"></i> Delete</button>' +
            "</td></tr>";
    });

    html += "</tbody></table>";
    box.innerHTML = html;
}

async function loadTable(key) {
    const entity = entities[key];
    const box = document.getElementById("table-" + key);
    let path = entity.path;
    let searching = false;

    if (key === "students") {
        const term = document.getElementById("studentSearch").value.trim();
        if (term) {
            path += "/search?name=" + encodeURIComponent(term);
            searching = true;
        }
    }

    try {
        entity.rows = await api(path);
        if (!searching) {
            document.getElementById("count-" + key).textContent = entity.rows.length;
        }
        renderTable(key);
    } catch (error) {
        box.innerHTML = errorHtml(error.message);
    }
}

function openForm(key, id) {
    const entity = entities[key];
    currentKey = key;
    editingId = id || null;

    let row = null;
    if (id) {
        row = entity.rows.find(function (r) { return r.id === id; });
    }

    document.getElementById("modalTitle").textContent = (row ? "Edit " : "Add ") + entity.label;

    let html = "";
    entity.fields.forEach(function (f) {
        const value = row && !f.secret && row[f.key] !== null && row[f.key] !== undefined ? row[f.key] : "";
        const required = f.secret ? !row : !f.optional;
        const placeholder = f.secret && row ? "Leave blank to keep current password" : "";
        const inputId = "f-" + f.key;

        html += '<div class="mb-3"><label class="form-label" for="' + inputId + '">' + escapeHtml(f.label) + "</label>";
        if (f.type === "textarea") {
            html += '<textarea class="form-control" id="' + inputId + '" rows="3"' + (required ? " required" : "") + ">" + escapeHtml(value) + "</textarea>";
        } else {
            html += '<input class="form-control" type="' + f.type + '" id="' + inputId + '" value="' + escapeHtml(value) + '" placeholder="' + placeholder + '"' + (required ? " required" : "") + ">";
        }
        html += "</div>";
    });

    document.getElementById("formFields").innerHTML = html;
    entityModal.show();
}

async function saveForm(event) {
    event.preventDefault();
    const entity = entities[currentKey];
    const data = {};

    entity.fields.forEach(function (f) {
        let value = document.getElementById("f-" + f.key).value;
        if (f.type === "number") {
            value = value === "" ? null : Number(value);
        } else if (!f.secret) {
            value = value.trim();
        }
        data[f.key] = value;
    });

    try {
        if (editingId) {
            await api(entity.path + "/" + editingId, "PUT", data);
        } else {
            await api(entity.path, "POST", data);
        }
        entityModal.hide();
        await loadTable(currentKey);
        showMessage(entity.label + " saved successfully.", "success");
    } catch (error) {
        showMessage(error.message, "danger");
    }
}

async function deleteRow(key, id) {
    const entity = entities[key];
    if (!confirm("Delete this " + entity.label.toLowerCase() + "?")) return;

    try {
        await api(entity.path + "/" + id, "DELETE");
        await loadTable(key);
        showMessage(entity.label + " deleted.", "success");
    } catch (error) {
        showMessage(error.message, "danger");
    }
}

function initTeacherDashboard() {
    if (!requireLogin("teacher")) return;

    document.getElementById("welcomeName").textContent = getUser().name;
    entityModal = new bootstrap.Modal(document.getElementById("entityModal"));

    Object.keys(entities).forEach(loadTable);

    document.getElementById("entityForm").addEventListener("submit", saveForm);
    document.getElementById("studentSearch").addEventListener("input", function () {
        loadTable("students");
    });

    document.getElementById("tabContent").addEventListener("click", function (event) {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        const key = button.dataset.entity;
        const id = Number(button.dataset.id);

        if (button.dataset.action === "add") openForm(key);
        if (button.dataset.action === "edit") openForm(key, id);
        if (button.dataset.action === "delete") deleteRow(key, id);
    });
}

document.addEventListener("DOMContentLoaded", function () {
    const page = document.body.dataset.page;

    if (page === "student-login") initLogin("student");
    if (page === "teacher-login") initLogin("teacher");
    if (page === "student-dashboard") initStudentDashboard();
    if (page === "teacher-dashboard") initTeacherDashboard();
    if (page === "clubs") initClubs();
    if (page === "events") initEvents();
});
