import { bootAuthenticatedPage } from "../root/page-bootstrap.js";
import { apiFetch } from "../root/api.js";

await bootAuthenticatedPage(async () => {
    const library = document.getElementById("game-library");
    const libraryStatus = document.getElementById("library-status");
    const libraryCount = document.getElementById("creator-count");
    const editor = document.getElementById("creator-editor");
    const editorTitle = document.getElementById("creator-editor-title");
    const editorDescription = document.getElementById("creator-editor-description");
    const form = document.getElementById("creator-form");
    const status = document.getElementById("creator-status");
    const backButton = document.getElementById("back-to-library");

    const response = await apiFetch("/api/game/creators");
    if (!response.ok) {
        throw new Error(`Could not load game creators (${response.status}).`);
    }

    const creators = await response.json();
    if (!Array.isArray(creators) || creators.length === 0) {
        libraryStatus.textContent = "No games are available yet. Install a game mod to fill your library.";
        return;
    }

    libraryCount.textContent = `${creators.length} ${creators.length === 1 ? "game" : "games"}`;
    libraryStatus.hidden = true;

    let selectedCreator = null;
    for (const creator of creators) {
        library.append(createGameCard(creator, () => {
            selectedCreator = creator;
            editorTitle.textContent = creator.descriptor.title;
            editorDescription.textContent = creator.descriptor.description;
            editor.hidden = false;
            library.hidden = true;
            renderForm(form, creator.descriptor.editorSchema);
            status.textContent = "";
            status.className = "tt-status";
        }));
    }

    backButton.addEventListener("click", () => {
        editor.hidden = true;
        library.hidden = false;
    });
    form.addEventListener("submit", async event => {
        event.preventDefault();
        status.textContent = "";
        status.className = "tt-status";

        const submit = form.querySelector("button[type='submit']");
        submit.disabled = true;
        try {
            const creator = selectedCreator;
            const data = readForm(form, creator.descriptor.editorSchema);
            const createResponse = await apiFetch(
                `/api/game/create/${encodeURIComponent(creator.gameType)}`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ data })
                }
            );

            if (!createResponse.ok) {
                status.classList.add("tt-status--error");
                status.textContent = await createResponse.text();
                return;
            }

            const blob = await createResponse.blob();
            const objectUrl = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = objectUrl;
            link.download = `${creator.gameType}.game.throne`;
            document.body.append(link);
            link.click();
            link.remove();
            window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
            status.classList.add("tt-status--success");
            status.textContent = "Game file downloaded.";
        } catch (error) {
            console.error("Could not generate the game file.", error);
            status.classList.add("tt-status--error");
            status.textContent = error instanceof Error
                ? error.message
                : "Could not generate the game file.";
        } finally {
            submit.disabled = false;
        }
    });
});

function createGameCard(creator, openGenericEditor) {
    const card = document.createElement(creator.hasCustomEditor ? "a" : "button");
    card.className = "game-card";
    if (creator.hasCustomEditor) {
        card.href = `/games/${encodeURIComponent(creator.gameType)}/editor`;
    } else {
        card.type = "button";
        card.addEventListener("click", openGenericEditor);
    }
    const cover = document.createElement("span");
    cover.className = "game-card__cover";
    cover.setAttribute("aria-hidden", "true");
    const symbol = document.createElement("span");
    symbol.className = "game-card__symbol";
    symbol.textContent = creator.gameType.slice(0, 1).toUpperCase();
    const badge = document.createElement("span");
    badge.className = "game-card__badge";
    badge.textContent = creator.gameType.toUpperCase();
    cover.append(symbol, badge);

    const body = document.createElement("span");
    body.className = "game-card__body";
    const title = document.createElement("span");
    title.className = "game-card__title";
    title.textContent = creator.descriptor.title;
    const description = document.createElement("span");
    description.className = "game-card__description";
    description.textContent = creator.descriptor.description;
    const action = document.createElement("span");
    action.className = "game-card__action";
    action.append(
        document.createTextNode(creator.hasCustomEditor ? "Open game editor" : "Start creating"),
        document.createTextNode(" →")
    );
    body.append(title, description, action);
    card.append(cover, body);
    return card;
}

function renderForm(form, schema) {
    if (schema.type !== "object" || !schema.properties) {
        throw new Error("This editor requires an object schema with properties.");
    }

    form.replaceChildren();
    const required = new Set(schema.required ?? []);

    for (const [name, field] of Object.entries(schema.properties)) {
        const fieldContainer = document.createElement("div");
        fieldContainer.className = "tt-field";
        const label = document.createElement("label");
        label.className = "tt-field__label";
        label.htmlFor = `creator-field-${name}`;
        label.textContent = field.title ?? name;

        let input;
        if (Array.isArray(field.enum)) {
            input = document.createElement("select");
            for (const [index, value] of field.enum.entries()) {
                const option = document.createElement("option");
                option.value = String(value);
                option.textContent = field.enumNames?.[index] ?? String(value);
                input.append(option);
            }
        } else {
            input = document.createElement("input");
            if (field.type === "integer" || field.type === "number") {
                input.type = "number";
                if (field.type === "integer") input.step = "1";
                if (field.minimum !== undefined) input.min = String(field.minimum);
                if (field.maximum !== undefined) input.max = String(field.maximum);
                if (field.multipleOf !== undefined) input.step = String(field.multipleOf);
            } else if (field.type === "boolean") {
                input.type = "checkbox";
            } else if (field.type === "string") {
                input.type = "text";
                if (field.minLength !== undefined) input.minLength = field.minLength;
                if (field.maxLength !== undefined) input.maxLength = field.maxLength;
            } else {
                throw new Error(`Unsupported field type for "${name}".`);
            }
        }

        input.id = label.htmlFor;
        input.name = name;
        input.required = required.has(name);
        if (input.type === "checkbox") {
            input.className = "tt-checkbox";
            fieldContainer.classList.add("tt-field--checkbox");
        } else {
            input.className = "tt-control";
        }
        if (field.default !== undefined) {
            if (input.type === "checkbox") {
                input.checked = Boolean(field.default);
            } else {
                input.value = String(field.default);
            }
        }
        fieldContainer.append(label, input);

        if (field.description) {
            const description = document.createElement("small");
            description.className = "tt-field__hint";
            description.textContent = field.description;
            fieldContainer.append(description);
        }
        form.append(fieldContainer);
    }

    const submit = document.createElement("button");
    submit.type = "submit";
    submit.className = "tt-button tt-button--primary tt-grid__full creator-submit";
    submit.textContent = "Generate game file";
    form.append(submit);
}

function readForm(form, schema) {
    const data = {};
    for (const [name, field] of Object.entries(schema.properties)) {
        const input = form.elements.namedItem(name);
        if (field.type === "boolean") {
            data[name] = input.checked;
        } else if (field.type === "integer") {
            data[name] = Number.parseInt(input.value, 10);
        } else if (field.type === "number") {
            data[name] = Number(input.value);
        } else if (Array.isArray(field.enum) && field.enum.every(value => typeof value === "number")) {
            data[name] = Number(input.value);
        } else {
            data[name] = input.value;
        }
    }
    return data;
}
