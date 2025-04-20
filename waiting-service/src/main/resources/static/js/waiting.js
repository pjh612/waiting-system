import {EventSource} from 'https://esm.sh/eventsource@3.0.0-beta.0'

const subscribe = () => {
    const alarm = new EventSource(`/api/waiting/subscribe`,{
        fetch: (input, init) =>
            fetch(input, {
                headers: {
                    token: token
                },
            }),
    });

    alarm.addEventListener("TOKEN_UPDATE", (e) => {
        token = e.data;
        console.log("Token updated:", token);
    });

    alarm.addEventListener('MESSAGE', (e) => {
        try {
            const parse = JSON.parse(e.data);
            if (parse.redirectUrl) {
                window.location.href = `${parse.redirectUrl}?token=${parse.token}`;
            }
            if (parse.position) {
                document.getElementById("position").textContent = parse.position;
            }
        } catch (error) {
            console.error("Failed to process message", error);
        }
    });
}
subscribe();

const updatePosition = () => {
    fetch('/api/waiting/position', {
        method: 'GET',
        headers: {
            "token" : token, // 토큰 인증
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
            }
        })
        .catch(error => {
            console.error('Error updating position:', error);
        });
};

setInterval(updatePosition, 5000);