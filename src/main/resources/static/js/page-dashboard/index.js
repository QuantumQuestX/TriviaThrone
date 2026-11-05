import {bootAuthenticatedPage} from "../root/page-bootstrap.js";

await bootAuthenticatedPage(async () => {
    await import("./account.js");
    await import("./dashboard.js");
    await import("./lobby-browser.js");
});