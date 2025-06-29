#!/bin/bash
echo "Building Android Soundboard Server..."
./gradlew clean assembleDebug
if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
    exit 1
fi
