function checkLoginStatus() {
    var authToken = localStorage.getItem('authToken');
    if ( authToken != null ) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/login/check', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: authToken
        })
        .then(response => {
            if (response.ok) {
                window.location.href = '../dashboard/dashboard.html';
            }
        })
        .catch(error => {
            console.error('Error checking login status: ', error);
        });
    } else {
        document.getElementById('login').style.display = 'block';
        document.getElementById('register').style.display = 'block';
    }
}

window.onload = function() {
    checkLoginStatus();
};