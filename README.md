UEF Dual-Task Counting App

Uses a large Whisper ASR model and Python postprocessing functions to automatically transcribe prerecorded/live audio of UEF dual-task and then extract outcomes. Android App coded in Kotlin and backend coded in Python. Communication between Python and Kotlin ends happens through TCP server on local computer. In future work, will need to update to run on cloud. <br><br>
Postprocessing functions were derived based on patterns found through listening to counting trials or by examining patterns in transcription.<br><br>
Upload prerecorded audio option was most tested. Live transcription works but occasionally skips last 1 or 2 numbers, will need to improve on this.<br><br>
Could also improve app performance by using other Whisper versions or transcription models (e.g. Whisper large-v2, -v3, turbo, or other models).

Before running:<br><br>
For Python backend:<br>
pip install faster-whisper<br>
pip install whisper-live<br><br>

For Android device:<br>
Find IP address of the device/server running Python and add it to network_security_config.xml in /res/xml/ folder
Build and run app phone<br><br>

When running:<br>
For prerecorded audio:<br>
