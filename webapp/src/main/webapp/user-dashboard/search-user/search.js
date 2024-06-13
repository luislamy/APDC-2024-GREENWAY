document.addEventListener('DOMContentLoaded', function() {
    var userSearchForm = document.getElementById('userSearchForm');

    userSearchForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userSearchForm);
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
        searchUser(JSON.stringify(jsonData));
    });

    function searchUser(jsonData) {
        fetch('https://greenway-be.nw.r.appspot.com/rest/user/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
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
                const listItem = document.createElement('li');
                var authToken = localStorage.getItem('authToken');
                var token = JSON.parse(authToken);
                if ( token.role === 'USER' ) {
                    listItem.textContent = `Username: ${user.username}, Email: ${user.email}, Name: ${user.name}, Photo: `;
                } else {
                    listItem.textContent = `Username: ${user.username}, Email: ${user.email}, Name: ${user.name}, Phone: ${user.phone}, Profile: ${user.profile}, 
                                        Work: ${user.work}, Workplace: ${user.workplace}, Address: ${user.address}, Postal Code: ${user.postalcode}, 
                                        Fiscal: ${user.fiscal}, Role: ${user.role}, State: ${user.state}, User Creation Time: ${user.userCreationTime}, 
                                        Photo: `;
                }
                if (user.photo) {
                    const photo = document.createElement('img');
                    photo.src = user.photo;
                    photo.style.maxWidth = '100px';
                    listItem.appendChild(photo);
                }
                userList.appendChild(listItem);
                userListContainer.appendChild(userList);
            }
        })
        .catch(error => {
            alert('Error fetching user: ' + error.message);
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