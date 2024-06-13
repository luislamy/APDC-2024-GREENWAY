document.addEventListener('DOMContentLoaded', function() {
    var passwordForm = document.getElementById('passwordForm');

    passwordForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var newPassword = document.getElementById('newPassword').value;
        var confirmation = document.getElementById('confirmation').value;

        if (newPassword !== confirmation) {
            alert('New password and confirmation do not match. Please try again.');
            return;
        }

        var formData = new FormData(passwordForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        var authToken = localStorage.getItem('authToken');
        if ( authToken == null ) {
            alert('Auth Token not found. Login again.');
            window.location.href = '../../login/login.html';
            return;
        }
        var token = JSON.parse(authToken);
        jsonData['token'] = token;
        changePassword(JSON.stringify(jsonData));
    });

    function changePassword(jsonData) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/user/change/password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                window.location.href = './../index.html';
            } else {
                const errorMessage = await response.text();
                alert('Fetch error: ' + errorMessage);
            }
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
                window.location.href = '../../index.html';
            }
        })
        .catch(error => {
            console.error('Error checking login status: ', error);
        });
    } else {
        window.location.href = '../../index.html';
    }
}

window.onload = function() {
    checkLoginStatus();
};