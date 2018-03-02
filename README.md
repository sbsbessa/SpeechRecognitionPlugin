SpeechRecognitionPlugin
=======================

W3C Web Speech API - Speech Recognition plugin for PhoneGap/Android with audio file recording


Basic example is working on android
```
<script type="text/javascript">
var recognition;
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    recognition = new SpeechRecognition();
    recognition.onresult = function(event) {
        if (event.results.length > 0) {
            q.value = event.results[0][0].transcript;
            q.form.submit();
        }
    }
}
</script>
<form action="http://www.example.com/search">
    <input type="search" id="q" name="q" size=60>
    <input type="button" value="Click to Speak" onclick="recognition.start()">
</form>
```

Example from section 6.1 Speech Recognition Examples of the W3C page
(https://dvcs.w3.org/hg/speech-api/raw-file/tip/speechapi.html#examples)

To install the plugin use 

```
cordova plugin add https://github.com/sbsbessa/SpeechRecognitionPlugin
```
