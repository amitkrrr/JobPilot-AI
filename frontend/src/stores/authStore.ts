import { create } from 'zustand';

interface AuthState {
  token: string | null;
  userEmail: string | null;
  userName: string | null;
  login: (token: string, email: string, name: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => {
  // Read initial state from localStorage if it exists
  const initialToken = localStorage.getItem('jobpilot_token');
  const initialEmail = localStorage.getItem('jobpilot_email');
  const initialName = localStorage.getItem('jobpilot_name');

  return {
    token: initialToken,
    userEmail: initialEmail,
    userName: initialName,
    login: (token, email, name) => {
      localStorage.setItem('jobpilot_token', token);
      localStorage.setItem('jobpilot_email', email);
      localStorage.setItem('jobpilot_name', name);
      set({ token, userEmail: email, userName: name });
    },
    logout: () => {
      localStorage.removeItem('jobpilot_token');
      localStorage.removeItem('jobpilot_email');
      localStorage.removeItem('jobpilot_name');
      set({ token: null, userEmail: null, userName: null });
    },
  };
});
