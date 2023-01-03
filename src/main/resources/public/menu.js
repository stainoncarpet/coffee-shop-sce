const consumables = document.querySelectorAll("div.consumable");
const totalAmount = document.getElementById("total-amount");
const takeoutControls = document.getElementById("takeout-controls");

if(consumables.length > 0 && !!totalAmount) {
    for (let index = 0; index < consumables.length; index++) {
        const consumable = consumables[index];
        consumable.querySelector(".consumable-counter").addEventListener("change", () => {
                let newTotalAmount = 0;

                for (let j = 0; j < consumables.length; j++) {
                    const consumable = consumables[j];
                    newTotalAmount += parseFloat(consumable.dataset.price) * (parseFloat(consumable.querySelector(".consumable-counter").value))
                
                    if(newTotalAmount === 0) {
                        takeoutControls.style = "visibility:hidden;";
                    } else {
                        takeoutControls.style = "visibility:visible;";
                    }
                }

                totalAmount.dataset.amount = newTotalAmount
                totalAmount.innerText = "Amount due: " + newTotalAmount
        });
    }
}

