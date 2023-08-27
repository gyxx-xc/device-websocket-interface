var ws;
if ("WebSocket" in window) {
  ws = new WebSocket("ws://###HOST###"); // use ###HOST### as this computer
} else {
  alert("WebSocket is not support!");
}

function send(a) {
ws.send(a);
}
