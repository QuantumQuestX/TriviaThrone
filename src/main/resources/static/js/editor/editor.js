import { BoardController } from "./board-controller.js";
import { token } from "./../root/token.js";
import { GameController } from "./game-controller.js";

window.addEventListener('DOMContentLoaded', () => {
    console.log("Editor initialised.");

    const jeopardyCanvas = document.getElementById('jeopardy-canvas');
    const boardController = new BoardController(jeopardyCanvas);
    const gameController = new GameController(boardController);

    gameController.init();

});
