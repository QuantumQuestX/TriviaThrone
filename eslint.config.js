export default [
    {
        files: [
            "triviathrone-client/src/main/resources/static/js/**/*.js",
            "triviathrone-games/jeopardy/src/main/resources/ui/js/**/*.js",
        ],
        rules: {
            curly: ["error", "all"],
            "padding-line-between-statements": [
                "error",
                { blankLine: "always", prev: "*", next: "return" },
                { blankLine: "always", prev: ["const", "let", "var"], next: "*" },
                { blankLine: "always", prev: "*", next: ["const", "let", "var"] },
                { blankLine: "any", prev: ["const", "let", "var"], next: ["const", "let", "var"] },
                { blankLine: "always", prev: "*", next: ["if", "for", "while", "switch", "try"] },
                { blankLine: "always", prev: ["if", "for", "while", "switch", "try"], next: "*" },
            ],
        },
    },
];
