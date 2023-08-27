html = document.querySelector('html');
var a = false;
window.onload = function initControl() {
  var StartEvt;
  var moveEvt;
  var endEvt;
  if("ontouchstart" in window) {
    document.addEventListener("touchstart", getTouchPos);
    document.addEventListener("touchmove", getTouchPos);
  } else {
    document.addEventListener("mousedown", getMousePos);
  }
}
function getTouchPos(event) {
  var e = event || window.event;
  document.getElementById("q").textContent =
    (e.touches[0].pageY + ' ' + e.touches[0].pageX);
  ws.send(e.touches[0].pageY + ' ' + e.touches[0].pageX);
    if (!a) {
    html.requestFullscreen();
    a = true;
    }

}
function getMousePos(event) {
  var e = event || window.event;
  document.getElementById("q").textContent =
    (e.pageY + ' ' + e.pageX);
  ws.send(e.pageY + ' ' + e.pageX);
  if (!a) {
  html.requestFullscreen();
  a = true;
  }
}
