export class Round {
    constructor(name) {
        this.name = name;
        this.categoryCount = 1;
        this.questionCount = 1;
        this.board = null;
        this.categories = [{ name: "Category 1" }];
    }
}