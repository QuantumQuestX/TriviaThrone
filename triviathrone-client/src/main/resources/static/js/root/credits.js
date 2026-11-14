const topbarAuthorsList = document.getElementById("topbar-authors");

fetch("/api/developers", { headers: { Accept: "application/json" } })
    .then((response) => {
        if (!response.ok) {
            throw new Error(`Developer metadata request failed with status ${response.status}`);
        }

        return response.json();
    })
    .then((developers) => {
        if (!Array.isArray(developers)) {
            throw new Error("Developer metadata response was not a list");
        }

        const maintainers = developers.filter(
            (developer) =>
                Array.isArray(developer.roles) &&
                developer.roles.some((role) => typeof role === "string" && role.trim().toLowerCase() === "maintainer"),
        );

        renderDevelopers(topbarAuthorsList, maintainers, "No maintainers listed.");
    })
    .catch((error) => {
        console.error("Could not load project credits:", error);
        showMessage(topbarAuthorsList, "Credits unavailable.");
    });

function renderDevelopers(list, developers, emptyMessage) {
    if (!list) {
        return;
    }

    list.replaceChildren();

    if (developers.length === 0) {
        showMessage(list, emptyMessage);

        return;
    }

    developers.forEach((developer) => {
        const item = document.createElement("li");
        const name = typeof developer.name === "string" ? developer.name : "Unknown contributor";
        const profileUrl = validWebUrl(developer.url);

        if (profileUrl) {
            appendLink(item, name, profileUrl);
        } else {
            item.append(document.createTextNode(name));
        }

        const organizations = Array.isArray(developer.organizations) ? developer.organizations : [];

        if (organizations.length > 0) {
            item.append(document.createTextNode(" @ "));
            organizations.forEach((organization, organizationIndex) => {
                if (organizationIndex > 0) {
                    item.append(document.createTextNode(", "));
                }

                appendOrganization(item, organization);
            });
        }

        list.append(item);
    });
}

function appendOrganization(item, organization) {
    const name = organization.name ?? "";
    const url = validWebUrl(organization.url);

    if (!url) {
        item.append(document.createTextNode(name));

        return;
    }

    appendLink(item, name, url);
}

function appendLink(parent, text, url) {
    const link = document.createElement("a");

    link.href = url;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.textContent = text;
    parent.append(link);
}

function validWebUrl(value) {
    if (typeof value !== "string" || value.trim() === "") {
        return null;
    }

    try {
        const url = new URL(value);

        return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
    } catch {
        return null;
    }
}

function showMessage(list, message) {
    if (!list) {
        return;
    }

    const item = document.createElement("li");

    item.textContent = message;
    list.replaceChildren(item);
}
