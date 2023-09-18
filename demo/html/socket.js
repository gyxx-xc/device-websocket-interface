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
// you can use blobs if you want
wsout.binaryType = "arraybuffer";
wsout.onmessage = function(e){
  view = new Uint8Array(e.data);
  document.querySelector("p").textContent = view[0];
}

function change(){
message = new Int8Array(1);
message[0] = document.getElementsByTagName("input")[0].value;
ws.send(message);
}
