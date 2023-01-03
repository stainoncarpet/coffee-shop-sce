const freeCoffees = document.getElementById("freeCoffeesRange");
const counter = document.getElementById("cups-count");
const output = document.getElementById("freeCoffees");

if(freeCoffees) {
    freeCoffees.addEventListener("change", (e) => {
        console.log("range change ", e.target.value);
        counter.innerText = e.target.value;
        output.value = e.target.value
    })
}