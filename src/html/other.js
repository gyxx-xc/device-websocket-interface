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
    var m = "";
    m += String.fromCharCode(1);
    var a = p.pageY/height*(1<<16);
    a = a | 0
    m += String.fromCharCode(a>>8);
    m += String.fromCharCode(a&255);
    m = btoa(m)
      document.getElementById("q").textContent =
        m;
  send(m);
}
