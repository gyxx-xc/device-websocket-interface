var ws;
var wsout;
if ("WebSocket" in window) {
  // use ###HOST### as this computer
  // and make sure not use this word for other uses
  ws = new WebSocket("ws://###HOST###/in");
  wsout = new WebSocket("ws://###HOST###/out");
} else {
  alert("WebSocket is not support!");
}

function send(a) {
  ws.send(a);
}
