let stompClient = null;
const subscriptions = new Map();

export async function connectSocket(endpoint = '/ws/lobby', token = null) {
    return new Promise((resolve, reject) => {
        if (stompClient && stompClient.connected) {
            resolve(stompClient);
            return;
        }

        const socket = new SockJS(endpoint);
        stompClient = Stomp.over(socket);

        stompClient.connect(
            { "Authorization": "Bearer " + token },
            () => {
                console.log("WebSocket connected:", endpoint);
                resolve(stompClient);
            },
            (err) => {
                console.error("WebSocket connection failed:", err);
                reject(err);
            }
        );
    });
}

export function subscribe(topic, callback) {
    if (!stompClient || !stompClient.connected) {
        throw new Error("WebSocket not connected");
    }

    if (subscriptions.has(topic)) {
        subscriptions.get(topic).unsubscribe();
    }

    const sub = stompClient.subscribe(topic, (message) => {
        callback(JSON.parse(message.body))
    });

    subscriptions.set(topic, sub);
    return sub;
}

export function unsubscribe(topic) {
    const sub = subscriptions.get(topic);
    if (sub) {
        sub.unsubscribe();
        subscriptions.delete(topic);
    }
}

export function send(destination, payload = {}) {
    if (!stompClient || !stompClient.connected) {
        throw new Error("WebSocket not connected");
    }
    stompClient.send(destination, {}, JSON.stringify(payload));
}

export function disconnect() {
    if (stompClient && stompClient.connected) {
        subscriptions.forEach((sub) => sub.unsubscribe());
        subscriptions.clear();
        stompClient.disconnect(() => {
            console.log("WebSocket disconnected");
        });
    }
}