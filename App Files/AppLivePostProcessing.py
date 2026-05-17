import socket
import json
import AppPostProcess
import os
import datetime

# Run file while LiveTestActivity open on Android app

# Create a new socket on local computer with port specifically for live transcription postprocessing
server = socket.socket()
server.bind(("0.0.0.0", 9092))
# Listen for data
server.listen()

while True:
    # Create a file name to store transcription file
    output_dir = "ResultFiles"
    fileNum = 1
    now = datetime.datetime.now()
    formattedDateTime = now.strftime("%m%d%y")
    transcriptName = f"Transcript{formattedDateTime}({fileNum}).tsv"

    # Accept incoming data
    client, addr = server.accept()
    tsv_data = b""

    # Store tsv data and compile it
    while True:
        chunk = client.recv(4096)

        if not chunk:
            break

        tsv_data += chunk
    
    # Adjust transcription file name if file already exists
    tsv_path = os.path.join(output_dir, transcriptName)
    while os.path.exists(tsv_path):
        fileNum += 1
        transcriptName = f"Transcript{formattedDateTime}({fileNum}).tsv"
        tsv_path = os.path.join(output_dir, transcriptName)
    # Write the tsv data from Android app to the tsv file to store
    with open(tsv_path, "wb") as f:
        f.write(tsv_data)
    # Post process tsv file
    response = AppPostProcess.getStats(tsv_path)
    # Send outcomes back to Android app
    client.sendall(json.dumps(response).encode())
    client.close()