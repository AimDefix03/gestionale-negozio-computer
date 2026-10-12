import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { DraftStoreProvider } from './drafts/DraftStoreProvider';
import { SessionProvider } from './session/SessionProvider';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <SessionProvider>
      <DraftStoreProvider>
        <App />
      </DraftStoreProvider>
    </SessionProvider>
  </React.StrictMode>
);
