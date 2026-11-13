import {apiFetch} from "../root/api.js";

const panel = document.getElementById("admin-section");
const userList = document.getElementById("admin-user-list");
const message = document.getElementById("admin-message");
const createForm = document.getElementById("admin-create-user-form");
const roleDropdown = document.getElementById("create-role-dropdown");
const roles = [ "USER", "ADMIN" ];

if (panel && userList && message && createForm && roleDropdown) {
    roleDropdown.append(createRoleDropdown(["USER"], "roles").element);
    createForm.addEventListener("submit", createUser);
    void loadUsers();
}

async function loadUsers() {
    try {
        const response = await apiFetch("/api/admin/users");
        if (response.status === 403) {
            window.location.replace("/");
            return;
        }
        if (!response.ok) {
            setMessage(`Could not load users: ${(await responseError(response)).message}`, "error");
            userList.replaceChildren();
            return;
        }

        renderUsers(await response.json());
    } catch (error) {
        setMessage(`Could not load users: ${error.message}`, "error");
        userList.replaceChildren();
    }
}

async function createUser(event) {
    event.preventDefault();
    const formData = new FormData(createForm);
    const submitButton = createForm.querySelector('button[type="submit"]');
    submitButton.disabled = true;

    try {
        const response = await apiFetch("/api/admin/users", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                username: formData.get("username"),
                displayName: formData.get("displayName"),
                password: formData.get("password"),
                roles: formData.getAll("roles")
            })
        });
        if (!response.ok) {
            setMessage((await responseError(response)).message, "error");
            return;
        }

        createForm.reset();
        roleDropdown.replaceChildren(createRoleDropdown(["USER"], "roles").element);
        setMessage("User added.", "success");
        await loadUsers();
    } catch (error) {
        setMessage(error.message, "error");
    } finally {
        submitButton.disabled = false;
    }
}

function renderUsers(users) {
    const rows = users.map(user => createUserRow(user));
    if (rows.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 5;
        cell.setAttribute("role", "status");
        cell.textContent = "No users found.";
        row.append(cell);
        rows.push(row);
    }
    userList.replaceChildren(...rows);
}

function createUserRow(user) {
    const row = document.createElement("tr");
    row.append(
        textCell(user.username),
        textCell(user.displayName || "—"),
        createRolesCell(user),
        createEnabledCell(user),
        createActionsCell(user)
    );
    return row;
}

function createRolesCell(user) {
    const cell = document.createElement("td");
    const roleSelector = createRoleDropdown(user.roles, null, user.defaultAdmin);
    const saveButton = createButton("Save roles", "secondary");
    saveButton.disabled = true;
    const updateButtonState = () => {
        saveButton.disabled = sameRoles(roleSelector.getRoles(), user.roles);
    };
    roleSelector.inputs.forEach(input => input.addEventListener("change", updateButtonState));
    saveButton.title = user.defaultAdmin ? "The default admin roles cannot be changed" : "";
    saveButton.disabled = user.defaultAdmin;
    saveButton.addEventListener("click", async () => {
        saveButton.disabled = true;
        try {
            await sendJson(`/api/admin/users/${encodeURIComponent(user.username)}/roles`, "PUT", {
                roles: roleSelector.getRoles()
            });
            setMessage(`Roles updated for ${user.username}.`, "success");
            await loadUsers();
        } catch (error) {
            roleSelector.setRoles(user.roles);
            setMessage(error.message, "error");
        } finally {
            updateButtonState();
        }
    });
    cell.append(roleSelector.element, saveButton);
    return cell;
}

