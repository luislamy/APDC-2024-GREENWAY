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
                document.getElementById('loginBtn').style.display = 'none';
                document.getElementById('registerBtn').style.display = 'none';
                document.getElementById('logoutBtn').style.display = 'block';
                if ( token.role === 'USER' ) {
                    document.getElementById('userCenterBtn').style.display = 'block';
                    document.getElementById('adminCenterBtn').style.display = 'none';
                } else {
                    document.getElementById('userCenterBtn').style.display = 'none';
                    document.getElementById('adminCenterBtn').style.display = 'block';
                }
            } else {
                document.getElementById('loginBtn').style.display = 'block';
                document.getElementById('registerBtn').style.display = 'block';
                document.getElementById('logoutBtn').style.display = 'none';
                document.getElementById('userCenterBtn').style.display = 'none';
                document.getElementById('adminCenterBtn').style.display = 'none';
            }
        })
        .catch(error => {
            console.error('Error checking login status: ', error);
        });
    } else {
        document.getElementById('loginBtn').style.display = 'block';
        document.getElementById('registerBtn').style.display = 'block';
        document.getElementById('logoutBtn').style.display = 'none';
        document.getElementById('userCenterBtn').style.display = 'none';
        document.getElementById('adminCenterBtn').style.display = 'none';
    }
}

window.onload = function() {
    checkLoginStatus();
};