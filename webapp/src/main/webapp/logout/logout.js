document.addEventListener('DOMContentLoaded', function() {
    var logoutButton = document.getElementById('logoutButton');

    logoutButton.addEventListener('click', function(event) {
        event.preventDefault();
        var jsonData = localStorage.getItem('authToken');
        logoutUser(jsonData);
    });

    function logoutUser(jsonData) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/logout/user', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                localStorage.removeItem('authToken');
                window.location.href = '../index.html';
            } else {
                const errorMessage = await response.text();
                alert('Fetch error: ' + errorMessage);
                window.location.href = '../index.html';
            }
        })
        .catch(error => {
            alert('Logout error:' + error);
        });
    }
});

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
            } else {
                localStorage.removeItem('authToken');
                window.location.href = '../index.html';
            }
        })
        .catch(error => {
            console.error('Error checking login status: ', error);
        });
    } else {
        window.location.href = '../index.html';
    }
}

window.onload = function() {
    checkLoginStatus();
};