function createRoleDropdown(selectedRoles, name = null, disabled = false) {
    const details = document.createElement("details");
    details.className = "role-dropdown";
    const summary = document.createElement("summary");
    const options = document.createElement("div");
    options.className = "role-dropdown__options";
    const inputs = roles.map(role => {
        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = role;
        checkbox.checked = selectedRoles.includes(role);
        checkbox.disabled = disabled;
        if (name) checkbox.name = name;
        checkbox.setAttribute("aria-label", `${role} role`);
        label.append(checkbox, document.createTextNode(role));
        options.append(label);
        return checkbox;
    });
    const getRoles = () => inputs.filter(input => input.checked).map(input => input.value);
    const updateSummary = () => {
        const selected = getRoles();
        summary.textContent = selected.length ? selected.join(", ") : "No roles";
        summary.title = selected.includes("USER") ? "" : "This account cannot sign in without USER";
    };
    inputs.forEach(input => input.addEventListener("change", updateSummary));
    details.append(summary, options);
    updateSummary();
    return {
        element: details,
        inputs,
        getRoles,
        setRoles(nextRoles) {
            inputs.forEach(input => {
                input.checked = nextRoles.includes(input.value);
            });
            updateSummary();
        }
    };
}

function sameRoles(first, second) {
    return first.length === second.length && first.every(role => second.includes(role));
}

function createEnabledCell(user) {
    const cell = document.createElement("td");
    const label = document.createElement("label");
    label.className = "admin-enabled-control";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = user.enabled;
    checkbox.disabled = user.defaultAdmin;
    checkbox.setAttribute("aria-label", `Enabled status for ${user.username}`);
    const state = document.createElement("span");
    const updateStateLabel = () => {
        const status = checkbox.checked ? "Enabled" : "Disabled";
        state.textContent = user.defaultAdmin ? `${status} (managed)` : status;
    };
    updateStateLabel();
    label.append(checkbox, state);

    checkbox.addEventListener("change", async () => {
        checkbox.disabled = true;
        updateStateLabel();
        try {
            await sendJson(`/api/admin/users/${encodeURIComponent(user.username)}/enabled`, "PUT", {
                enabled: checkbox.checked
            });
            setMessage(`${user.username} ${checkbox.checked ? "enabled" : "disabled"}.`, "success");
            await loadUsers();
        } catch (error) {
            checkbox.checked = user.enabled;
            updateStateLabel();
            setMessage(error.message, "error");
        } finally {
            checkbox.disabled = user.defaultAdmin;
        }
    });
    cell.append(label);
    return cell;
}

function createActionsCell(user) {
    const cell = document.createElement("td");
    const removeButton = createButton("Remove", "danger");
    removeButton.disabled = user.defaultAdmin;
    removeButton.title = user.defaultAdmin ? "The default admin cannot be removed" : `Remove ${user.username}`;
    removeButton.addEventListener("click", async () => {
        if (!window.confirm(`Remove user "${user.username}"?`)) return;
        removeButton.disabled = true;
        try {
            const response = await apiFetch(`/api/admin/users/${encodeURIComponent(user.username)}`, {
                method: "DELETE"
            });
            if (!response.ok) {
                setMessage((await responseError(response)).message, "error");
                return;
            }
            setMessage(`Removed ${user.username}.`, "success");
            await loadUsers();
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            removeButton.disabled = user.defaultAdmin;
        }
    });
    cell.append(removeButton);
    return cell;
}

async function sendJson(url, method, body) {
    const response = await apiFetch(url, {
        method,
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    });
    if (!response.ok) throw await responseError(response);
    return response.status === 204 ? null : response.json();
}

async function responseError(response) {
    const text = await response.text();
    if (text) {
        try {
            const problem = JSON.parse(text);
            return new Error(problem.detail || problem.message || problem.title || `Request failed (${response.status}).`);
        } catch {
            return new Error(text);
        }
    }
    return new Error(`Request failed (${response.status}).`);
}

function textCell(text) {
    const cell = document.createElement("td");
    cell.textContent = text;
    return cell;
}

function createButton(text, variant) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `tt-button tt-button--${variant}`;
    button.textContent = text;
    return button;
}

function setMessage(text, type) {
    message.textContent = text;
    message.className = "tt-status";
    if (type) message.classList.add(`tt-status--${type}`);
}
