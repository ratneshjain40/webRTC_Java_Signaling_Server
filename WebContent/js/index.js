var Peer = require("simple-peer");
var ws = new WebSocket("ws://localhost:8080/webRTC/signal");
var stream = navigator.mediaDevices.getUserMedia({ video: true, audio: false });


var our_username;
var users = [];

// ------------------- JSON TEMPLATES -------------------
//
// var obj_to_server = {
//     "action": "action",
//     "room_id": "room_id",
//     "to_user": "to user",
//     "data": {
//         "type": "offer/answer",
//         "username": "ratnesh",
//         "sdp_data": "simple_peer_signal_data",
//         "info": "extra field"
//     }
// };

// var response_obj = {
//     "response" : "response",
//     "data": {
//         "type": "offer/answer/participants",
//         "username": "ratnesh",
//         "sdp_data": "simple_peer_signal_data",
//         "info": "extra field"
//     }
// }
// here if we get type -> participant, we need to create an initiator peer for it and send it back.

// var user = {
//     "user_name": "random name",
//     "initiate": "true",
//     "send_signal" : "flase",
//     "sdp_data" : "peer_signal_data",
//     "peer_obj" : ""
// }
// here sdp_data -> sdp data we get is of the other user
// here initiate -> decides if we need to create a inititor peer or client peer.
// here peer_obj -> ref to the peer object created to communicated with that user

function send_to_server(action, room_id, to_user = " ", data_type = " ", data_username = our_username, sdp_data = " ") {
    var obj = {
        "action": action,
        "room_id": room_id,
        "to_user": to_user,
        "data": {
            "type": data_type,
            "username": data_username,
            "sdp_data": sdp_data,
        }
    };
    console.log(obj);
    ws.send(JSON.stringify(obj));
}

function init_event_binders() {
    document.getElementById("create").addEventListener('click', function () {
        create_room();
    });

    document.getElementById("join").addEventListener('click', function () {
        join_room();
    });
}

function create_room() {
    console.log("button create is clicked ");
    room_id = document.getElementById("RoomID").value;
    our_username = document.getElementById("our_username").value;
    send_to_server("Create Room", room_id);
}

function join_room() {
    console.log("button join is clicked ");
    room_id = document.getElementById("RoomID").value;
    our_username = document.getElementById("our_username").value;
    send_to_server("Join Room", room_id);
}

//---------------------- websocket event listeners ----------------------

ws.onopen = () => {
    console.log("On Open ");
    init_event_binders();
}

ws.onmessage = function (msg) {
    var res = JSON.parse(msg.data);
    console.log("On msg = " + res.response);

    if (res.response == "room created") {
        init_self_stream();
    }

    if (res.response == "room joined") {
        init_self_stream();
        var res_data = JSON.parse(res.data);
        console.log(res_data);
        if (res_data.type == "participants") {
            var usernames = res_data.username;
            usernames.forEach((username) => {
                var user = {
                    "user_name": username,
                    "initiate": true,
                    "send_signal": false,
                    "sdp_data": " ",
                    "peer_obj": " "
                }
                create_peer(user);
            })
        }
    }
    // if you send and offer then you will recive an answer that you need to put in peer.signal(), so peer.signal will run for both initiator and reciver
    if (res.response == "connection data") {
        var res_data = JSON.parse(res.data);
        console.log(res_data);
        if (res_data.type == "offer") {
            var username = res_data.username;
            var user = {
                "user_name": username,
                "initiate": false,
                "send_signal": true,
                "sdp_data": res_data.sdp_data,
                "peer_obj": " "
            }
            create_peer(user);
        }
        if (res_data.type == "answer") {
            var username = res_data.username;
            // dont use foreach
            users.forEach((user) => {
                if (user.user_name == username) {
                    user.peer_obj.signal(res_data.sdp_data);
                }
            })
        }

    }
}

ws.onclose = (msg) => {
    console.log("On Close = " + msg);
};

function init_self_stream() {
    stream.then(function (stream) {
        show_video(stream);
    });
}

function create_peer(user) {

    users.push(user);

    stream.then(function (stream) {

        var peer = new Peer({
            initiator: user.initiate,
            trickle: false,
            stream: stream
        })

        peer.on('signal', function (data) {
            if (user.initiate) {
                send_to_server(action = "Send Data", room_id = room_id, to_user = user.user_name, data_type = "offer", data_username = our_username, sdp_data = JSON.stringify(data));
            } else {
                send_to_server(action = "Send Data", room_id = room_id, to_user = user.user_name, data_type = "answer", data_username = our_username, sdp_data = JSON.stringify(data));
            }
        });

        if (user.send_signal) {
            peer.signal(user.sdp_data);
            user.send_signal = false;
        }

        peer.on("stream", function (stream) {
            show_video(stream, user.user_name);
        });

        user.peer_obj = peer;
    })

}

function create_video_element(name) {
    var vid_div = document.getElementById("vid_div");
    var vid = document.createElement("video");
    vid.setAttribute("id", name);
    vid.setAttribute("autoplay","true");
    vid_div.appendChild(vid);
}

function show_video(stream, streamer = 'yourvid') {
    if (streamer != 'yourvid') {
        create_video_element(streamer);
    }
    const video = document.getElementById(streamer);;
    video.srcObject = stream;
    video.play();
};