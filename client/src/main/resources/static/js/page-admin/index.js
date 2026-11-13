import {bootAuthenticatedPage} from "../root/page-bootstrap.js";

await bootAuthenticatedPage(async () => {
    await import("./users.js");
});
