const inputs = document.querySelectorAll(".form-check-input");
const button = document.getElementById("reserve-btn");
const totalAmount = document.getElementById("total-amount");

if(inputs && button) {
    for (let index = 0; index < inputs.length; index++) {
        inputs[index].addEventListener('change', (e) => {
            let checkedCount = 0;
            let newTotalAmount = 0;

            for (let index2 = 0; index2 < inputs.length; index2++) {
                if(inputs[index2].checked) {
                    checkedCount++;
                    newTotalAmount += parseFloat(inputs[index2].parentElement.parentElement.parentElement.dataset.price);
                }
            }

            if(checkedCount > 0) {
                totalAmount.style.visibility = "visible";
                button.style.visibility = "visible";
            } else {
                totalAmount.style.visibility = "hidden";
                button.style.visibility = "hidden";
            }

            totalAmount.dataset.amount = newTotalAmount
            totalAmount.innerText = "Amount due: " + newTotalAmount
        });
    }
}