document.addEventListener('DOMContentLoaded', function() {
        document.querySelector('#addCommunity').onsubmit = () => {
            const name = document.getElementById("communityName").value;
            const nickname = document.getElementById("nickname").value;
            const description = document.getElementById("description").value;
            const bodyData = JSON.stringify({
                "name": name,
                "nickname": nickname,
                "description": description
            });
            fetch('https://apdc-grupo-7.oa.r.appspot.com/rest/communities/create', {
                method: "POST",
                headers: {
                    "Content-type": "application/json",
                    "authToken": localStorage.getItem("authToken"),
                },
                body: bodyData
            })
                .then(async response => {
                    if (await response.ok) {
                        window.location.href = "communities.html";
                    } else {
                        window.location.href = "communities.html";
                    }
                })
                .catch(err => console.log(err));
        }
    }
)