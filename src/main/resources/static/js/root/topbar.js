function initTopbarUserByToken(parsedToken) {
    const username = parsedToken?.sub || "Unkown";
    document.getElementById("username-display").textContent = username;
}