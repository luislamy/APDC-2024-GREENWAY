document.addEventListener("DOMContentLoaded", () => {
    checkLoginStatus();
    getThreadDetails();
});

function getThreadDetails() {
    const communityID = localStorage.getItem("communityID");
    const threadID = localStorage.getItem("threadID");
    const authToken = localStorage.getItem("authToken");

    fetch(`https://greenway-be.nw.r.appspot.com/rest/communities/${communityID}/get/${threadID}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "authToken": authToken
        }
    })
    .then(response => response.json())
    .then(threadData => {
        displayThreadDetails(threadData);
        getThreadReplies();
    })
    .catch(error => console.error("Error fetching thread details:", error));
}

function displayThreadDetails(threadData) {
    const threadDetailsContainer = document.getElementById("threadDetails");
    const threadDetailsHTML = `
        <h2>${threadData.title}</h2>
        <p><strong>Started by:</strong> ${threadData.username}</p>
        <p><strong>Start Date:</strong> ${new Date(threadData.threadStartDate).toLocaleString()}</p>
        <p><strong>Replies:</strong> ${threadData.replies}</p>
        <p><strong>Tags:</strong> ${threadData.tags.join(", ")}</p>
        <p><strong>Last Reply by:</strong> ${threadData.lastReplyUsername} on ${new Date(threadData.lastReplyDate).toLocaleString()}</p>
    `;
    threadDetailsContainer.innerHTML = threadDetailsHTML;
}

function getThreadReplies() {
    const communityID = localStorage.getItem("communityID");
    const threadID = localStorage.getItem("threadID");
    const authToken = localStorage.getItem("authToken");

    fetch(`https://greenway-be.nw.r.appspot.com/rest/communities/${communityID}/${threadID}/replies`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "authToken": authToken
        }
    })
    .then(response => response.json())
    .then(replies => {
        displayThreadReplies(replies);
    })
    .catch(error => console.error("Error fetching thread replies:", error));
}

function displayThreadReplies(replies) {
    const repliesContainer = document.getElementById("repliesContainer");
    let repliesHTML = "";
    replies.forEach(reply => {
        repliesHTML += `
            <div class="reply">
                <p><strong>${reply.username}</strong> replied on ${new Date(reply.replyDate).toLocaleString()}:</p>
                <p>${reply.replyBody}</p>
                <p><strong>Likes:</strong> ${reply.likes} ${reply.likedByUser ? "(You liked this)" : ""}</p>
                ${reply.threadmark ? `<p><strong>Threadmark:</strong> ${reply.threadmark}</p>` : ""}
            </div>
        `;
    });
    repliesContainer.innerHTML = repliesHTML;
}

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
                window.location.href = '../index.html';
            }
        })
        .catch(error => {
            console.error('Error checking login status: ', error);
        });
    } else {
        window.location.href = '../index.html';
    }
}