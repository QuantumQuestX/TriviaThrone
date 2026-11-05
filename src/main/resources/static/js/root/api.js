import {
    AuthenticationError,
    getAccessToken,
    isAccessTokenNearExpiry,
    redirectToLogin,
    refreshAccessToken
} from "./auth.js";

export async function apiFetch(resource, options = {}) {
    let token = await getAccessToken();
    let response = await sendAuthenticatedRequest(resource, options, token);

    if (response.status !== 401) {
        return response;
    }

    const currentToken = await getAccessToken();
    if (currentToken !== token) {
        token = currentToken;
        response = await sendAuthenticatedRequest(resource, options, token);
        if (response.status !== 401) return response;
    }

    if (!isAccessTokenNearExpiry()) {
        return response;
    }

    try {
        token = await refreshAccessToken();
    } catch (error) {
        if (error instanceof AuthenticationError) {
            redirectToLogin();
        }
        throw error;
    }

    response = await sendAuthenticatedRequest(resource, options, token);
    return response;
}

function sendAuthenticatedRequest(resource, options, token) {
    const headers = new Headers(options.headers);
    headers.set("Authorization", `Bearer ${token}`);

    return fetch(resource, {
        ...options,
        headers,
        credentials: options.credentials ?? "same-origin"
    });
}
