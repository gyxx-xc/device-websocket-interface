var ws;
var wsout;
if ("WebSocket" in window) {
  ws = new WebSocket("ws://###HOST###/in"); // use ###HOST### as this computer
  wsout = new WebSocket("ws://###HOST###/out"); // use ###HOST### as this computer
} else {
  alert("WebSocket is not support!");
}

function send(a) {
ws.send(a);
}
