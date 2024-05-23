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
                var token = JSON.parse(authToken);
                if ( token.role === 'USER' || token.role === 'EP' ) {
                    document.getElementById('userCenterBtn').style.display = 'block';
                    document.getElementById('adminCenterBtn').style.display = 'none';
                } else {
                    document.getElementById('userCenterBtn').style.display = 'none';
                    document.getElementById('adminCenterBtn').style.display = 'block';
                }
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