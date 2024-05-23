document.addEventListener('DOMContentLoaded', function() {
        document.querySelector('#addCommunity').onsubmit = (event) => {
            event.preventDefault();
            const name = document.getElementById("communityName").value;
            const nickname = document.getElementById("nickname").value;
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
                        //window.location.href = "communities.html";
                        console.log("Success")
                    } else {
                        //window.location.href = "communities.html";
                        console.log("Failure")
                    }
                })
                .catch(err => console.log(err));
        }
    }
)