// (c) Copyright 2025 by Muczynski
import React from 'react'
import ReactDOM from 'react-dom/client'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { BrowserRouter } from 'react-router-dom'
import { queryClient } from './config/queryClient'
import { idbPersister, PERSISTED_CACHE_MAX_AGE, persistDehydrateOptions } from './config/idbPersister'
import App from './App'
import './index.css'
import 'cropperjs/dist/cropper.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{
        persister: idbPersister,
        maxAge: PERSISTED_CACHE_MAX_AGE,
        dehydrateOptions: persistDehydrateOptions,
      }}
    >
      <BrowserRouter>
        <App />
      </BrowserRouter>
      <ReactQueryDevtools initialIsOpen={false} />
    </PersistQueryClientProvider>
  </React.StrictMode>
)
