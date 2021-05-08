<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>First Try</title>
</head>

<body>
    <label for="">Username</label>
    <br>
    <textarea id="our_username" cols="15" rows="5"></textarea>
    <br>
    <label for="">Room ID</label>
    <br>
    <textarea id="RoomID" cols="30" rows="10"></textarea>
    <button id="create">Create</button>
    <button id="join">Join</button>

    <pre id="messages"></pre>

    <div id="vid_div">
        <label>Your vid</label>
        <video id="yourvid" autoplay></video>
        <br>
    </div>
</body>
<script src="./js/client.js"></script>

</html>