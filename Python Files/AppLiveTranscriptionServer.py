from whisper_live.server import TranscriptionServer

# Run file while LiveTestActivity open on Android app

if __name__ == "__main__":
    # Create a new TranscriptionServer to run WhisperLive
    server = TranscriptionServer()
    print("Server object created")
    # Use FasterWhisper for faster transcription
    server.run(
        "0.0.0.0",
        port=9090,
        backend="faster_whisper",
        single_model=False
    )