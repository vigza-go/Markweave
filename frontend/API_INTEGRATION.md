# MarkWeave Frontend API Integration

## Overview

This document describes the complete frontend API integration for the MarkWeave project. The frontend is built with Vue.js 3 and communicates with the Spring Boot backend through RESTful APIs and WebSocket connections.

## Project Structure

```
frontend/
├── src/
│   ├── services/          # API service layer
│   │   ├── index.js        # Export all services
│   │   ├── auth.js         # Authentication APIs
│   │   ├── fileSystem.js   # File management APIs
│   │   ├── collaboration.js # Collaboration APIs
│   │   ├── ai.js          # AI formatting APIs
│   │   └── websocket.js   # WebSocket service for real-time collaboration
│   ├── components/        # Reusable components
│   │   ├── FileManager.vue       # File operations dialogs
│   │   ├── CollaborationManager.vue # Collaboration management
│   │   └── EditorToolbar.vue     # Editor toolbar with AI formatting
│   ├── views/             # Page components
│   │   ├── Dashboard.vue  # Main dashboard with file management
│   │   ├── Editor.vue      # Collaborative markdown editor
│   │   ├── Login.vue       # Login page
│   │   └── Register.vue   # Registration page
│   ├── http/
│   │   └── index.js       # Axios HTTP client configuration
│   └── router/
│       └── index.js       # Vue Router configuration
```

## API Services

### 1. Auth Service (`/services/auth.js`)

Handles user authentication and session management.

**Available Methods:**

```javascript
import { authService } from '@/services';

// Login
const response = await authService.login(account, password);
// Returns: { code: 200, data: { token, user }, message: "success" }

// Register
const response = await authService.register(account, password, confirmPassword, nickname);

// Logout
const response = await authService.logout();

// Utility methods
authService.setAuthHeader();      // Set Authorization header from localStorage
authService.clearAuth();          // Clear auth data from localStorage
authService.getToken();           // Get token from localStorage
authService.getUser();            // Get user object from localStorage
authService.isAuthenticated();    // Check if user is logged in
```

**API Endpoints:**
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/logout` - User logout

### 2. FileSystem Service (`/services/fileSystem.js`)

Manages file and folder operations.

**Available Methods:**

```javascript
import { fileSystemService } from '@/services';

// Create file
await fileSystemService.createFile(fileName, faId, fileType);
// Parameters: fileName (string), faId (number), fileType (number: 1=folder, 2=markdown, etc.)

// Create shortcut
await fileSystemService.createShortcut(faId, srcNodeId);

// Rename file/folder
await fileSystemService.rename(nodeId, newName);

// Move file/folder
await fileSystemService.move(nodeId, targetFolderId);

// Delete/recycle file
await fileSystemService.recycle(nodeId);

// List files in folder
const response = await fileSystemService.listFiles(faId);
// Returns: { code: 200, data: [{ id, name, type, ownerName, size, updateTime, ... }] }

// Update view time (when opening document)
await fileSystemService.updateViewTime(nodeId);

// Get recent documents
const response = await fileSystemService.getRecentDocs();
```

**API Endpoints:**
- `POST /api/fs/file/create` - Create new file or folder
- `POST /api/fs/file/shortcut/create` - Create shortcut to file
- `POST /api/fs/file/rename` - Rename file or folder
- `POST /api/fs/file/move` - Move file or folder
- `POST /api/fs/file/recycle` - Move to recycle bin
- `GET /api/fs/files/{faId}/list` - List files in folder
- `POST /api/fs/file/viewtime/update` - Update document view time
- `GET /api/fs/docs/recent` - Get recent documents

**File Types:**
- 1: Folder
- 2: Markdown
- 3: PDF
- 4: Word
- 5: Excel
- 6: PowerPoint
- 7: Code
- 8: Text

### 3. Collaboration Service (`/services/collaboration.js`)

Manages document collaboration and sharing.

**Available Methods:**

```javascript
import { collaborationService } from '@/services';

// Create invitation link
const response = await collaborationService.createInvitation(docId, permission, expTime);
// Returns: { code: 200, data: "invitation_token", message: "success" }
// permission: 1=view, 2=edit, 3=manage
// expTime: hours until expiration (1, 6, 12, 24, 168)

// Accept invitation
await collaborationService.acceptInvitation(invToken);

// Update collaborator permission
await collaborationService.updatePermission(docId, targetUserId, permission);

// Get collaborators list
const response = await collaborationService.getCollaborators(docId);
// Returns: { code: 200, data: [{ userId, userName, permission, ... }] }
```

**API Endpoints:**
- `POST /api/collaboration/invite` - Create invitation
- `POST /api/collaboration/invite/accept` - Accept invitation
- `POST /api/collaboration/permission` - Update permission
- `GET /api/collaboration/docs/{docId}/collaborators` - Get collaborators

**Permission Levels:**
- 1: View (可查看)
- 2: Edit (可编辑)
- 3: Manage (可管理)

### 4. AI Service (`/services/ai.js`)

Provides AI-powered document formatting.

**Available Methods:**

```javascript
import { aiService } from '@/services';

