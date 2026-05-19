<h2>UEF Dual-Task Counting App</h2>
<ul><li>Uses a large Whisper ASR model and Python postprocessing functions to automatically transcribe prerecorded/live audio of UEF dual-task and then extract outcomes. Android App coded in Kotlin and backend coded in Python. Communication between Python and Kotlin ends happens through TCP server on local computer. In future work, will need to update to run on cloud.</li>
<li>Postprocessing functions were derived based on patterns found through listening to counting trials or by examining patterns in transcription.</li>
<li>Upload prerecorded audio option was most tested. Live transcription works but occasionally skips last 1 or 2 numbers, will need to improve on this.</li>
<li>Could also improve app performance by using other Whisper versions or transcription models (e.g. Whisper large-v2, -v3, turbo, or other models).</li></ul>

<h2>Before running:</h2>
<h3>For Python backend:</h3>
<ul><li>pip install faster-whisper</li>
<li>pip install whisper-live</li></ul>

<h3>For Android device:</h3>
<ul><lI>Find IP address of the computer/server running Python and add it to network_security_config.xml in /res/xml/ folder in Android Studio</lI>
<li>Build and run app phone</li></ul>

<h2>While running:</h2>
<h3>For prerecorded audio:</h3>
1. Navigate to test page of app<br>
2. Run AppPrerecordedTranscription.py on computer/server for Python<br>
3. Input device/server IP address and participant ID in app input fields and select 'Upload Audio'<br>
4. Select audio file for upload<br>
5. Audio file will be sent to Python backend to be automatically transcribed and checked for outcomes<br>
6. Outcomes will be sent back to the phone and displayed on its screen<br>
7. Results will be added to a result.csv file in app's storage directory<br>

<h3>For live test:</h3>
1. Navigate to test page of app<br>
2. Input device/server IP address and participant ID in app input fields and select 'Live Test'<br>
3. Run AppLiveTranscriptionServer.py and AppLivePostprocessing.py on computer/server for Python<br>
4. When ready to begin UEF dual-task, press 'Begin Test' and speak into phone's microphone<br>
5. To end test, press 'Stop Test'
6. Audio sent to Python backend during test will be automatically transcribed and checked for outcomes<br>
7. Outcomes will be sent back to the phone and displayed on its screen<br>
8. Results will be added to a result.csv file in app's storage directory
