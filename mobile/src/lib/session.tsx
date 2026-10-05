import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { setToken } from './api';

export interface Session { userId: string; email: string; roles: string[]; accessToken: string }

interface Ctx {
  session: Session | null;
  booted: boolean;
  signIn: (s: Session) => Promise<void>;
  signOut: () => Promise<void>;
}
const SessionCtx = createContext<Ctx>({
  session: null, booted: false,
  signIn: async () => {}, signOut: async () => {},
});
export const useSession = () => useContext(SessionCtx);

const KEY = 'starmitra.session';

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [booted, setBooted] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(KEY).then((raw) => {
      if (raw) {
        try {
          const s = JSON.parse(raw) as Session;
          setToken(s.accessToken);
          setSession(s);
        } catch { /* corrupted store — drop */ }
      }
      setBooted(true);
    });
  }, []);

  const signIn = async (s: Session) => {
    setToken(s.accessToken);
    setSession(s);
    await AsyncStorage.setItem(KEY, JSON.stringify(s));
  };
  const signOut = async () => {
    setToken(null);
    setSession(null);
    await AsyncStorage.removeItem(KEY);
  };

  return (
    <SessionCtx.Provider value={{ session, booted, signIn, signOut }}>
      {children}
    </SessionCtx.Provider>
  );
}
