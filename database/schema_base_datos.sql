-- ============================================================================
-- COMPONENTE 2: BASE DE DATOS Y SCRIPTS DE DESPLIEGUE
-- Proyecto: Aplicación Móvil para la Gestión de Informes - CSR
-- Desarrolladores: Matías Castaño Ospina, Manolo Bañol Giraldo
-- Arquitectura Híbrida: SQLite (Room Local) + Cloud Firestore (Nube)
-- ============================================================================

-- ============================================================================
-- SECCIÓN 1: ESTRUCTURA RELACIONAL LOCAL (DDL - ROOM / SQLITE)
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY NOT NULL,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'SERVICIOS_GENERALES', 'DOCENTE', 'ESTUDIANTE')),
    avatar_id INTEGER DEFAULT 1,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    location TEXT NOT NULL,
    risk_level TEXT NOT NULL CHECK (risk_level IN ('BAJO', 'MEDIO', 'ALTO')),
    status TEXT NOT NULL DEFAULT 'PENDIENTE' CHECK (status IN ('PENDIENTE', 'EN_PROCESO', 'SOLUCIONADO')),
    image_url TEXT,
    author_id TEXT NOT NULL,
    assigned_to TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    id TEXT PRIMARY KEY NOT NULL,
    report_id TEXT NOT NULL,
    title TEXT NOT NULL,
    observations TEXT,
    is_completed INTEGER NOT NULL DEFAULT 0 CHECK (is_completed IN (0, 1)),
    assigned_user_id TEXT NOT NULL,
    due_date INTEGER,
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Índices para optimizar consultas de trazabilidad y estado
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_author ON reports(author_id);
CREATE INDEX IF NOT EXISTS idx_tasks_report ON tasks(report_id);


-- ============================================================================
-- SECCIÓN 2: ESQUEMA EN LA NUBE (FIREBASE CLOUD FIRESTORE)
-- Referencia de archivos en raíz: firebase-blueprint.json, firestore.rules, firestore.test.js
-- ============================================================================
/*
Colecciones principales en Cloud Firestore:
1. /users/{userId}
   - Campos: username (string), email (string), role (string), avatarId (number), createdAt (timestamp)
   - Reglas: Lectura autenticada; escritura restringida al propietario o rol ADMIN.

2. /reports/{reportId}
   - Campos: title (string), description (string), location (string), riskLevel (string),
             status (string), imageUrl (string), authorId (string), assignedTo (string),
             createdAt (timestamp), updatedAt (timestamp)
   - Reglas: Creación abierta a usuarios autenticados; actualización de estado exclusiva para
             roles SERVICIOS_GENERALES y ADMIN.

3. /tasks/{taskId}
   - Campos: reportId (string), title (string), observations (string), isCompleted (boolean),
             assignedUserId (string), dueDate (timestamp)

4. /chats/{messageId} y /social_posts/{postId}
   - Colecciones para comunicación institucional en tiempo real y feed de novedades.
*/


-- ============================================================================
-- SECCIÓN 3: DATOS INICIALES Y DE PRUEBA (SEED DATA)
-- ============================================================================

INSERT OR IGNORE INTO users (id, username, email, role, avatar_id, created_at) VALUES
('usr_admin_01', 'admin_csr', 'admin@colredentorista.edu.co', 'ADMIN', 1, 1763380000000),
('usr_serv_01', 'servicios_generales', 'mantenimiento@colredentorista.edu.co', 'SERVICIOS_GENERALES', 2, 1763380000000),
('usr_doc_01', 'prof_carlos', 'carlos.docente@colredentorista.edu.co', 'DOCENTE', 3, 1763385000000),
('usr_est_01', 'matias_castano', 'matias.castano@colredentorista.edu.co', 'ESTUDIANTE', 4, 1763386000000);

INSERT OR IGNORE INTO reports (id, title, description, location, risk_level, status, image_url, author_id, assigned_to, created_at, updated_at) VALUES
('rep_001', 'Fuga de agua en lavamanos', 'Llave principal del baño de hombres presenta goteo constante.', 'Bloque B - Primer Piso', 'MEDIO', 'PENDIENTE', 'https://storage.googleapis.com/csr-reportes/evidencia_001.jpg', 'usr_doc_01', 'usr_serv_01', 1763400000000, 1763400000000),
('rep_002', 'Luminaria parpadeando en aula', 'Tubo LED del fondo del salón 204 no enciende correctamente.', 'Bloque A - Salón 204', 'BAJO', 'EN_PROCESO', NULL, 'usr_est_01', 'usr_serv_01', 1763410000000, 1763420000000),
('rep_003', 'Toma corriente suelto', 'Caja eléctrica desprendida de la pared cerca al tablero.', 'Laboratorio de Sistemas', 'ALTO', 'SOLUCIONADO', 'https://storage.googleapis.com/csr-reportes/evidencia_003.jpg', 'usr_doc_01', 'usr_serv_01', 1763390000000, 1763450000000);

INSERT OR IGNORE INTO tasks (id, report_id, title, observations, is_completed, assigned_user_id, due_date) VALUES
('tsk_001', 'rep_001', 'Revisión y cambio de empaque en lavamanos', 'Verificar válvula de paso antes de intervenir.', 0, 'usr_serv_01', 1763500000000),
('tsk_002', 'rep_002', 'Reemplazo de tubo LED 18W', 'Solicitado repuesto en almacén.', 0, 'usr_serv_01', 1763510000000),
('tsk_003', 'rep_003', 'Fijación y aislamiento de toma eléctrico', 'Trabajo finalizado y verificado sin riesgo.', 1, 'usr_serv_01', 1763450000000);
