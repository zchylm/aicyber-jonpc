import { useCallback, useEffect, useRef, type Dispatch, type SetStateAction } from "react";

const historyViewKey = "jonpcView";

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
    const syncWithHistory = () => {
      const shouldBeOpen = activeView() === view;
      if (shouldBeOpen !== isOpenRef.current) {
        isOpenRef.current = shouldBeOpen;
        setIsOpen(shouldBeOpen);
      }
    };

    window.addEventListener("popstate", syncWithHistory);
    return () => window.removeEventListener("popstate", syncWithHistory);
  }, [setIsOpen, view]);

  const open = useCallback(() => {
    if (activeView() !== view) {
      const currentState = window.history.state;
      const nextState = currentState && typeof currentState === "object" ? { ...currentState } : {};
      window.history.pushState({ ...nextState, [historyViewKey]: view }, "", window.location.href);
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
    }
    isOpenRef.current = false;
    setIsOpen(false);
  }, [setIsOpen, view]);

  return { open, close, dismiss };
}
