document.addEventListener('DOMContentLoaded', (event) => {
    console.log("DOM LOADED IN");
});

const toastCloseButton = document.querySelector("#toast button");
if(toastCloseButton) {
    toastCloseButton.addEventListener("click", () => {
        const toast = document.querySelector("#toast");
        toast.style.display="none";
    })
}