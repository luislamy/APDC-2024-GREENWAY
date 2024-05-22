document.addEventListener('DOMContentLoaded', function() {
    var userDeleteForm = document.getElementById('userDeleteForm');

    userDeleteForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userDeleteForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        var authToken = localStorage.getItem('authToken')
        if ( authToken == null ) {
            alert('Auth Token not found.');
            window.location.href = '../../login/login.html';
            return;
        }
        var token = JSON.parse(authToken);
        jsonData['token'] = token;
        changeUserRemove(JSON.stringify(jsonData));
    });

    function changeUserRemove(jsonData) {
        fetch('/rest/user/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                console.log('Remove user: ', message);
                window.location.href = '../../index.html';
            } else {
                const errorMessage = await response.text();
                alert('Fetch error: ' + errorMessage);
            }
        }).catch(error => {
            alert('Remove user:' + error);
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