# Design Document: RFID Busy State Synchronization

## 1. Objective
To synchronize the `bRfidBusy` flag with hardware Trigger Press/Release events and UI interactions. This prevents race conditions, duplicate commands, and unintended inventory interruptions caused by trigger bouncing or UI latency.

## 2. Problem Statement
Previously, the `bRfidBusy` flag was updated only upon receiving `INVENTORY_START_EVENT` or `INVENTORY_STOP_EVENT` from the reader. This introduced a latency gap (tens to hundreds of milliseconds) between the user's action (Trigger Press) and the state update.

**Consequences:**
1.  **Race Conditions:** Rapid trigger presses (bouncing) could bypass the `if(bRfidBusy)` check, causing `OperationFailureException` (Reader already performing).
2.  **UI Desync:** The UI might remain enabled for a split second after starting, allowing double-taps.
3.  **Interruption:** If the busy check logic included a safety `stopInventory()` call, a trigger bounce could inadvertently stop the inventory immediately after it started.

## 3. Solution: Optimistic State Locking

We implemented an **Optimistic Locking** strategy where the application assumes the command will succeed and updates the state immediately, reverting only on failure.

### 3.1. Workflow: Trigger Press (Start Inventory)

1.  **Event:** `STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT` (Pressed) received in `eventStatusNotify`.
2.  **Action:** Call `performInventory()`.
3.  **Synchronization (Inside `performInventory`):**
    *   **Check:** If `bRfidBusy` is `true`, return immediately (Ignore). **Crucial:** Do not call `stopInventory()` here to prevent bounce-induced stops.
    *   **Lock:** Set `bRfidBusy = true` **immediately**.
    *   **Execute:** Call `reader.Actions.Inventory.perform()`.
    *   **Revert:** If `perform()` throws an exception, set `bRfidBusy = false` and update UI.
4.  **Confirmation:**
    *   Reader sends `INVENTORY_START_EVENT`.
    *   `eventStatusNotify` receives event.
    *   Set `bRfidBusy = true` (Idempotent confirmation).
    *   Update UI to "Running" state.

### 3.2. Workflow: Trigger Release (Stop Inventory)

1.  **Event:** `STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT` (Released) received in `eventStatusNotify`.
2.  **Action:** Call `stopInventory()`.
3.  **Synchronization:**
    *   `stopInventory()` sends the stop command.
    *   **Note:** We do **not** set `bRfidBusy = false` yet. The reader is still active until it fully stops.
4.  **Confirmation:**
    *   Reader sends `INVENTORY_STOP_EVENT`.
    *   `eventStatusNotify` receives event.
    *   Set `bRfidBusy = false`.
    *   Update UI to "Stopped" state (Enable Start buttons).

## 4. Implementation Details

### Modified `performInventory` Logic
```java
synchronized void performInventory() {
    if (bRfidBusy) {
        return; // Simply ignore, do not stop
    }
    
    // Optimistic Lock
    bRfidBusy = true; 
    
    try {
        reader.Actions.Inventory.perform();
    } catch (Exception e) {
        // Revert on failure
        bRfidBusy = false; 
        if (context != null) context.toggleInventoryButtons(false);
        throw e;
    }
}
```

### UI Synchronization
The UI state (`toggleInventoryButtons`) is driven by the asynchronous events to ensure it reflects the *actual* hardware state, not just the user's intent.

*   **`INVENTORY_START_EVENT`**: Disable Start, Enable Stop.
*   **`INVENTORY_STOP_EVENT`**: Enable Start, Disable Stop.
*   **Exception in `performInventory`**: Revert to Stopped state immediately.