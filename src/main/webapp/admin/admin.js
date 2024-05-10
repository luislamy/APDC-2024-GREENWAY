document.addEventListener('DOMContentLoaded', function() {
    var tokenButton = document.getElementById('tokenButton');


    var authToken = localStorage.getItem('authToken');
    var token = JSON.parse(authToken);
    var username = token.username;
    var welcomeHeader = document.querySelector('h1');
    welcomeHeader.textContent = 'Welcome ' + username + '!';

    tokenButton.addEventListener('click', function(event) {
        event.preventDefault();
        var authToken = localStorage.getItem('authToken');
        if ( authToken != null ) {
            alert('Auth Token Data:\n' + authToken);
        } else {
            alert('Auth Token not found.');
        }
    });
});

function checkLoginStatus() {
    var authToken = localStorage.getItem('authToken');
    if ( authToken != null ) {
        fetch('/rest/login/check', {
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
                window.location.href = 'dashboard.html';
            }
        })
        .catch(error => {
            console.error('Error checking login status: ', error);
        });
    } else {
        window.location.href = 'index.html';
    }
}

window.onload = function() {
    checkLoginStatus();
};