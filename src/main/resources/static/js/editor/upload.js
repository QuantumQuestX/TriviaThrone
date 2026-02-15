function generateManifestXml(game, resourcesMap) {
    const esc = s => s == null ? '' : s.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&apos;');
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<game id="${game.id}" title="${esc(game.title)}" created="${new Date(game.created).toISOString()}">\n`;
    xml += `  <resources>\n`;
    for (const [id, meta] of resourcesMap.entries()) {
        xml += `    <resource id="${id}" name="${esc(meta.name)}" mime="${meta.mime}" size="${meta.size}" hash="${meta.hash}" path="resources/${id}/${meta.name}"/>\n`;
    }
    xml += `  </resources>\n`;
    xml += `  <structure>\n`;
    for (const round of game.rounds) {
        xml += `    <round id="${round.id}" title="${esc(round.title)}">\n`;

        for (const cat of round.categories) {
            xml += `      <category id="${cat.id}" title="${esc(cat.title)}">\n`;

            for (const q of cat.questions) {
                xml += `        <question id="${q.id}" type="${q.type}" points="${q.points}">\n`;

                xml += `          <prompt><![CDATA[${esc(q.prompt)}]]></prompt>\n`;
                if (q.choices?.length) {
                    xml += `          <choices>\n`;

                    for (const ch of q.choices) {
                        xml += `            <choice ${ch.correct ? 'correct="true"' : ''}>${esc(ch.text)}</choice>\n`;
                    }

                    xml += `          </choices>\n`;
                }

                xml += `        </question>\n`;
            }

            xml += `      </category>\n`;
        }

        xml += `    </round>\n`;
    }
    xml += `  </structure>\n`;
    xml += `</game>\n`;
    return xml;
}

export class Upload {
    constructor(resourceManager, token) {
        this.rm = resourceManager;
        this.token = token;
    }

    async uploadGame(game) {
        const manifest = generateManifestXml(game, this.rm.items);
        const formData = new FormData();

        formData.append('manifest', new Blob([manifest], { type: 'application/xml' }), 'manifest.xml');
        for (const [id, meta] of this.rm.items) {
            formData.append('resources', meta.blob, `${id}_${meta.name}`);
        }
        formData.append('metadata', new Blob([JSON.stringify({title: game.title})], {type: 'application/json'}), 'metadata.json');

        const resp = await fetch('/api/games/upload', {
            method: "POST",
            headers: { "Authorization": `Bearer ${this.token}` },
            body: formData
        });

        if (!resp.ok) {
            throw new Error("Upload failed: " + resp.status);
        }

        return resp.json();
    }
}