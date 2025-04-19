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