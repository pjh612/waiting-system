import {EventSource} from 'https://esm.sh/eventsource@3.0.0-beta.0'

let myQueueRank = null;

const subscribe = () => {
    console.log("Subscribing to waiting service events...");
    const alarm = new EventSource(`/api/waiting/subscribe`, {
        fetch: (input, init) =>
            fetch(input, {
                headers: {
                    token: token
                },
            }),
    });

    alarm.addEventListener("TOKEN_UPDATE", (e) => {
        try {
            const parsedData = JSON.parse(e.data);
            token = parsedData.token;
            console.log("Token updated successfully");
        } catch (err) {
            console.error("Failed to parse token update:", err);
        }
    });

    alarm.addEventListener('MESSAGE', (e) => {
        try {
            const payload = JSON.parse(e.data);
            if (payload.redirectUrl) {
                window.location.href = `${payload.redirectUrl}?token=${payload.token}`;
                return;
            }
            if (payload.enteredCount) {
                const remaining = myQueueRank - payload.enteredCount;
                myQueueRank = remaining;
                document.getElementById("position").textContent = remaining > 0 ? remaining : 0;
            }

            if (myQueueRank == null && payload.position) {
                document.getElementById("position").textContent = payload.position;
                myQueueRank = payload.position;
            }
        } catch (error) {
            console.error("Failed to process message", error);
        }
    });
}

const updatePosition = () => {
    fetch('/api/waiting/position', {
        method: 'GET',
        headers: {
            "token": token, // 토큰 인증
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch position');
            }
            return response.json();
        })
        .then(data => {
            const positionElement = document.getElementById('position');
            if (data.position !== undefined) {
                positionElement.textContent = data.position;
                myQueueRank = data.position;
            }
        })
        .catch(error => {
            console.error('Error updating position:', error);
        });
};

updatePosition();
subscribe();


// setInterval(updatePosition, 5000);