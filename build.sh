#!/bin/bash
echo "Building Android App..."
./gradlew clean assembleDebug
if [ $? -ne 0 ]; then
    echo "Android build failed!"
    exit 1
fi

echo "Building Android Soundboard Server..."
npm run build:server
if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
    exit 1
fi
