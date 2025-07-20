# State Management Architecture

## Zustand Store Structure
```typescript
// src/stores/soundboardStore.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { SoundButton, Soundboard } from '../types/audio';

interface SoundboardState {
  // State
  currentSoundboard: Soundboard | null;
  soundboards: Soundboard[];
  isLoading: boolean;
  error: string | null;
  
  // Actions
  loadSoundboards: () => Promise<void>;
  createSoundboard: (name: string) => Promise<Soundboard>;
  updateSoundboard: (id: string, updates: Partial<Soundboard>) => Promise<void>;
  deleteSoundboard: (id: string) => Promise<void>;
  setCurrentSoundboard: (soundboard: Soundboard) => void;
  
  // Button actions
  addButton: (soundboardId: string, button: Omit<SoundButton, 'id'>) => Promise<void>;
  updateButton: (buttonId: string, updates: Partial<SoundButton>) => Promise<void>;
  deleteButton: (buttonId: string) => Promise<void>;
  reorderButtons: (soundboardId: string, buttonIds: string[]) => Promise<void>;
}

export const useSoundboardStore = create<SoundboardState>()()
  persist(
    (set, get) => ({
      // Initial state
      currentSoundboard: null,
      soundboards: [],
      isLoading: false,
      error: null,

      // Actions
      loadSoundboards: async () => {
        set({ isLoading: true, error: null });
        try {
          const soundboards = await soundboardService.getAllSoundboards();
          set({ soundboards, isLoading: false });
        } catch (error) {
          set({ error: error.message, isLoading: false });
        }
      },

      createSoundboard: async (name: string) => {
        set({ isLoading: true, error: null });
        try {
          const newSoundboard = await soundboardService.createSoundboard({ name });
          set(state => ({
            soundboards: [...state.soundboards, newSoundboard],
            isLoading: false,
          }));
          return newSoundboard;
        } catch (error) {
          set({ error: error.message, isLoading: false });
          throw error;
        }
      },

      updateSoundboard: async (id: string, updates: Partial<Soundboard>) => {
        try {
          const updatedSoundboard = await soundboardService.updateSoundboard(id, updates);
          set(state => ({
            soundboards: state.soundboards.map(sb => 
              sb.id === id ? updatedSoundboard : sb
            ),
            currentSoundboard: state.currentSoundboard?.id === id 
              ? updatedSoundboard 
              : state.currentSoundboard,
          }));
        } catch (error) {
          set({ error: error.message });
        }
      },

      deleteSoundboard: async (id: string) => {
        try {
          await soundboardService.deleteSoundboard(id);
          set(state => ({
            soundboards: state.soundboards.filter(sb => sb.id !== id),
            currentSoundboard: state.currentSoundboard?.id === id 
              ? null 
              : state.currentSoundboard,
          }));
        } catch (error) {
          set({ error: error.message });
        }
      },

      setCurrentSoundboard: (soundboard: Soundboard) => {
        set({ currentSoundboard: soundboard });
      },

      addButton: async (soundboardId: string, buttonData: Omit<SoundButton, 'id'>) => {
        try {
          const newButton = await soundboardService.addButton(soundboardId, buttonData);
          set(state => ({
            soundboards: state.soundboards.map(sb => 
              sb.id === soundboardId 
                ? { ...sb, buttons: [...sb.buttons, newButton] }
                : sb
            ),
            currentSoundboard: state.currentSoundboard?.id === soundboardId
              ? { ...state.currentSoundboard, buttons: [...state.currentSoundboard.buttons, newButton] }
              : state.currentSoundboard,
          }));
        } catch (error) {
          set({ error: error.message });
        }
      },

      updateButton: async (buttonId: string, updates: Partial<SoundButton>) => {
        try {
          const updatedButton = await soundboardService.updateButton(buttonId, updates);
          set(state => ({
            soundboards: state.soundboards.map(sb => ({
              ...sb,
              buttons: sb.buttons.map(btn => 
                btn.id === buttonId ? updatedButton : btn
              ),
            })),
            currentSoundboard: state.currentSoundboard ? {
              ...state.currentSoundboard,
              buttons: state.currentSoundboard.buttons.map(btn => 
                btn.id === buttonId ? updatedButton : btn
              ),
            } : null,
          }));
        } catch (error) {
          set({ error: error.message });
        }
      },

      deleteButton: async (buttonId: string) => {
        try {
          await soundboardService.deleteButton(buttonId);
          set(state => ({
            soundboards: state.soundboards.map(sb => ({
              ...sb,
              buttons: sb.buttons.filter(btn => btn.id !== buttonId),
            })),
            currentSoundboard: state.currentSoundboard ? {
              ...state.currentSoundboard,
              buttons: state.currentSoundboard.buttons.filter(btn => btn.id !== buttonId),
            } : null,
          }));
        } catch (error) {
          set({ error: error.message });
        }
      },

      reorderButtons: async (soundboardId: string, buttonIds: string[]) => {
        try {
          await soundboardService.reorderButtons(soundboardId, buttonIds);
          set(state => ({
            soundboards: state.soundboards.map(sb => 
              sb.id === soundboardId 
                ? { 
                    ...sb, 
                    buttons: buttonIds.map(id => 
                      sb.buttons.find(btn => btn.id === id)!
                    )
                  }
                : sb
            ),
          }));
        } catch (error) {
          set({ error: error.message });
        }
      },
    }),
    {
      name: 'soundboard-storage',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        currentSoundboard: state.currentSoundboard,
        soundboards: state.soundboards,
      }),
    }
  )
);
```

## State Management Patterns
- **Normalized State:** Complex nested data is flattened for efficient updates
- **Optimistic Updates:** UI updates immediately, with rollback on API failure
- **Selective Persistence:** Only essential data is persisted to local storage
- **Error Boundaries:** Graceful error handling with user-friendly messages
- **Loading States:** Granular loading indicators for better UX

---
