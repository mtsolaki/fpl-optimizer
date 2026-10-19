import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.tsx';

/**
 * Frontend bootstrap entry point.
 *
 * Mounts the top-level React root container inside StrictMode for
 * enhanced lifecycle debugging and component safety.
 */
createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <App />
    </StrictMode>,
);