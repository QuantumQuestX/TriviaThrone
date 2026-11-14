import {registerHooks} from "node:module";

const browserModuleAliases = new Map(
    ["api", "auth", "socket"].map(name => [
        `/js/root/${name}.js`,
        new URL(`../../main/resources/static/js/root/${name}.js`, import.meta.url).href
    ])
);

registerHooks({
    resolve(specifier, context, nextResolve) {
        const url = browserModuleAliases.get(specifier);
        return url ? {url, shortCircuit: true} : nextResolve(specifier, context);
    }
});
