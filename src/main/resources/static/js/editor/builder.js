import JSZip from "https://cdn.skypack.dev/jszip";

export async function exportGame(game) {
    const zip = new JSZip();

    try {
        const manifestXml = buildManifestXml(game);
        zip.file("manifest.xml", manifestXml);
    } catch (err) {
        throw err;
    }

    const resourceFolder = zip.folder("resources");

    for (const resource of game.resources.resources.values()) {
        resourceFolder.file(
            `${resource.id}${getExtension(resource.name)}`,
            resource.file
        );
    }

    const blob = await zip.generateAsync( { type: "blob" } );

    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `${sanitizeFileName(game.meta?.title || "game")}.game.throne`;
    link.click();
}

function sanitizeFileName(name) {
    return name.replace(/[^a-z0-9_\-]/gi, "_");
}

function buildManifestXml(game) {
    const doc = document.implementation.createDocument("", "", null);

    const gameEl = doc.createElement("game");
    gameEl.setAttribute("version", game.meta?.version || "1.0");
    gameEl.setAttribute("id", game.meta?.id || crypto.randomUUID());
    doc.appendChild(gameEl);


    const metaEl = doc.createElement("meta");

    const titleEl = doc.createElement("title");
    titleEl.textContent = game.meta?.title || "";

    const descEl = doc.createElement("description");
    descEl.textContent = game.meta?.description || "";

    const createdEl = doc.createElement("created");
    createdEl.textContent = game.meta?.created || new Date().toISOString();

    metaEl.append(titleEl, descEl, createdEl);
    gameEl.appendChild(metaEl);

    
    const resourcesEl = doc.createElement("resources");
    for (const resource of game.resources.resources.values()) {
        const resEl = doc.createElement("resource");

        resEl.setAttribute("id", resource.id);
        resEl.setAttribute(
            "file",
            `resources/${resource.id}${getExtension(resource.name)}`
        );
        resEl.setAttribute("type", resource.type);

        resourcesEl.appendChild(resEl);
    }

    gameEl.appendChild(resourcesEl);


    const roundsEl = doc.createElement("rounds");

    game.rounds.forEach((round, roundIdx) => {
        const roundEl = doc.createElement("round");

        roundEl.setAttribute("index", roundIdx);
        roundEl.setAttribute("name", round.name);
        roundEl.setAttribute("questionCount", round.questionCount);
        roundEl.setAttribute("categoryCount", round.categoryCount);
        
        const categoryEl = doc.createElement("categories");
        if (round.categories) {
            round.categories.forEach((category, i) => {
                if (!category) return;

                const cEl = doc.createElement("category");
                cEl.setAttribute("row", i);

                cEl.textContent = category.name;

                categoryEl.appendChild(cEl);
            });
        }

        roundEl.appendChild(categoryEl);

        const questionsEl = doc.createElement("questions");

        if (round.questions) {
            round.questions.forEach((row, rowIdx) => {
                row.forEach((question, colIdx) => {
                    if (!question) return;


                    const hasContent = (question.prompt && question.prompt.trim() !== '') ||
                                       (question.options && question.options.length > 0) ||
                                       (question.media && question.media.trim() !== '');
                    if (!hasContent) {
                        throw new Error(
                            `Round "${round.name}" question at row ${rowIdx + 1}, category ${colIdx + 1} is empty. Add a prompt, options, or media.`
                        );
                    }

                    const qEl = doc.createElement("question");
                    qEl.setAttribute("row", rowIdx);
                    qEl.setAttribute("category", colIdx);
                    qEl.setAttribute("points", question.points ?? 0);
                    qEl.setAttribute("type", question.type || "text");

                    const promptEl = doc.createElement("prompt");
                    promptEl.textContent = question.prompt || "";
                    qEl.appendChild(promptEl);

                    if (question.options && Array.isArray(question.options)) {
                        const optionsEl = doc.createElement("options");

                        question.options.forEach(opt => {
                            const optEl = doc.createElement("option");
                            optEl.textContent = opt.text || "";

                            if (opt.correct) {
                                optEl.setAttribute("correct", "true");
                            }

                            optionsEl.appendChild(optEl);
                        })

                        qEl.appendChild(optionsEl);
                    }

                    if (question.media) {
                        const mediaEl = doc.createElement("media");
                        mediaEl.setAttribute("ref", question.media);
                        qEl.appendChild(mediaEl);
                    }

                    questionsEl.appendChild(qEl);
                });
            });
        }

        roundEl.appendChild(questionsEl);
        roundsEl.appendChild(roundEl);
    });

    if (roundsEl.childElementCount == 0) {
        throw new Error(`No rounds configured.`);
    }

    gameEl.appendChild(roundsEl);

    const xmlString = new XMLSerializer().serializeToString(doc);
    const prettyXml = formatXml(xmlString);
    return `<?xml version="1.0" encoding="UTF-8"?>\n${prettyXml}`;
}

function formatXml(xml) {
    const PADDING = '    ';
    const reg = /(>)(<)(\/*)/g;
    let xmlStr = xml.replace(reg, "$1\n$2$3");
    let pad = 0;
    return xmlStr
        .split("\n")
        .map((node) => {
            let indent = "";
            if (node.match(/.+<\/\w[^>]*>$/))
                indent = PADDING.repeat(pad);
            else if (node.match(/^<\/\w/)) {
                pad = pad > 0 ? pad - 1 : 0;
                indent = PADDING.repeat(pad);
            } else if (node.match(/^<\w[^>]*[^\/]>.*$/)) {
                indent = PADDING.repeat(pad);
                pad += 1;
            } else
                indent = PADDING.repeat(pad);
            return indent + node;
        })
        .join("\n");
}

function getExtension(filename) {
    if (!filename) return "";

    const idx = filename.lastIndexOf(".");
    return idx !== -1 ? filename.slice(idx) : "";
}