// Smart format markdown document
const response = await aiService.formatMarkdown(docId);
// Returns: { code: 200, data: "formatted_content", message: "success" }
```

**API Endpoints:**
- `POST /api/ai/format` - AI smart formatting

### 5. WebSocket Service (`/services/websocket.js`)

Handles real-time collaboration through WebSocket connections.

**Available Methods:**

```javascript
import { websocketService } from '@/services';

// Connect to collaboration session
websocketService.connect(docId, onMessage, onOpen, onClose, onError);

// Send operation
websocketService.sendOperation(operation);

// Request history sync
websocketService.requestHistory(version);

// Disconnect
websocketService.disconnect();

// Check connection status
websocketService.isConnected();
```

**WebSocket URL:** `ws://{host}/ws/collaboration?token={jwt_token}`

## HTTP Client Configuration

The HTTP client is configured with the following features:

**File:** `/http/index.js`

```javascript
const http = axios.create({
  baseURL: '/',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - adds Authorization header
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor - handles errors
http.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

## Component Usage

### FileManager Component

```vue
<template>
  <FileManager 
    ref="fileManager" 
    :current-folder-id="folderId"
    @refresh="loadFiles"
  />
</template>

<script setup>
import { ref } from 'vue';
import FileManager from '@/components/FileManager.vue';

const fileManager = ref(null);

// Show dialogs
fileManager.value?.showCreateDialog();
fileManager.value?.showRenameDialog(fileNode);
fileManager.value?.showMoveDialog(fileNode);
fileManager.value?.handleDelete(fileNode);
</script>
```

### CollaborationManager Component

```vue
<template>
  <CollaborationManager 
    ref="collaborationManager"
    :doc-id="documentId"
    @refresh="loadCollaborators"
  />
</template>

<script setup>
import { ref } from 'vue';
import CollaborationManager from '@/components/CollaborationManager.vue';

const collaborationManager = ref(null);

// Show dialogs
collaborationManager.value?.showInviteDialog();
collaborationManager.value?.showCollaboratorsDialog();
</script>
```

### EditorToolbar Component

```vue
<template>
  <EditorToolbar 
    :doc-id="documentId"
    @format="handleFormat"
    @export="handleExport"
  />
</template>

<script setup>
import EditorToolbar from '@/components/EditorToolbar.vue';

const handleFormat = (type, data) => {
  // Handle formatting
};

const handleExport = (format) => {
  // Handle export
};
</script>
```

## Editor Page

The Editor page integrates all collaboration features:

```javascript
// Route configuration
{
  path: '/editor/:docId',
  name: 'Editor',
  component: Editor,
  meta: { requiresAuth: true }
}

// Navigation
this.$router.push(`/editor/${docId}`);
```

**Features:**
- Monaco editor with markdown syntax highlighting
- Real-time collaborative editing with OT (Operational Transformation)
- Toolbar with formatting options
- AI smart formatting
- Export to Markdown, HTML, PDF
- WebSocket-based collaboration

## Error Handling

All API calls include proper error handling:

```javascript
try {
  const response = await someApiCall();
  if (response.code === 200) {
    // Success handling
  } else {
    ElMessage.error(response.message || 'Operation failed');
  }
} catch (error) {
  ElMessage.error('Error: ' + (error.response?.data?.message || error.message));
}
```

## Loading States

Components handle loading states for better UX:

```javascript
const loading = ref(false);

const handleAction = async () => {
  loading.value = true;
  try {
    // API call
  } finally {
    loading.value = false;
  }
};
```

## Routing

The router includes authentication guards:

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  
  if (to.meta.requiresAuth && !token) {
    next('/login');
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/dashboard');
  } else {
    next();
  }
});
```

## Usage Examples

### Login Flow

```javascript
import { authService } from '@/services';

const handleLogin = async () => {
  const response = await authService.login(account, password);
  if (response.code === 200) {
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('user', JSON.stringify(response.data.user));
    this.$router.push('/dashboard');
  }
};
```

### File Operations

```javascript
import { fileSystemService } from '@/services';

const createNewFile = async () => {
  const response = await fileSystemService.createFile('新文档.md', currentFolderId, 2);
  if (response.code === 200) {
    ElMessage.success('文件创建成功');
    loadFiles(); // Refresh file list
  }
};

const loadDocuments = async () => {
  const response = await fileSystemService.listFiles(folderId);
  if (response.code === 200) {
    documents.value = response.data;
  }
};
```

### Collaboration Flow

```javascript
import { collaborationService } from '@/services';

const inviteUser = async () => {
  const response = await collaborationService.createInvitation(docId, 2, 24);
  if (response.code === 200) {
    const link = `${window.location.origin}/invite/${response.data}`;
    await navigator.clipboard.writeText(link);
    ElMessage.success('邀请链接已复制');
  }
};

const loadCollaborators = async () => {
  const response = await collaborationService.getCollaborators(docId);
  if (response.code === 200) {
    collaborators.value = response.data;
  }
};
```

## Dependencies

- Vue 3
- Vue Router 4
- Element Plus
- Axios
- Monaco Editor
- Operational Transformation (OT) library

## Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

## Production Build

The frontend is configured to work with the backend through proxy configuration in `vite.config.js`. For production deployment, configure your web server to proxy API requests to the backend service.
