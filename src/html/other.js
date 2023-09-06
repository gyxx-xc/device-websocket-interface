html = document.querySelector('html');

if("ontouchstart" in window) {
  document.addEventListener("touchstart", getTouchPos);
  document.addEventListener("touchmove", getTouchPos);
} else {
  document.addEventListener("mousedown", getMousePos);
}

var a = false;
function getTouchPos(event) {
  var e = event || window.event;
  transfer(e.touches[0])
}
function getMousePos(event) {
  var e = event || window.event;
  transfer(e);
}

function transfer(p){
  if (!a) {
    html.requestFullscreen();
    a = true;
  }

  height = document.documentElement.scrollHeight;
  var a = p.pageY/height;
  if (a < 0.1) a = 0.1;
  if (a > 0.9) a = 0.9;
  document.getElementById("pos").style.height = a*100 + "%";

  a = (a - 0.1) / 0.8;
  a = (a * ((1<<16) - 1)) | 0;
  var m = "";
  m += String.fromCharCode(1);
  m += String.fromCharCode((a>>8)&255) + String.fromCharCode(a&255);
  m = btoa(m);
  document.getElementById("debug").textContent = m;
  send(m);
}

wsout.onmessage = function (e) {
  console.log(e.data);
}