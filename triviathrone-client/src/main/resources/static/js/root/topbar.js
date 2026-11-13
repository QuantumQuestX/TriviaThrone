import {getAccessTokenExpiresAt, getCurrentUser, logout} from "./auth.js";

const mount = document.querySelector("[data-topbar]");
if (mount) {
    const page = mount.dataset.topbar;
    const topbar = createTopbar(page);
    mount.replaceWith(topbar);
}

document.getElementById("logout-btn")?.addEventListener("click", async () => {
    try {
        await logout();
        window.location.replace("/login");
    } catch (error) {
        window.alert(error instanceof Error ? error.message : "Could not end the session.");
    }
});

function createTopbar(page) {
    const header = document.createElement("header");
    header.className = "site-header tt-header";

    const brand = document.createElement("div");
    brand.className = "site-brand";
    const brandLink = document.createElement("a");
    brandLink.className = "tt-brand site-header__brand";
    brandLink.href = "/";
    brandLink.textContent = "TriviaThrone";
    const version = document.createElement("span");
    version.id = "app-version";
    version.className = "app-version";
    version.setAttribute("aria-live", "polite");
    version.textContent = "Version loading…";
    brand.append(brandLink, version);

    if (page === "game") {
        const gameTitle = document.createElement("p");
        gameTitle.id = "game-title";
        gameTitle.className = "site-header__context";
        gameTitle.textContent = "TriviaThrone";
        brand.append(gameTitle);
    }

    header.append(brand, createCredits());

    if (page === "lobby" || page === "game") {
        header.append(createLobbyInfo());
    }

    if (page !== "login") {
        header.append(createActions(page));
    }

    return header;
}

function createCredits() {
    const credits = document.createElement("div");
    credits.className = "topbar-credits";
    credits.setAttribute("aria-label", "Made by");
    const label = document.createElement("span");
    label.textContent = "Made by";
    const authors = document.createElement("ul");
    authors.id = "topbar-authors";
    const loading = document.createElement("li");
    loading.setAttribute("role", "status");
    loading.textContent = "Loading…";
    authors.append(loading);
    credits.append(label, authors);
    return credits;
}

function createLobbyInfo() {
    const info = document.createElement("div");
    info.id = "lobby-info";
    info.className = "site-header__lobby";
    info.append(document.createTextNode("Lobby "));
    const code = document.createElement("span");
    code.id = "lobby-code";
    code.textContent = "A123";
    info.append(code);
    return info;
}

function createActions(page) {
    const nav = document.createElement("nav");
    nav.className = "site-header__actions";
    nav.setAttribute("aria-label", "Main navigation");

    const user = document.createElement("p");
    user.id = "username-info";
    user.className = "site-header__user";
    user.append(document.createTextNode("Logged in as: "));
    const username = document.createElement("span");
    username.id = "username-display";
    username.dataset.currentUser = "";
    user.append(username);
    nav.append(user);

    if (page === "dashboard") {
        appendLink(nav, "Dashboard", "/");
        appendLink(nav, "Game library", "/game-creator");
        appendButton(nav, "Account", "account-btn");
        appendButton(nav, "Logout", "logout-btn", "danger");
    } else if (page === "lobby" || page === "game") {
        appendButton(nav, "Leave Lobby", "leave-btn", "danger");
    } else if (page === "editor") {
        appendLink(nav, "Dashboard", "/");
        appendLink(nav, "Game library", "/game-creator");
        appendButton(nav, "Save", "save-draft-btn", "primary");
        appendButton(nav, "Logout", "logout-btn", "danger");
    } else {
        appendLink(nav, "Dashboard", "/");
        if (page !== "game-creator") {
            appendLink(nav, "Game library", "/game-creator");
        }
        appendButton(nav, "Logout", "logout-btn", "danger");
    }

    return nav;
}

function appendButton(nav, label, id, variant = "secondary") {
    const button = document.createElement("button");
    button.id = id;
    button.type = "button";
    button.className = `tt-button tt-button--${variant}`;
    button.textContent = label;
    nav.append(button);
}

function appendLink(nav, label, href) {
    const link = document.createElement("a");
    link.className = "tt-button tt-button--secondary";
    link.href = href;
    link.textContent = label;
    nav.append(link);
}

export function initializeTopbarValues() {
    document.querySelectorAll("[data-current-user]").forEach(element => {
        element.textContent = getCurrentUser() || "Unknown";
    });

    const expiryElement = document.getElementById("token-expiry");
    if (expiryElement) updateTokenExpiry(expiryElement);
}

function updateTokenExpiry(element) {
    const minutes = Math.max(0, Math.ceil((getAccessTokenExpiresAt() - Date.now()) / 60_000));
    element.textContent = minutes
        ? `${minutes} minute${minutes === 1 ? "" : "s"}`
        : "Refreshing…";
    window.setTimeout(() => updateTokenExpiry(element), 60_000);
}
