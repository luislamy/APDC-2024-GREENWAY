document.addEventListener('DOMContentLoaded', function() {
    var conversationForm = document.getElementById('conversationForm');
    var messageForm = document.getElementById('messageForm');

    conversationForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(conversationForm);
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
        jsonData['sender'] = token.username;
        loadMessages(JSON.stringify(jsonData));
    });

    function loadMessages(jsonData) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/message/conversation', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(response => response.json())
        .then(data => {
            const messageListContainer = document.getElementById('messageList');
            messageListContainer.innerHTML = '';
            const messageList = document.createElement('ul');
            data.forEach(message => {
                const listItem = document.createElement('li');
                listItem.textContent = `Sender: ${message.sender}, Message: ${message.message}, Date: ${message.timeStamp}`;
                messageList.appendChild(listItem);
            });
            messageListContainer.appendChild(messageList);
        })
        .catch(error => {
            alert('Error fetching messages: ' + error.message);
        });
    }

    messageForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(messageForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        let receiver = document.getElementById('receiver').value;
        if ( receiver === '' ) {
            alert('Please choose who to message.');
        } else {
            var authToken = localStorage.getItem('authToken')
            if ( authToken == null ) {
                alert('Auth Token not found.');
                window.location.href = '../../login/login.html';
                return;
            }
            var token = JSON.parse(authToken);
            jsonData['token'] = token;
            jsonData['sender'] = token.username;
            jsonData['receiver'] = receiver;
            sendMessage(JSON.stringify(jsonData));
        }
    });

    function sendMessage(jsonData) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/message/user', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                var message = JSON.parse(jsonData);
                jsonData = {};
                var authToken = localStorage.getItem('authToken')
                if ( authToken == null ) {
                    alert('Auth Token not found.');
                    window.location.href = '../../login.html';
                    return;
                }
                var token = JSON.parse(authToken);
                jsonData['token'] = token;
                jsonData['sender'] = token.username;
                jsonData['receiver'] = message.receiver;
                loadMessages(JSON.stringify(jsonData));
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