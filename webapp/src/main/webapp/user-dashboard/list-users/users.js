document.addEventListener('DOMContentLoaded', function() {
    var authToken = localStorage.getItem('authToken');
    if ( authToken == null ) {
        alert('Auth Token not found. Login again.');
        window.location.href = '../../login/login.html';
        return;
    }
    fetch('https://greenway-be.nw.r.appspot.com/rest/list/users', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: authToken
    })
    .then(response => response.json())
    .then(data => {
        const userListContainer = document.getElementById('userList');
        const userList = document.createElement('ul');
        data.forEach(user => {
            const listItem = document.createElement('li');
            listItem.textContent = `Username: ${user.username}, Email: ${user.email}, Name: ${user.name}`;
            userList.appendChild(listItem);
        });
        userListContainer.appendChild(userList);
    })
    .catch(error => {
        alert('Error fetching users: ' + error.message);
    });
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