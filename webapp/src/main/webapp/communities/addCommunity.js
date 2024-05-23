document.addEventListener('DOMContentLoaded', function() {
        document.querySelector('#addCommunity').onsubmit = () => {
            const communityID = document.getElementById("communityID").value;
            const name = document.getElementById("name").value;
            const description = document.getElementById("description").value;
            const bodyData = JSON.stringify({
                "communityID": communityID,
                "name": name,
                "description": description,
                "isLocked": false
            });
            fetch('https://apdc-grupo-7.oa.r.appspot.com/rest/communities/create', {
                method: "POST",
                headers: {
                    "Content-type": "application/json",
                    "authToken": localStorage.getItem("authToken")
                },
                body: bodyData
            })
            .then(async response => {
                if (response.ok) {
                    window.location.href = "./communities.html";
                } else {
                    window.location.href = "./communities.html";
                }
            });
        }
    }
)