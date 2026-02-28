import { MediaController, MediaType } from "./media-controller.js";

export class QuestionManager {
    constructor() {
        this.categoryEl = document.getElementById("question-category");
        this.pointsEl = document.getElementById("question-points");
        this.textEl = document.getElementById("question-text");
        this.choicesContainer = document.getElementById("question-choices");

        this.mediaController = new MediaController();
    }

    showQuestion(question) {
        this.reset();

        this.categoryEl.textContent = question.category;
        this.pointsEl.textContent = question.points;
        this.textEl.textContent = question.prompt;

        if (question.mediaType && question.mediaType !== MediaType.UNDEFINED) {
            this.mediaController.setMedia(question.mediaType, question.mediaUrl);
        }

        if (question.multipleChoice && question.choices?.length) {
            this.renderChoices(question.choices);
        }
    }

    renderChoices(choices) {
        this.choicesContainer.classList.remove("hidden");
        this.choicesContainer.innerHTML = "";

        choices.slice(0, 4).forEach(choiceText => {
            const choiceEl = document.createElement("div");
            choiceEl.className = "question-choice closed";
            choiceEl.textContent = choiceText;
            this.choicesContainer.appendChild(choiceEl);
        });
    }

    revealCorrectAnswer(correctIndex) {
        const children = [...this.choicesContainer.children];

        children.forEach((child, index) => {
            if (index === correctIndex) {
                child.classList.add("correct");
            } else {
                child.classList.add("wrong");
            }
        });
    }

    reset() {
        this.mediaController.clearMedia();

        this.textEl.textContent = "";
        this.choicesContainer.innerHTML = "";
        this.choicesContainer.classList.add("hidden");
    }
}