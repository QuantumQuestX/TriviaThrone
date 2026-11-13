window.addEventListener("DOMContentLoaded", () => {
    const adminAttribute = "data-admin";

    const adminElements = document.querySelectorAll(`[${adminAttribute}]`);

    adminElements.forEach(el => 
        el?.remove()
    );
});