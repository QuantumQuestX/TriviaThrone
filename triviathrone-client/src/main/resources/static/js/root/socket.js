import { getAccessToken, refreshAccessToken } from "./auth.js";

let stompClient = null;
let connectionPromise = null;
let connectedEndpoint = null;
let pendingEndpoint = null;
const subscriptions = new Map();

export async function connectSocket(endpoint = "/ws/lobby") {
    if (stompClient?.connected && connectedEndpoint === endpoint) {
        return stompClient;
    }

    if (connectionPromise) {
        if (pendingEndpoint !== endpoint) {
            throw new Error("A connection to a different WebSocket endpoint is already starting.");
        }

        return connectionPromise;
    }

    pendingEndpoint = endpoint;
    connectionPromise = connectWithRefresh(endpoint).finally(() => {
        connectionPromise = null;
        pendingEndpoint = null;
    });

    return connectionPromise;
}

async function connectWithRefresh(endpoint) {
    for (let attempt = 0; attempt < 2; attempt++) {
        const token = await getAccessToken();

        try {
            return await openConnection(endpoint, token);
        } catch (error) {
            if (attempt === 1) {
                throw error;
            }

            await refreshAccessToken();
        }
    }

    throw new Error("Could not establish the WebSocket connection.");
}

function openConnection(endpoint, token) {
    return new Promise((resolve, reject) => {
        const client = Stomp.over(new SockJS(endpoint));

        client.debug = () => {};
        client.connect(
            { Authorization: `Bearer ${token}` },
            () => {
                stompClient = client;
                connectedEndpoint = endpoint;
                resolve(client);
            },
            (error) => reject(error),
        );
    });
}

export function subscribe(topic, callback) {
    if (!stompClient?.connected) {
        throw new Error("WebSocket not connected.");
    }

    unsubscribe(topic);

    const subscription = stompClient.subscribe(topic, (message) => {
        try {
            callback(JSON.parse(message.body));
        } catch (error) {
            console.error(`Could not process WebSocket message for ${topic}.`, error);
        }
    });

    subscriptions.set(topic, subscription);

    return subscription;
}

/**
 * Subscribes to the authenticated user's private game-state queue.
 *
 * @param {Function} callback receives each parsed private game-state message
 * @returns {Object} the STOMP subscription
 */
export function subscribePrivateGameState(callback) {
    return subscribe("/user/queue/game.private", callback);
}

export function unsubscribe(topic) {
    const subscription = subscriptions.get(topic);

    if (subscription) {
        subscription.unsubscribe();
        subscriptions.delete(topic);
    }
}

export function send(destination, payload = {}) {
    if (!stompClient?.connected) {
        throw new Error("WebSocket not connected.");
    }

    stompClient.send(destination, {}, JSON.stringify(payload));
}

export function disconnect() {
    const client = stompClient;

    stompClient = null;
    connectedEndpoint = null;
    subscriptions.forEach((subscription) => subscription.unsubscribe());
    subscriptions.clear();

    if (client?.connected) {
        client.disconnect();
    }
}
