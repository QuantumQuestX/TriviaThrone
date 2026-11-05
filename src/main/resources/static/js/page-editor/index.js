import {bootAuthenticatedPage} from "../root/page-bootstrap.js";

await bootAuthenticatedPage(async () => {
    const editor = await import("../editor/editor.js");
    editor.initializeEditor();
});
