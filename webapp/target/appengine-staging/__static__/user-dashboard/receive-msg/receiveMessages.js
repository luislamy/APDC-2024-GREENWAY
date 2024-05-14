document.addEventListener('DOMContentLoaded', function() {
    var authToken = localStorage.getItem('authToken');
    if ( authToken == null ) {
        alert('Auth Token not found. Login again.');
        window.location.href = 'login.html';
        return;
    }
    fetch('/rest/message/receive', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: authToken
    })
    .then(response => response.json())
    .then(data => {
        const messageListContainer = document.getElementById('messageList');
        const messageList = document.createElement('ul');
        data.forEach(message => {
            const listItem = document.createElement('li');
            listItem.textContent = `Sender: ${message.sender}, Date: ${message.timeStamp}, Message: ${message.message}`;
            messageList.appendChild(listItem);
        });
        messageListContainer.appendChild(messageList);
    })
    .catch(error => {
        alert('Error fetching users: ' + error.message);
    });
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

window.onload = function() {
    checkLoginStatus();
};