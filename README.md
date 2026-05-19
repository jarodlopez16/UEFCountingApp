<h2>UEF Dual-Task Counting App</h2>

Uses a large Whisper ASR model and Python postprocessing functions to automatically transcribe prerecorded/live audio of UEF dual-task and then extract outcomes. Android App coded in Kotlin and backend coded in Python. Communication between Python and Kotlin ends happens through TCP server on local computer. In future work, will need to update to run on cloud. <br><br>
Postprocessing functions were derived based on patterns found through listening to counting trials or by examining patterns in transcription.<br><br>
Upload prerecorded audio option was most tested. Live transcription works but occasionally skips last 1 or 2 numbers, will need to improve on this.<br><br>
Could also improve app performance by using other Whisper versions or transcription models (e.g. Whisper large-v2, -v3, turbo, or other models).

<h2>Before running:</h2>
<h3>For Python backend:</h3>
pip install faster-whisper<br>
pip install whisper-live<br>

<h3>For Android device:</h3>
Find IP address of the device/server running Python and add it to network_security_config.xml in /res/xml/ folder<br>
Build and run app phone<br>

<h2>When running:</h2>
<h3>For prerecorded audio:</h3>
1. Navigate to test page of app<br>
2. Run AppPrerecordedTranscription.py on device/server for Python<br>
3. Input device/server IP address and participant ID in app input fields and select 'Upload Audio'<br>
4. Select audio file for upload<br>
5. Audio file will be sent to Python backend to be automatically transcribed and checked for outcomes<br>
6. Outcomes will be sent back to the phone and displayed on its screen<br>

For live test:<br>
