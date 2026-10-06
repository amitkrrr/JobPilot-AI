import { useState } from 'react';
import { useAuthStore } from './stores/authStore';
import LandingPage from './views/LandingPage';
import AuthPage from './views/AuthPage';
import Dashboard from './views/Dashboard';

export default function App() {
  const token = useAuthStore((state) => state.token);
  
  // App navigation state for unauthenticated flows: 'landing', 'login', or 'register'
  const [currentScreen, setCurrentScreen] = useState<'landing' | 'login' | 'register'>('landing');

  // If we have a signed token in the state, immediately grant entry to our main profile workspace.
  if (token) {
    return <Dashboard />;
  }

  // Otherwise, orchestrate between our beautiful Landing Page and mock-styled Auth Screen
  if (currentScreen === 'login') {
    return (
      <AuthPage 
        initialView="login" 
        onNavigateBack={() => setCurrentScreen('landing')} 
      />
    );
  }

  if (currentScreen === 'register') {
    return (
      <AuthPage 
        initialView="register" 
        onNavigateBack={() => setCurrentScreen('landing')} 
      />
    );
  }

  return (
    <LandingPage 
      onNavigate={(view) => setCurrentScreen(view)} 
    />
  );
}
