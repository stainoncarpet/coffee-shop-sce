const redirect = () => {
    setTimeout(() => {
        console.log("redirect");
        window.location.href = "/";
    }, 4000);
}

redirect();