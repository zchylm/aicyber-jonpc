import { useCallback, useEffect, useRef, type Dispatch, type SetStateAction } from "react";

const historyViewKey = "jonpcView";
const historyScrollKey = "jonpcScrollY";
const historyViewChangeEvent = "jonpc:history-view-change";

function activeView() {
  const state = window.history.state;
  return state && typeof state === "object" ? state[historyViewKey] as string | undefined : undefined;
}

export function useHistoryPanel(view: string, isOpen: boolean, setIsOpen: Dispatch<SetStateAction<boolean>>) {
  const isOpenRef = useRef(isOpen);

  useEffect(() => {
    isOpenRef.current = isOpen;
  }, [isOpen]);

  useEffect(() => {
    const syncWithHistory = (event: Event) => {
      const shouldBeOpen = activeView() === view;
      if (shouldBeOpen !== isOpenRef.current) {
        isOpenRef.current = shouldBeOpen;
        setIsOpen(shouldBeOpen);
        if (event.type === "popstate") {
          const scrollY = (event as PopStateEvent).state?.[historyScrollKey];
          if (typeof scrollY === "number") {
            window.setTimeout(() => window.scrollTo({ top: scrollY, left: 0, behavior: "auto" }), 50);
          }
        }
      }
    };

    window.addEventListener("popstate", syncWithHistory);
    window.addEventListener(historyViewChangeEvent, syncWithHistory);
    const initialSync = activeView() === undefined
      ? null
      : window.setTimeout(() => syncWithHistory(new Event(historyViewChangeEvent)), 0);
    return () => {
      if (initialSync !== null) window.clearTimeout(initialSync);
      window.removeEventListener("popstate", syncWithHistory);
      window.removeEventListener(historyViewChangeEvent, syncWithHistory);
    };
  }, [setIsOpen, view]);

  const open = useCallback(() => {
    if (activeView() !== view) {
      const currentState = window.history.state;
      const nextState = currentState && typeof currentState === "object" ? { ...currentState } : {};
      const scrollY = window.scrollY;
      const stateWithScroll = { ...nextState, [historyScrollKey]: scrollY };
      window.history.replaceState(stateWithScroll, "", window.location.href);
      window.history.pushState({ ...stateWithScroll, [historyViewKey]: view }, "", window.location.href);
      window.dispatchEvent(new Event(historyViewChangeEvent));
    }
    isOpenRef.current = true;
    setIsOpen(true);
  }, [setIsOpen, view]);

  const close = useCallback(() => {
    if (activeView() === view) {
      window.history.back();
      return;
    }
    isOpenRef.current = false;
    setIsOpen(false);
  }, [setIsOpen, view]);

  const dismiss = useCallback(() => {
    if (activeView() === view) {
      const currentState = window.history.state as Record<string, unknown>;
      const nextState = { ...currentState };
      delete nextState[historyViewKey];
      window.history.replaceState(nextState, "", window.location.href);
      window.dispatchEvent(new Event(historyViewChangeEvent));
    }
    isOpenRef.current = false;
    setIsOpen(false);
  }, [setIsOpen, view]);

  return { open, close, dismiss };
}
