import pandas as pd
from faster_whisper import WhisperModel
import os
import pandas as pd
from word2number import w2n
import datetime
import socket
import json
import AppPostProcess

# Run file while SelectTestActivity open on Android app if uploading prerecorded file option is selected

# Convert any numbers in word form to numeric form using word2number module
def convert_numbers_in_sentence(sentence):
    # Split sentence into words
    words = sentence.split()
    # List to store final words/numbers
    converted = []
    buffer = []

    # Iterate all words
    for w in words:
        # Add current word to buffer
        buffer.append(w)
        try:
            # Join words in buffer with a space and convert to a number
            num = w2n.word_to_num(" ".join(buffer))
            # Add converted number to final list of words
            converted.append(str(num))
            # Clear buffer
            buffer = []
        except:
            # If more than 1 word in buffer and the buffer does not make a number
            if len(buffer) > 1:
                # Remove the first word
                converted.append(buffer.pop(0))

    # Add remaining words in buffer to list
    converted.extend(buffer)
    # Join all words into a string with spaces
    return " ".join(converted)

# Initialize a large Whisper model
model = WhisperModel(
    "large", 
    device="cpu",           
    compute_type="int8" 
)

# Run audio file through Whisper model
def run_whisper_and_load(file_path, output_dir="ResultFiles"):
    # Create a new file name to store transcription results in ResultFiles folder
    fileNum = 1
    now = datetime.datetime.now()
    formattedDateTime = now.strftime("%m%d%y")
    transcriptName = f"Transcript{formattedDateTime}({fileNum}).tsv"

    segments, _ = model.transcribe(
        file_path,
        beam_size=5,    
        language="en",  
        vad_filter=True,
        clip_timestamps=[0, 60] # For paper, only using the first 60 seconds of data
    )

    # For speech segment/row returned by Whisper, create a dictionary with times and convert any numbers in word form to numeric form
    rows = [
        {
            "start": seg.start,
            "end": seg.end,
            "text": convert_numbers_in_sentence(seg.text.strip())
        }
        for seg in segments
    ]
    # Create a pandas dataframe from dictionaries
    df = pd.DataFrame(rows)
    # Adjust file name is file already exists, if not create the file
    tsv_path = os.path.join(output_dir, transcriptName)
    while os.path.exists(tsv_path):
        fileNum += 1
        transcriptName = f"Transcript{formattedDateTime}({fileNum}).tsv"
        tsv_path = os.path.join(output_dir, transcriptName)
    # Write the dataframe to the tsv file
    df.to_csv(tsv_path, sep="\t", index=False)
    return tsv_path

# Initialize a socket on the local computer at a port specifically for precorded audio transcription
server = socket.socket()
server.bind(("0.0.0.0", 9091))
# Listen for data
server.listen()

while True:
    # Write audio data received from Android app to a temporary audio file
    client, addr = server.accept()
    audio_path = "audio.m4a"

    with open(audio_path, "wb") as f:
        while True:
            chunk = client.recv(4096)

            if not chunk:
                break
            f.write(chunk)
    
    # Transcribe the audio file and calculate outcomes with resulting tsv file
    transcriptionFile = run_whisper_and_load(audio_path)
    response = AppPostProcess.getStats(transcriptionFile)
    # Send outcomes back to Android app
    client.sendall(json.dumps(response).encode())
    client.close()