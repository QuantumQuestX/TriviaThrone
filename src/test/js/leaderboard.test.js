import assert from "node:assert/strict";
import {test} from "node:test";
import {renderLeaderboard} from "../../main/resources/static/js/page-game/leaderboard.js";

function createElement(tagName) {
    return {
        tagName,
        children: [],
        style: {},
        append(...elements) {
            this.children.push(...elements);
        }
    };
}

test("renders every player and score in leaderboard order", () => {
    const list = {
        children: [],
        replaceChildren() {
            this.children = [];
        },
        appendChild(element) {
            this.children.push(element);
        }
    };

    renderLeaderboard(list, [
        {username: "winner", score: 500},
        {username: "player", score: 200}
    ], createElement);

    assert.equal(list.children.length, 2);
    assert.deepEqual(list.children.map(row => row.children.map(child => child.textContent)), [
        ["#1", "winner", "500 points"],
        ["#2", "player", "200 points"]
    ]);
    assert.deepEqual(list.children.map(row => row.style.animationDelay), ["0ms", "420ms"]);
    assert.deepEqual(list.children[0].children.map(child => child.style.animationDelay), [
        "0ms",
        "100ms",
        "200ms"
    ]);
});
