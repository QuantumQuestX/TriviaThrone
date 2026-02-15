import { Board } from "./board.js";
import { ResourceManager } from "./ResourceManager.js";
import { Upload} from "./upload.js";
import { ResourcePanel } from "./resource-panel.js";
import { QuestionEditor } from "./question-editor.js";
import { StorageManager } from "./utils.js";
import { token } from "./../root/token.js";

window.addEventListener('DOMContentLoaded', () => {
    /*
    CORE System
    */
    const rm = new ResourceManager();
    const storage = new StorageManager("editor-draft");
    const uploader = new Upload(rm, token);

    let game = loadOrCreateGame();
    let currentRoundId = null;

    if (game.rounds.length > 0) {
        currentRoundId = game.rounds[0].id;
    }

    /*
    UI Components
    */
    const resourcePanel = new ResourcePanel(
        rm,
        document.querySelector('.resource-panel')
    );

    const board = new Board(
        document.getElementById("board-container"),
        (roundId, catIdx, valueIdx) => {
            openQuestionEditor(roundId, catIdx, valueIdx);
        }
    );

    const questionEditor = new QuestionEditor(
        document.querySelector('.question-editor'),
        rm,
        (action, payload) => {
            if (action === 'save') {
                handleSaveQuestion(payload);
            }
        }
    );

    /*
    Game Model
    */

    function createEmptyGame() {
        return {
            id: crypto.randomUUID(),
            title: "",
            created: Date.now(),
            rounds: []
        };
    }
    
    function loadOrCreateGame() {
        const saved = storage.load();
        return saved || createEmptyGame();
    }

    function saveGame() {
        storage.save(game);
    }

    function getCurrentRound() {
        return game.rounds.find(r => r.id === currentRoundId);
    }

    /*
    Board interactions
    */

    function openQuestionEditor(roundId, categoryIdx, valueIdx) {
        const round = game.rounds.find(r => r.id === roundId);
        if (!round) return;

        const category = round.categories[categoryIdx];
        if (!category) return;

        const question = category.questions[valueIdx] || null;
        
        questionEditor.open(
            round,
            category,
            question,
            { roundId, categoryIdx, valueIdx }
        );
    }

    function handleSaveQuestion( { meta, question } ) {
        const round = game.rounds.find(r => r.id === meta.roundId);
        if (!round) return;

        const category = round.categories[meta.categoryIdx];
        if (!category) return;

        category.questions[meta.valueIdx] = question;

        saveGame();
        renderBoard();
    }

    function renderBoard() {
        board.render(getCurrentRound());
    }

    /*
    Round management
    */
    const roundSelect = document.getElementById('round-select');
    const addRoundBtn = document.getElementById('add-round-btn');

    function refreshRoundSelect() {
     roundSelect.innerHTML = '<option value="">Select or Create Round</option>';

     game.rounds.forEach(r => {
        const option = document.createElement('option');
        option.value = r.id;
        option.textContent = r.title;
        roundSelect.appendChild(option);
     });

     if (currentRoundId) {
         roundSelect.value = currentRoundId;
     }
    }

    roundSelect.addEventListener('change', () => {
        currentRoundId = roundSelect.value || null;
        renderBoard();
    });

    addRoundBtn.addEventListener('click', () => {
        const newRound = {
            id: crypto.randomUUID(),
            title: `Round ${game.rounds.length + 1}`,
            categories: createDefaultCategories()
        };

        game.rounds.push(newRound);
        currentRoundId = newRound.id;
    
        saveGame();
        refreshRoundSelect();
        renderBoard();
    });

    function createDefaultCategories(count = 5) {
        const categories = [];
        for (let i = 0; i < count; i++) {
            categories.push(
                {
                    id: crypto.randomUUID(),
                    title: `Category ${i + 1}`,
                    questions: []
                }
            );
        }
        return categories;
    }

    /*
    Title handling
    */
    const titleInput = document.getElementById('game-title');
    titleInput.value = game.title || "";

    titleInput.addEventListener('input', () => {
        game.title = titleInput.value.trim();
        saveGame();
    });

    /*
    Draf buttons
    */
    document.getElementById('save-draft-btn').addEventListener('click', () => {
        saveGame();
        alert("Draft saved!");
    });

    document.getElementById('clear-draft-btn').addEventListener('click', () => {
        if (!confirm("Clear current draft?")) return;

        storage.clear();
        game = createEmptyGame();
        currentRoundId = null;
        titleInput.value = "";

        refreshRoundSelect();
        renderBoard();
    });

    /*
    Upload handling
    */
    document.getElementById('upload-btn').addEventListener('click', async() => {
        try {
            if (!game.title.trim()) {
                alert("Game must have a title.");
                return;
            }

            if (game.rounds.length === 0) {
                alert("Add at least one round.");
                return;
            }

            await uploader.uploadGame(game);
            alert("Upload successful!");
        } catch (err) {
            console.error(err);
            alert("Upload failed: " + err.message);
        }
    });

    /*
    initial
    */
   refreshRoundSelect();
   renderBoard();

   console.log("Editor initialised.");

    const onClickToggle = (panelId) => {
        const panel = document.getElementById(panelId);
        if (panel) {
            panel.classList.toggle('collapsed');
        }
    };

   document.getElementById('left-panel-fold-btn').addEventListener('click', () => onClickToggle('left-panel'));
   document.getElementById('right-panel-fold-btn').addEventListener('click', () => onClickToggle('right-panel'));
});
