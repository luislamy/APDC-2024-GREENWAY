document.addEventListener('DOMContentLoaded', function() {
    var loginForm = document.getElementById('loginForm');

    loginForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(loginForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        loginUser(JSON.stringify(jsonData));
    });

    function loginUser(jsonData) {
        fetch('/rest/login/user', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const token = await response.json();
                localStorage.setItem('authToken', JSON.stringify(token));
                console.log('User logged in.')
                window.location.href = '../dashboard/dashboard.html';
            } else {
                const errorMessage = await response.text();
                alert('Fetch error: ' + errorMessage);
            }
        });
    }
});