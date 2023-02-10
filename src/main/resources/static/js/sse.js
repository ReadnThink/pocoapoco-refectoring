let userId = document.getElementById("myName").innerText;

console.log(userId);

if (userId.length > 0) {
    const eventSource = new EventSource("/sse" + "?userId=" + userId)
    eventSource.addEventListener("alarm", function (event) {
        let message = event.data;
        Swal.fire({
            // toast:true,
            position: 'top-end',
            icon: 'success',
            title: message,
            showConfirmButton: false,
            timer: 1500
        });

    });
}

if (userId.length > 0) {
    const eventSource = new EventSource("/sse/for")
    eventSource.onmessage = event => {
        let message = event.data;

        $("#randomCnt").empty();
        $("#randomCnt").append('현재 대기중인 인원 :' + message);
    }
}



