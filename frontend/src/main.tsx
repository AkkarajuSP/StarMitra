import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './admin/brand.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode><App /></StrictMode>,
);
