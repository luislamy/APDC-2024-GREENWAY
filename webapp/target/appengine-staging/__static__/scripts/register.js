document.addEventListener('DOMContentLoaded', function() {
    var registrationForm = document.getElementById('registrationForm');
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

    registrationForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var password = document.getElementById('password').value;
        var confirmation = document.getElementById('confirmation').value;

        if (password != confirmation) {
            alert('Password and Confirmation password do not match. Please try again.');
            return;
        }

        var formData = new FormData(registrationForm);
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
        registerUser(JSON.stringify(jsonData));
    });

    function registerUser(jsonData) {
        fetch('/rest/register/user', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if ( response.ok ) {
                console.log('User registered.')
                window.location.href = '../index.html';
            } else {
                const errorMessage = await response.text();
                alert('Fetch error: ' + errorMessage);
            }
        });
    }
});