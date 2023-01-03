const prices = document.querySelectorAll(".price");

if (prices.length > 0) {
    for (let i = 0; i < prices.length; i++) {
        let priceArr = prices[i].innerText.split(" ");

        if (priceArr.length == 1 && parseFloat(priceArr[0]) === parseInt(priceArr[0])) {
            prices[i].innerText = parseInt(priceArr[0]);
        } else if(priceArr.length == 2 && parseFloat(priceArr[1]) === parseInt(priceArr[1])) {
            prices[i].innerText = "Price: " + parseInt(priceArr[1]);
        } else if (priceArr.length == 3 && parseFloat(priceArr[2]) === parseInt(priceArr[2])) {
            prices[i].innerText = priceArr[0] + " " + priceArr[1] + " " + parseInt(priceArr[2]);
        }
    }
}