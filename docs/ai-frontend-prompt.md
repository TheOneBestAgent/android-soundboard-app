# AI Frontend Generation Prompt: Android Soundboard App

## Generated Prompt for AI UI Tools (v0, Lovable, etc.)

```
**PROJECT CONTEXT:**
Create a modern Android soundboard application with a StreamDeck-inspired interface. This is a React Native/Android app that allows users to trigger audio clips through customizable button grids. The app emphasizes tactile feedback, visual polish, and comprehensive customization options.

**TECH STACK:**
- React Native for Android
- Material Design 3 (Material You) integration
- Expo for development workflow
- React Navigation for routing
- Async Storage for local data persistence
- React Native Sound for audio playback
- React Native Haptics for vibration feedback
- React Native Gesture Handler for touch interactions

**HIGH-LEVEL GOAL:**
Create the main soundboard interface screen with StreamDeck-inspired button grid, floating navigation elements, and comprehensive interaction patterns including press animations and haptic feedback.

**DETAILED STEP-BY-STEP INSTRUCTIONS:**

1. **Main Layout Structure:**
   - Create a full-screen layout with the soundboard grid as the focal point
   - Add a header section above the soundboard containing:
     - App name and version (top-left)
     - Soundboard selection dropdown (center)
     - Settings menu button (top-right hamburger icon)
   - Position a floating library access button (top-left, overlaying content)
   - Reserve bottom area for swipe gesture detection (board switching)

2. **Soundboard Grid Implementation:**
   - Create a responsive grid layout (default 4x3 buttons, but customizable)
   - Each button should be square with rounded corners (StreamDeck aesthetic)
   - Implement pinch-to-zoom functionality for grid scaling
   - Support dynamic grid sizing (2x2 up to 6x8)
   - Add visual spacing between buttons for clear separation

3. **Button Design & States:**
   - Default state: Display album art or fallback (text/emoji)
   - Pressed state: Scale down animation (0.95x) with subtle shadow
   - Playing state: Subtle glow or border highlight
   - Loading state: Spinner overlay on button
   - Empty state: Dashed border with "+" icon
   - Support text overlays on album art
   - Adapt colors to Material You theme

4. **Interaction Patterns:**
   - Single tap: Trigger sound with press animation + haptic feedback
   - Long press: Open edit menu modal
   - Pinch gesture: Zoom grid in/out
   - Swipe left/right on bottom 20% of screen: Switch soundboards
   - Drag from library: Drop sound onto button

5. **Navigation Elements:**
   - Floating library button: Circular FAB with music note icon
   - Settings menu: Slide-out from right with full customization options
   - Soundboard dropdown: Material Design dropdown with board names
   - Library slide-out: Left-side panel with drag-and-drop support

6. **Audio & Feedback Systems:**
   - Integrate React Native Sound for instant audio playback
   - Add React Native Haptics for button press vibration
   - Implement visual feedback animations using React Native Animated
   - Support simultaneous audio playback (mixing)
   - Add volume controls (per-button and master)

**CODE EXAMPLES & CONSTRAINTS:**

```javascript
// Button Component Structure
const SoundButton = ({ soundData, onPress, onLongPress, isPlaying }) => {
  const scaleAnim = useRef(new Animated.Value(1)).current;
  
  const handlePressIn = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    Animated.spring(scaleAnim, {
      toValue: 0.95,
      useNativeDriver: true,
    }).start();
  };
  
  const handlePressOut = () => {
    Animated.spring(scaleAnim, {
      toValue: 1,
      useNativeDriver: true,
    }).start();
  };
  
  return (
    <Animated.View style={[styles.button, { transform: [{ scale: scaleAnim }] }]}>
      {/* Album art or fallback content */}
    </Animated.View>
  );
};
```

```javascript
// Grid Layout Structure
const SoundboardGrid = ({ sounds, gridSize = { rows: 3, cols: 4 } }) => {
  return (
    <View style={styles.gridContainer}>
      {Array.from({ length: gridSize.rows * gridSize.cols }).map((_, index) => (
        <SoundButton
          key={index}
          soundData={sounds[index]}
          style={[styles.gridButton, { width: `${100/gridSize.cols}%` }]}
        />
      ))}
    </View>
  );
};
```

**VISUAL DESIGN SPECIFICATIONS:**
- **Color Palette**: Adapt to Android Material You dynamic colors
- **Typography**: Roboto font family, following Material Design guidelines
- **Spacing**: 16dp base unit, 8dp for tight spacing, 24dp for loose spacing
- **Button Style**: Rounded corners (12dp radius), subtle elevation (2dp)
- **Animations**: Spring-based with 300ms duration, easeInOut timing
- **Icons**: Material Design icons throughout
- **Album Art**: 1:1 aspect ratio, center-crop scaling

**STRICT SCOPE DEFINITION:**
- Create ONLY the main soundboard screen component
- Include button grid, header, and floating navigation elements
- Implement basic interaction patterns and animations
- DO NOT create settings screens, library management, or audio editing features
- DO NOT implement actual audio playback (use placeholder functions)
- DO NOT create navigation between screens
- Focus on the visual interface and interaction patterns only

**MOBILE-FIRST REQUIREMENTS:**
- Design for Android phones (360dp width minimum)
- Ensure touch targets are minimum 48dp for accessibility
- Optimize for portrait orientation primarily
- Support landscape mode with adjusted grid layout
- Consider tablet layouts with larger grid sizes
- Implement proper safe area handling for modern Android devices

**ACCESSIBILITY CONSIDERATIONS:**
- Add proper accessibility labels for all interactive elements
- Support TalkBack screen reader
- Ensure sufficient color contrast ratios
- Provide haptic feedback alternatives for audio cues
- Support large text scaling
```

## Prompt Structure Explanation

This prompt follows the four-part structured framework:

1. **High-Level Goal**: Clear objective to create the main soundboard interface
2. **Detailed Instructions**: Step-by-step breakdown of all components and interactions
3. **Code Examples**: Concrete React Native examples with proper patterns
4. **Strict Scope**: Clearly defined boundaries to prevent scope creep

## Key Features Included

✅ **StreamDeck-Inspired Design**: Album art buttons with system theming
✅ **Comprehensive Interactions**: Touch, long-press, pinch, swipe gestures
✅ **Haptic Feedback**: Vibration on button presses
✅ **Visual Animations**: Press animations and state changes
✅ **Floating Navigation**: Library access and settings menu
✅ **Responsive Grid**: Customizable button layouts
✅ **Material Design**: Android-native styling and theming

## Usage Instructions

1. Copy the generated prompt above
2. Paste into your preferred AI UI generation tool (v0, Lovable, etc.)
3. Review and refine the generated code
4. Test interactions and animations
5. Iterate with additional prompts for specific refinements

**Important Note**: All AI-generated code requires careful human review, testing, and refinement to be considered production-ready. Use this as a starting point for your soundboard interface implementation.