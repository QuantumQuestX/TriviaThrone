class BuzzerUI {
    constructor(wsUrl, token) {
        this.statusEl = document.getElementById("status");
        this.timerEl = document.getElementById("timer");
        this.button = document.getElementById("buzz-button");

        this.ws = new WebSocket(`${wsUrl}?token=${token}`);

        this.ws.onopen = () => this.setStatus("Connected to game");
        this.ws.onmessage = () => this.handleMessage(JSON.parse(msg.data));
        this.ws.onclose = () => this.setStatus("Disconnected from game");

        this.button.addEventListener("click", () => this.sendBuzz());
    }

    setStatus(text) {
        this.statusEl.textContent = text;
    }

    handleMessage(msg) {
        switch (msg.type) {
            case "GAME_START":
                this.setStatus("Game started! Get ready...");
                this.button.disabled = false;
                break;
            case "BUZZ_ACCEPTED":
                this.setStatus("You buzzed first!");
                this.button.disabled = true;
                break;
            case "BUZZ_REJECTED":
                this.setStatus("Too slow! Wait for next round.");
                this.button.disabled = true;
                break;
            case "RESET":
                this.setStatus("New round!");
                this.button.disabled = false;
                break;
            case "TIMER_UPDATE":
                this.timerEl.textContent = `Time: ${msg.time}`;
                break;
        }
    }

    sendBuzz() {
        this.ws.send(JSON.stringify({ type: "BUZZ" }));
        this.setStatus("Buzz sent!");
        this.button.disabled = true;
    }
}

const token = localStorage.getItem("token");
if (!token) {
    window.location.href = "/index.html";
} else {
    new BuzzerUI(`ws://${window.location.host}/game`, token);
}