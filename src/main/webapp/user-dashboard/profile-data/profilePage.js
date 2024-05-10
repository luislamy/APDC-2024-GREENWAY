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
                if ( token.role === 'USER' ) {
                    document.getElementById('userCenterBtn').style.display = 'block';
                    document.getElementById('adminCenterBtn').style.display = 'none';
                } else {
                    document.getElementById('userCenterBtn').style.display = 'none';
                    document.getElementById('adminCenterBtn').style.display = 'block';
                }
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

function getUserProfile() {
    var authToken = localStorage.getItem('authToken');
    fetch('/rest/user/profile', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: authToken
    })
    .then(response => {
        if (!response.ok) {
            return response.text();
        }
        return response.json();
    })
    .then(data => {
        if ( typeof data === "string" ) {
            alert('Error fetching user: ' + data);
        } else {
            const user = data;
            const userListContainer = document.getElementById('userData');
            const userList = document.createElement('ul');
            for (const [key, value] of Object.entries(user)) {
                const listItem = document.createElement('li');
                if ( key !== 'photo' ) {
                    listItem.textContent = `${key}: ${value}`;
                } else {
                    const photo = document.createElement('img');
                    photo.src = user.photo;
                    photo.style.maxWidth = '200px';
                    listItem.appendChild(photo);
                }
                userList.appendChild(listItem);
            }
            userListContainer.appendChild(userList);
        }
    })
    .catch(error => {
        alert('Error fetching user: ' + error.message);
    });
}

window.onload = function() {
    checkLoginStatus();
    getUserProfile();
};