document.addEventListener('DOMContentLoaded', function() {
    var userRoleForm = document.getElementById('userRoleForm');

    userRoleForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userRoleForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        var authToken = localStorage.getItem('authToken')
        if ( authToken == null ) {
            alert('Auth Token not found.');
            window.location.href = 'login.html';
            return;
        }
        var token = JSON.parse(authToken);
        jsonData['token'] = token;
        changeUserRole(JSON.stringify(jsonData));
    });

    function changeUserRole(jsonData) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/user/change/role', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                console.log('Change user role: ', message);
                window.location.href = '../admin.html';
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