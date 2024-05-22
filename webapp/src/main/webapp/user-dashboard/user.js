document.addEventListener('DOMContentLoaded', function() {
    var tokenButton = document.getElementById('tokenButton');
    var deleteButton = document.getElementById('deleteButton');
    var confirmDeleteButton = document.getElementById('confirmDeleteButton');


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

    deleteButton.addEventListener('click', function(event) {
        event.preventDefault();
        document.getElementById('confirmDeleteButton').style.display = 'block';
    });

    confirmDeleteButton.addEventListener('click', function(event) {
        event.preventDefault();

        var authToken = localStorage.getItem('authToken');
        if ( authToken == null ) {
            alert('Auth Token not found.');
            window.location.href = '../login/login.html';
            return;
        }
        var token = JSON.parse(authToken);
        var username = token.username;
        var jsonData = {};
        jsonData['username'] = username;
        jsonData['token'] = token;
        deleteUser(JSON.stringify(jsonData));
    });

    function deleteUser(jsonData) {
        fetch('/rest/user/remove', {
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
            }
        })
        .catch(error => {
            alert('User deletion error:' + error.message);
        });
    }
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