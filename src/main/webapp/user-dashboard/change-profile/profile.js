document.addEventListener('DOMContentLoaded', function() {
    var userDataForm = document.getElementById('userDataForm');
    var photoForm = document.getElementById('photo');
    let photoRead = false;

    photoForm.addEventListener('change', function(event) {
        const preview = document.querySelector("img");
        const file = event.target.files[0];
        const maxSize = 1024 * 1024;
    
        if ( file.size > maxSize ) {
            alert('File size exceeds 1MB limit. Choose a smaller file.');
            event.target.value = '';
        }
        var reader = new FileReader();
        reader.addEventListener(
            "load",
            () => {
              preview.src = reader.result;
              photoRead = true;
            },
            false,
        );
        reader.readAsDataURL(file);
    });

    userDataForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userDataForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            if ( key === "photo" ) {
                if ( photoRead ) {
                    const preview = document.querySelector("img");
                    jsonData[key] = preview.src;
                }
            } else {
                jsonData[key] = value;
            }
        });
        var authToken = localStorage.getItem('authToken')
        if ( authToken == null ) {
            alert('Auth Token not found.');
            window.location.href = 'login.html';
            return;
        }
        var token = JSON.parse(authToken);
        jsonData['username'] = token.username;
        jsonData['token'] = token;
        changeUserData(JSON.stringify(jsonData));
    });

    function changeUserData(jsonData) {
        fetch('/rest/user/change/data', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                alert('Change user data: ' + message);
                window.location.href = 'user.html';
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
                window.location.href = 'index.html';
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